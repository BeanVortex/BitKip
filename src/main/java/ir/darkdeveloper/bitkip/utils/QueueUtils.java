package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.observers.QueueSubject;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.repo.ScheduleRepo;
import javafx.application.Platform;
import javafx.scene.control.MenuItem;
import org.controlsfx.control.Notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.config.observers.QueueSubject.addAllQueues;
import static ir.darkdeveloper.bitkip.config.observers.QueueSubject.getQueues;

public class QueueUtils {


    public static void startQueue(QueueModel qm, MenuItem startItem, MenuItem stopItem) {
        var schedule = qm.getSchedule();
        if (!startedQueues.contains(qm)) {
            var downloadsByQueue = new ArrayList<>(
                    QueuesRepo.findByName(qm.getName(), true)
                            .getDownloads()
                            .stream()
                            .sorted(Comparator.comparing(DownloadModel::getAddToQueueDate))
                            .toList()
            );
            var executor = Executors.newCachedThreadPool();
            if (downloadsByQueue.isEmpty()) {
                if (triggerTurnOffOnEmptyQueue)
                    whenQueueDone(qm, startItem, stopItem, executor);
                else{
                    queueDoneNotification(qm);
                    executor.shutdownNow();
                }
                return;
            }
            startItem.setDisable(true);
            stopItem.setDisable(false);
            if (qm.isDownloadFromTop())
                Collections.reverse(downloadsByQueue);
            downloadsByQueue = new ArrayList<>(downloadsByQueue.stream().map(mainTableUtils::getObservedDownload).toList());
            qm.setDownloads(new CopyOnWriteArrayList<>(downloadsByQueue));
            startedQueues.add(qm);
            start(qm, startItem, stopItem, executor);
            log.info("Queue has been started: " + qm);
        } else if (schedule.isEnabled() && schedule.isOnceDownload()) {
            // in case when user starts the queue manually which adds the queue to startedQueues and
            // when start scheduler runs, it shutdowns the start scheduler
            currentSchedules.get(schedule.getId()).getStartScheduler().shutdownNow();
            log.info("Start scheduler has been disabled for: " + qm.getName());
        }

    }

    private static void start(QueueModel qm, MenuItem startItem, MenuItem stopItem, ExecutorService executor) {
        executor.submit(() -> {
            var simulDownloads = new AtomicInteger(0);
            var sDownloads = qm.getSimultaneouslyDownload();
            for (int i = 0; i < qm.getDownloads().size(); i++) {
                var pauseCount = qm.getDownloads().stream()
                        .filter(dm -> dm.getDownloadStatus() == DownloadStatus.Paused)
                        .count();
                var dm = qm.getDownloads().get(i);
                if (dm.getDownloadStatus() == DownloadStatus.Paused) {
                    dm.setOpenAfterComplete(false);
                    dm.setShowCompleteDialog(false);
                    if (!dm.getQueues().contains(qm))
                        dm.getQueues().add(qm);
                    String speedLimit = null;
                    if (qm.getSpeed() != null)
                        speedLimit = qm.getSpeed();
                    if (sDownloads > 1) {
                        if (pauseCount >= sDownloads || (pauseCount > 0 && sDownloads == simulDownloads.get()))
                            i = performSimultaneousDownloadWaitForPrev(qm, simulDownloads, i, dm, speedLimit, sDownloads);
                        else if (pauseCount < sDownloads)
                            performSimultaneousDownloadDontWaitForPrev(
                                    sDownloads - simulDownloads.get(),
                                    simulDownloads,
                                    dm, speedLimit);
                    } else DownloadOpUtils.startDownload(dm, speedLimit, null, true, true, null);

                }
                if (!startedQueues.contains(qm))
                    break;
            }

            var pauseCount = qm.getDownloads().stream().filter(dm -> dm.getDownloadStatus() == DownloadStatus.Paused).count();
            // when queue is done or paused all manually and one simultaneously download
            if (sDownloads == 1 && startedQueues.contains(qm) && pauseCount != 0)
                whenQueueDone(qm, startItem, stopItem, executor);
            else
                waitToFinishForLessPausedDownloads(qm, startItem, stopItem, executor);

        });

    }


    /**
     * consider 7 files are going to download in parallel. 3 of them will get inside if-clause and the 4th one will be hold
     * in else clause until one of those 3 stops or finishes
     */
    private static int performSimultaneousDownloadWaitForPrev(QueueModel qm, AtomicInteger simulDownloads,
                                                              int i, DownloadModel dm, String speedLimit, int sDownloads) {
        if (simulDownloads.get() < sDownloads) {
            DownloadOpUtils.startDownload(dm, speedLimit,
                    null, true, false, null);
            simulDownloads.getAndIncrement();
        } else {
            while (true) {
                var count = currentDownloadings.stream().filter(d -> d.getQueues().contains(qm)).count();
                if (count < simulDownloads.get()) {
                    simulDownloads.set((int) count);
                    i--;
                    break;
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignore) {
                }
            }
        }
        return i;
    }

