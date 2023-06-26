package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.repo.ScheduleRepo;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.MenuUtils;
import ir.darkdeveloper.bitkip.utils.QueueUtils;
import ir.darkdeveloper.bitkip.utils.SideUtils;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;

import java.time.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.sun.jna.Platform.isLinux;
import static com.sun.jna.Platform.isMac;
import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.config.observers.QueueSubject.getQueues;


public class ScheduleTask {

    public static void scheduleQueues() {
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


        startSchedule(queue, startItem, stopItem);
        if (schedule.isStopTimeEnabled())
            stopSchedule(queue, startItem, stopItem);
        currentSchedules.put(schedule.getId(), schedule);
        if (schedule.isTurnOffEnabled() && (isLinux() || isMac()) && userPassword == null) {
            var header = "You have at least one queue that has been scheduled to turn off or suspend.";
            var content = "To be able to turn off or suspend your pc, application needs your system password";
            FxUtils.askForPassword(header, content);
        }
    }

    private static void startSchedule(QueueModel queue, MenuItem startItem, MenuItem stopItem) {
        Runnable run = () -> {
            log.info("Start scheduler triggered for " + queue.toStringModel());
            if (isSchedulerNotTriggeredOnTime("Start", queue)) return;
            QueueUtils.startQueue(queue, startItem, stopItem);
        };
        createSchedule(run, queue, false);
    }

    private static void stopSchedule(QueueModel queue, MenuItem startItem, MenuItem stopItem) {
        Runnable run = () -> {
            log.info("Stop scheduler triggered for " + queue.toStringModel());
            if (isSchedulerNotTriggeredOnTime("Stop", queue)) return;
            QueueUtils.stopQueue(queue, startItem, stopItem);
        };
        createSchedule(run, queue, true);
    }

    private static boolean isSchedulerNotTriggeredOnTime(String mode, QueueModel queue) {
        var schedule = queue.getSchedule();
        var startDate = schedule.getStartDate();
        var desiredTime = schedule.getStartTime().toSecondOfDay();
        if (mode.equals("Stop"))
            desiredTime = schedule.getStopTime().toSecondOfDay();
        var nowTime = LocalTime.now().toSecondOfDay();
        // maximum 10 seconds, desiredTime or nowTime can be different from each other
        var isDesiredTimeNotOK = Math.abs(desiredTime - nowTime) > 10;
        var isOnceDownloadNotOK = schedule.isOnceDownload() && (!LocalDate.now().equals(startDate) || isDesiredTimeNotOK);
        var isDayOfWeekNotOk = !schedule.isOnceDownload() && (!schedule.getDays().contains(LocalDate.now().getDayOfWeek())
                || isDesiredTimeNotOK);
        if (isOnceDownloadNotOK || isDayOfWeekNotOk) {
            if (isOnceDownloadNotOK){
                queue.getSchedule().setEnabled(false);
                SideUtils.changeScheduledQueueIcon(queue);
            }
            log.info(mode + " scheduler triggered on a wrong date or time for %s. Rescheduling all schedules... "
                    .formatted(queue.getName()));
            scheduleQueues();
            return true;
        }
        return false;
    }

    private static void createSchedule(Runnable run, QueueModel queue, boolean isStop) {
        var schedule = queue.getSchedule();
        var scheduler = Executors.newScheduledThreadPool(1);

        if (schedule.isOnceDownload()) {
            var initialDelay = calculateOnceInitialDelay(isStop, schedule);
            var firstTriggerMsg = "Once start scheduler will trigger after ";
            if (isStop) {
                schedule.setStopScheduler(scheduler);
                firstTriggerMsg = "Once stop scheduler will trigger after ";
            } else
                schedule.setStartScheduler(scheduler);
            firstTriggerMsg += TimeUnit.MILLISECONDS.toMinutes(initialDelay) + " minutes :" + queue.toStringModel();
            log.info(firstTriggerMsg);
            scheduler.schedule(run, initialDelay, TimeUnit.MILLISECONDS);
        } else {
            var specifiedTime = schedule.getStartTime();
            var firstTriggerMsg = "Daily start scheduler will trigger after ";

            if (isStop) {
                schedule.setStopScheduler(scheduler);
                firstTriggerMsg = "Daily stop scheduler will trigger after ";
                specifiedTime = schedule.getStopTime();
            } else
                schedule.setStartScheduler(scheduler);

            var initialDelay = calculateDailyInitialDelay(specifiedTime);
            firstTriggerMsg += TimeUnit.MILLISECONDS.toMinutes(initialDelay) + " minutes :" + queue.toStringModel();
            log.info(firstTriggerMsg);

            scheduler.scheduleAtFixedRate(run, initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
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

        var sm = currentSchedules.get(schedule.getId());
        if (!schedule.isEnabled()) {
            if (isThereSchedule) {
                sm.getStartScheduler().shutdownNow();
                if (sm.getStopScheduler() != null)
                    sm.getStopScheduler().shutdownNow();
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
            if (sm != null && sm.getStartScheduler() != null) sm.getStartScheduler().shutdownNow();
            if (sm != null && sm.getStopScheduler() != null) sm.getStopScheduler().shutdownNow();
        }
        return false;
    }
}