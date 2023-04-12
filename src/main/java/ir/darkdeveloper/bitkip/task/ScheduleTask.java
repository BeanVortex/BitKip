package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.utils.MainTableUtils;
import ir.darkdeveloper.bitkip.utils.MenuUtils;
import ir.darkdeveloper.bitkip.utils.QueueUtils;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;

import java.time.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentSchedules;


public class ScheduleTask {

    private final ScheduleModel schedule;
    private final QueueModel queue;
    private final MainTableUtils mainTableUtils;
    private final MenuItem stopItem, startItem;

    public ScheduleTask(ScheduleModel schedule, QueueModel queue, MainTableUtils mainTableUtils) {
        this.schedule = schedule;
        this.queue = queue;
        this.mainTableUtils = mainTableUtils;
        this.stopItem = MenuUtils.stopQueueMenu.getItems().stream()
                .filter(m -> ((Label) m.getGraphic()).getText().equals(queue.getName()))
                .findFirst().orElse(null);
        this.startItem = MenuUtils.startQueueMenu.getItems().stream()
                .filter(m -> ((Label) m.getGraphic()).getText().equals(queue.getName()))
                .findFirst().orElse(null);
    }

    public void schedule() {
        var isThereSchedule = currentSchedules.keySet().stream().anyMatch(id -> id == schedule.getId());
        if (isThereSchedule) {
            var areAllScheduleFieldSame = currentSchedules.values().stream().anyMatch(s -> s.equals(schedule));
            if (areAllScheduleFieldSame)
                return;
        }

        if (!schedule.isEnabled()) {
            if (isThereSchedule) {
                var sm = currentSchedules.get(schedule.getId());
                sm.getStartScheduler().shutdown();
                if (sm.getStopScheduler() != null)
                    sm.getStopScheduler().shutdown();
                currentSchedules.remove(sm.getId());
            }
            return;
        }

        startSchedule(isThereSchedule);
        if (schedule.isStopTimeEnabled())
            stopSchedule(isThereSchedule);
        currentSchedules.put(schedule.getId(), schedule);
    }

    private void startSchedule(boolean isThereSchedule) {
        Runnable run = () -> QueueUtils.startQueue(queue, startItem, stopItem, mainTableUtils);
        createSchedule(run, false);

        var sm = currentSchedules.get(schedule.getId());
        if (isThereSchedule)
            sm.getStartScheduler().shutdown();
    }

    private void stopSchedule(boolean isThereSchedule) {
        Runnable run = () -> QueueUtils.stopQueue(queue, startItem, stopItem, mainTableUtils);
        createSchedule(run, true);

        var sm = currentSchedules.get(schedule.getId());
        if (isThereSchedule && sm.getStopScheduler() != null)
            sm.getStopScheduler().shutdown();
    }

    private void createSchedule(Runnable run, boolean isStop) {
        var scheduler = Executors.newScheduledThreadPool(1);
        var specifiedTime = schedule.getStartTime();
        if (isStop) {
            schedule.setStopScheduler(scheduler);
            specifiedTime = schedule.getStopTime();
        } else
            schedule.setStartScheduler(scheduler);

        if (schedule.isOnceDownload()) {
            var initialDelay = calculateOnceInitialDelay(isStop);
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


    private long calculateOnceInitialDelay(boolean isStop) {
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

    private long calculateDailyInitialDelay(LocalTime specifiedTime) {
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
}