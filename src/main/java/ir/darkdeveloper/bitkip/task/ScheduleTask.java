package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.utils.MainTableUtils;
import ir.darkdeveloper.bitkip.utils.MenuUtils;
import ir.darkdeveloper.bitkip.utils.QueueUtils;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;

import java.time.*;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


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

    public void start() {
        if (!schedule.isEnabled())
            return;
        var scheduler = Executors.newScheduledThreadPool(1);

        if (schedule.isOnceDownload()) {
            var startDate = schedule.getStartDate();
            var startTime = schedule.getStartTime();
            var dateTime = LocalDateTime.of(startDate, startTime);
            Runnable task = () -> System.out.println("Task executed at specific time.");
            var zone = ZoneId.systemDefault();
            var zonedDateTime = dateTime.atZone(zone);
            var duration = Duration.between(ZonedDateTime.now(), zonedDateTime);
            long initialDelay = Math.max(duration.toMillis(), 0);
            scheduler.schedule(task, initialDelay, TimeUnit.MILLISECONDS);
        } else {
            var a = calculateInitialDate();
            scheduler.scheduleAtFixedRate(() -> {
                if (!schedule.getDays().contains(LocalDate.now().getDayOfWeek()))
                    return;
                System.out.println("period");
            }, a, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
        }


    }

    private long calculateInitialDate() {
        var startTime = schedule.getStartTime();
        var nowTime = LocalTime.now();
        // today
        if (startTime.isAfter(nowTime)) {
            var zone = ZoneId.systemDefault();
            var zonedDateTime = startTime.atDate(LocalDate.now()).atZone(zone);
            var duration = Duration.between(ZonedDateTime.now(), zonedDateTime);
            return Math.max(duration.toMillis(), 0);
        }
        // next day
        else {
            var zone = ZoneId.systemDefault();
            var zonedDateTime = startTime.atDate(LocalDate.now().plusDays(1)).atZone(zone);
            var duration = Duration.between(ZonedDateTime.now(), zonedDateTime);
            return Math.max(duration.toMillis(), 0);
        }
    }


    private class StopTask extends TimerTask {
        public void run() {
            System.out.println("stop");
            QueueUtils.stopQueue(queue, startItem, stopItem, mainTableUtils);
        }
    }
}