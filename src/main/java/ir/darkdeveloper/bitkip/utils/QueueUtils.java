package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.application.Platform;
import javafx.scene.control.MenuItem;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;

public class QueueUtils {


    public static void startQueue(QueueModel qm, MenuItem startItem, MenuItem stopItem, MainTableUtils mainTableUtils) {
        var schedule = qm.getSchedule();
        if (!startedQueues.contains(qm)) {
            var downloadsByQueue = QueuesRepo.findByName(qm.getName(), true)
                    .getDownloads()
                    .stream()
                    .sorted(Comparator.comparing(DownloadModel::getAddToQueueDate))
                    .toList();
            if (downloadsByQueue.isEmpty()) {
                queueDoneNotification(qm);
                return;
            }
            startItem.setDisable(true);
            stopItem.setDisable(false);
            qm.setDownloads(new CopyOnWriteArrayList<>(downloadsByQueue));
            startedQueues.add(qm);
            var executor = Executors.newCachedThreadPool();
            if (schedule.isEnabled())
                startFromSchedule(qm, startItem, stopItem, executor);
            else {
                executor.submit(() -> {
                    for (int i = 0; i < qm.getDownloads().size(); i++) {
                        var dm = qm.getDownloads().get(i);
                        if (dm.getDownloadStatus() == DownloadStatus.Paused) {
                            dm = mainTableUtils.getObservedDownload(dm);
                            DownloadOpUtils.startDownload(mainTableUtils, dm, null,
                                    null, true, true, executor);
                        }
                        if (!startedQueues.contains(qm))
                            break;
                    }
                    whenQueueDone(qm, startItem, stopItem, schedule, executor);
                });
            }
        } else if (schedule.isEnabled() && schedule.isOnceDownload())
            currentSchedules.get(schedule.getId()).getStartScheduler().shutdown();

    }

    private static void startFromSchedule(QueueModel qm, MenuItem startItem, MenuItem stopItem, ExecutorService executor) {
        var simulDownloads = new AtomicInteger(0);
        var schedule = qm.getSchedule();
        executor.submit(() -> {
            Thread.currentThread().setName("queue_runner");
            for (int i = 0; i < qm.getDownloads().size(); i++) {
                var dm = qm.getDownloads().get(i);
                if (dm.getDownloadStatus() == DownloadStatus.Paused) {
                    dm = mainTableUtils.getObservedDownload(dm);
                    String speedLimit = null;
                    var sDownloads = qm.getSimultaneouslyDownload();
                    if (qm.getSpeed() != null)
                        speedLimit = qm.getSpeed();
                    if (sDownloads > 1) {
                        if (simulDownloads.get() < sDownloads) {
                            DownloadOpUtils.startDownload(mainTableUtils, dm, speedLimit,
                                    null, true, false, null);
                            simulDownloads.getAndIncrement();
                        } else {
                            while (true) {
                                var count = currentDownloadings.stream().filter(d -> d.getQueues().contains(qm)).count();
                                if (!(count >= simulDownloads.get())) {
                                    simulDownloads.set((int) count);
                                    i--;
                                    break;
                                }
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                    } else DownloadOpUtils.startDownload(mainTableUtils, dm, speedLimit,
                            null, true, true, null);
                }
                if (!startedQueues.contains(qm))
                    break;
            }

            while (true) {
                var count = currentDownloadings.stream().filter(d -> d.getQueues().contains(qm)).count();
                if (count == 0) {
                    whenQueueDone(qm, startItem, stopItem, schedule, executor);
                    break;
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    private static void whenQueueDone(QueueModel qm, MenuItem startItem, MenuItem stopItem,
                                      ScheduleModel schedule, ExecutorService executor) {
        startItem.setDisable(false);
        stopItem.setDisable(true);
        if (startedQueues.contains(qm))
            queueDoneNotification(qm);
        startedQueues.remove(qm);
        shutdownSchedulersOnOnceDownload(schedule);
        if (schedule.isEnabled() && schedule.isTurnOffEnabled())
            PowerUtils.turnOff(schedule.getTurnOffMode());
        executor.shutdown();
    }


    public static void stopQueue(QueueModel qm, MenuItem startItem, MenuItem stopItem, MainTableUtils mainTableUtils) {
        if (startedQueues.contains(qm)) {
            startItem.setDisable(false);
            stopItem.setDisable(true);
            var downloadsByQueue = startedQueues.get(startedQueues.indexOf(qm)).getDownloads();
            downloadsByQueue.forEach(dm -> {
                dm = mainTableUtils.getObservedDownload(dm);
                DownloadOpUtils.pauseDownload(dm);
            });

            startedQueues.remove(qm);
            queueDoneNotification(qm);
            var schedule = qm.getSchedule();
            if (schedule.isEnabled() && schedule.isTurnOffEnabled())
                PowerUtils.turnOff(schedule.getTurnOffMode());
        }
    }

    private static void shutdownSchedulersOnOnceDownload(ScheduleModel schedule) {
        if (schedule.isEnabled() && schedule.isOnceDownload()) {
            currentSchedules.get(schedule.getId()).getStartScheduler().shutdown();
            var stopScheduler = currentSchedules.get(schedule.getId()).getStopScheduler();
            if (stopScheduler != null) stopScheduler.shutdownNow();
        }
    }

    private static void queueDoneNotification(QueueModel qm) {
        Runnable Notification = () -> Notifications.create()
                .title("Queue finished")
                .text("Queue %s finished or stopped".formatted(qm))
                .showInformation();

        if (Platform.isFxApplicationThread())
            Notification.run();
        else
            Platform.runLater(Notification);
    }

    public static void createFolders() {
        getQueues().stream().filter(QueueModel::hasFolder)
                .forEach(qm -> IOUtils.createFolder("Queues" + File.separator + qm.getName()));
    }

}
