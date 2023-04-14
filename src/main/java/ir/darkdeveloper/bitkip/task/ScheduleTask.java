package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.repo.ScheduleRepo;
import ir.darkdeveloper.bitkip.utils.MenuUtils;
import ir.darkdeveloper.bitkip.utils.QueueUtils;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;

import java.time.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;


public class ScheduleTask {

    public static void startSchedules() {
        getQueues().forEach(ScheduleTask::schedule);
    }

    public static void schedule(QueueModel queue) {
        var schedule = queue.getSchedule();
        var isThereSchedule = currentSchedules.keySet().stream().anyMatch(id -> id == schedule.getId());
        if (validateScheduleModel(schedule, isThereSchedule)) return;

        var stopItem = MenuUtils.stopQueueMenu.getItems().stream()
                .filter(m -> ((Label) m.getGraphic()).getText().equals(queue.getName()))
                .findFirst().orElse(null);

        var startItem = MenuUtils.startQueueMenu.getItems().stream()
                .filter(m -> ((Label) m.getGraphic()).getText().equals(queue.getName()))
                .findFirst().orElse(null);


        startSchedule(isThereSchedule, queue, schedule, startItem, stopItem);
        if (schedule.isStopTimeEnabled())
            stopSchedule(isThereSchedule, queue, schedule, startItem, stopItem);
        currentSchedules.put(schedule.getId(), schedule);
    }

    private static void startSchedule(boolean isThereSchedule, QueueModel queue, ScheduleModel schedule,
                                      MenuItem startItem, MenuItem stopItem) {
        Runnable run = () -> QueueUtils.startQueue(queue, startItem, stopItem, mainTableUtils);
        createSchedule(run, schedule, false);

        var sm = currentSchedules.get(schedule.getId());
        if (isThereSchedule)
            sm.getStartScheduler().shutdown();
    }

    private static void stopSchedule(boolean isThereSchedule, QueueModel queue, ScheduleModel schedule,
                                     MenuItem startItem, MenuItem stopItem) {
        Runnable run = () -> QueueUtils.stopQueue(queue, startItem, stopItem, mainTableUtils);
        createSchedule(run, schedule, true);

        var sm = currentSchedules.get(schedule.getId());
        if (isThereSchedule && sm.getStopScheduler() != null)
            sm.getStopScheduler().shutdown();
    }

    private static void createSchedule(Runnable run, ScheduleModel schedule, boolean isStop) {
        var scheduler = Executors.newScheduledThreadPool(1);
        var specifiedTime = schedule.getStartTime();
        if (isStop) {
            schedule.setStopScheduler(scheduler);
            specifiedTime = schedule.getStopTime();
        } else
            schedule.setStartScheduler(scheduler);

        if (schedule.isOnceDownload()) {
            var initialDelay = calculateOnceInitialDelay(isStop, schedule);
            scheduler.schedule(run, initialDelay, TimeUnit.MILLISECONDS);
        } else {
            var initialDelay = calculateDailyInitialDelay(specifiedTime);
            scheduler.scheduleAtFixedRate(() -> {
                if (!schedule.getDays().contains(LocalDate.now().getDayOfWeek()))
                    return;
                run.run();
            }, initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        }
    }

    private static long calculateOnceInitialDelay(boolean isStop, ScheduleModel schedule) {
        var startDate = schedule.getStartDate();
        var startTime = schedule.getStartTime();
        var dateTime = LocalDateTime.of(startDate, startTime);
        if (isStop) {
            var stopTime = schedule.getStopTime();
            if (stopTime.isBefore(startTime))
                startDate = startDate.plusDays(1);
            dateTime = LocalDateTime.of(startDate, stopTime);
        }
        var zone = ZoneId.systemDefault();
        var zonedDateTime = dateTime.atZone(zone);
        var duration = Duration.between(ZonedDateTime.now(), zonedDateTime);
        return Math.max(duration.toMillis(), 0);
    }

    private static long calculateDailyInitialDelay(LocalTime specifiedTime) {
        var nowTime = LocalTime.now();
        // today
        if (specifiedTime.isAfter(nowTime)) {
            var zone = ZoneId.systemDefault();
            var zonedDateTime = specifiedTime.atDate(LocalDate.now()).atZone(zone);
            var duration = Duration.between(ZonedDateTime.now(), zonedDateTime);
            return Math.max(duration.toMillis(), 0);
        }
        // next day
        else {
            var zone = ZoneId.systemDefault();
            var zonedDateTime = specifiedTime.atDate(LocalDate.now().plusDays(1)).atZone(zone);
            var duration = Duration.between(ZonedDateTime.now(), zonedDateTime);
            return Math.max(duration.toMillis(), 0);
        }
    }

    private static boolean validateScheduleModel(ScheduleModel schedule, boolean isThereSchedule) {
        if (isThereSchedule) {
            var areAllScheduleFieldSame = currentSchedules.values().stream().anyMatch(s -> s.equals(schedule));
            if (areAllScheduleFieldSame)
                return true;
        }

        var sm = currentSchedules.get(schedule.getId());
        if (!schedule.isEnabled()) {
            if (isThereSchedule) {
                sm.getStartScheduler().shutdown();
                if (sm.getStopScheduler() != null)
                    sm.getStopScheduler().shutdown();
                currentSchedules.remove(sm.getId());
            }
            return true;
        } else {
            if (schedule.isOnceDownload()) {
                var scheduledTime = schedule.getStartDate().atTime(schedule.getStartTime());
                if (scheduledTime.isBefore(LocalDateTime.now())) {
                    schedule.setEnabled(false);
                    ScheduleRepo.updateScheduleEnabled(schedule.getId(), schedule.isEnabled());
                    return true;
                }
            }
            if (sm != null && sm.getStartScheduler() != null) sm.getStartScheduler().shutdown();
            if (sm != null && sm.getStopScheduler() != null) sm.getStopScheduler().shutdown();
        }
        return false;
    }
}