    /**
     * it is useful when queue simultaneously downloads are greater than current paused downloads in queue
     * it starts all downloads non-blocking and the waiting to finish is done later in waitToFinishForLessPausedDownloads method
     *
     * @see QueueUtils#waitToFinishForLessPausedDownloads(QueueModel, MenuItem, MenuItem, ExecutorService)
     */
    private static void performSimultaneousDownloadDontWaitForPrev(int remainingSimul, AtomicInteger simulDownloads,
                                                                   DownloadModel dm, String speedLimit) {
        if (remainingSimul != 0) {
            DownloadOpUtils.startDownload(dm, speedLimit, null, true, false, null);
            simulDownloads.getAndIncrement();
        }
    }


    /**
     * waits for non-blocking downloads to finish
     *
     * @see QueueUtils#performSimultaneousDownloadDontWaitForPrev(int, AtomicInteger, DownloadModel, String)
     */
    private static void waitToFinishForLessPausedDownloads(QueueModel qm, MenuItem startItem, MenuItem stopItem,
                                                           ExecutorService executor) {
        var count = currentDownloadings.stream().filter(d -> d.getQueues().contains(qm)).count();
        while (count != 0) {
            try {
                Thread.sleep(2000);
                count = currentDownloadings.stream().filter(d -> d.getQueues().contains(qm)).count();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (startedQueues.contains(qm))
            whenQueueDone(qm, startItem, stopItem, executor);
    }

    private static void whenQueueDone(QueueModel qm, MenuItem startItem, MenuItem stopItem, ExecutorService executor) {
        startItem.setDisable(false);
        stopItem.setDisable(true);
        queueDoneNotification(qm);
        startedQueues.remove(qm);
        var schedule = qm.getSchedule();
        if (schedule.isEnabled() && schedule.isTurnOffEnabled()) {
            shutdownSchedulersOnOnceDownload(qm);
            var turnOffMode = schedule.getTurnOffMode();
            Platform.runLater(() -> {
                log.info("Turn off triggered");
                if (FxUtils.askForShutdown(turnOffMode))
                    PowerUtils.turnOff(turnOffMode);
            });
        }
        shutdownSchedulersOnOnceDownload(qm);
        if (executor != null)
            executor.shutdownNow();
        log.info("Queue stopped automatically: " + qm.toStringModel());
    }


    public static void stopQueue(QueueModel qm, MenuItem startItem, MenuItem stopItem) {
        if (startedQueues.contains(qm)) {
            var downloadsByQueue = startedQueues.get(startedQueues.indexOf(qm)).getDownloads();
            downloadsByQueue.forEach(dm -> {
                dm = mainTableUtils.getObservedDownload(dm);
                DownloadOpUtils.pauseDownload(dm);
            });

            startedQueues.remove(qm);
            log.info("Queue has been stopped: " + qm);
            whenQueueDone(qm, startItem, stopItem, null);
        }
    }

    private static void shutdownSchedulersOnOnceDownload(QueueModel qm) {
        var schedule = qm.getSchedule();
        if (schedule.isEnabled() && schedule.isOnceDownload()) {
            currentSchedules.get(schedule.getId()).getStartScheduler().shutdownNow();
            var stopScheduler = currentSchedules.get(schedule.getId()).getStopScheduler();
            if (stopScheduler != null) stopScheduler.shutdownNow();
            schedule.setEnabled(false);
            var updatedQueues = getQueues().stream()
                    .peek(q -> {
                        if (q.equals(qm))
                            q.setSchedule(schedule);
                    }).toList();

            Platform.runLater(() -> addAllQueues(updatedQueues));
            ScheduleRepo.updateScheduleEnabled(schedule.getId(), schedule.isEnabled());
            log.info("Schedulers has been disabled for: " + qm);
        }
    }

    private static void queueDoneNotification(QueueModel qm) {
        Runnable Notification = () -> {
            // when app is in tray an exception is thrown
            try {
                Notifications.create()
                        .title("Queue finished")
                        .text("Queue %s finished or stopped".formatted(qm))
                        .showInformation();
            } catch (NullPointerException ignore) {
            }
        };

        if (Platform.isFxApplicationThread())
            Notification.run();
        else
            Platform.runLater(Notification);
    }


    public static void deleteQueue(String name) {
        var content = "Are you sure you want to delete '" + name + "' queue?\nFiles are not deleted";
        var header = "Delete '" + name + "' ?";
        if (FxUtils.askWarning(header, content)) {
            IOUtils.moveFilesAndDeleteQueueFolder(name);
            QueuesRepo.deleteQueue(name);
            QueueSubject.deleteQueue(name);
        }
    }
}