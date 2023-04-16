package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.*;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.repo.ScheduleRepo;
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


    public static void startQueue(QueueModel qm, MenuItem startItem, MenuItem stopItem) {
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
            start(qm, startItem, stopItem);
        } else if (schedule.isEnabled() && schedule.isOnceDownload())
            currentSchedules.get(schedule.getId()).getStartScheduler().shutdown();

    }

    private static void start(QueueModel qm, MenuItem startItem, MenuItem stopItem) {
        var executor = Executors.newCachedThreadPool();
        var simulDownloads = new AtomicInteger(0);
        executor.submit(() -> {
            var sDownloads = qm.getSimultaneouslyDownload();
            for (int i = 0; i < qm.getDownloads().size(); i++) {
                var dm = qm.getDownloads().get(i);
                if (dm.getDownloadStatus() == DownloadStatus.Paused) {
                    dm = mainTableUtils.getObservedDownload(dm);
                    if (!dm.getQueues().contains(qm))
                        dm.getQueues().add(qm);
                    String speedLimit = null;
                    if (qm.getSpeed() != null)
                        speedLimit = qm.getSpeed();
                    if (sDownloads > 1)
                        i = performSimultaneousDownload(qm, simulDownloads, i, dm, speedLimit, sDownloads);
                    else DownloadOpUtils.startDownload(dm, speedLimit,
                            null, true, true, null);
                }
                if (!startedQueues.contains(qm))
                    break;
            }

            if (sDownloads == 1)
                whenQueueDone(qm, startItem, stopItem, executor);
        });

    }

    private static int performSimultaneousDownload(QueueModel qm, AtomicInteger simulDownloads,
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
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return i;
    }

    private static void whenQueueDone(QueueModel qm, MenuItem startItem, MenuItem stopItem, ExecutorService executor) {
        startItem.setDisable(false);
        stopItem.setDisable(true);
        queueDoneNotification(qm);
        startedQueues.remove(qm);
        shutdownSchedulersOnOnceDownload(qm);
        var schedule = qm.getSchedule();
        if (schedule.isEnabled() && schedule.isTurnOffEnabled())
            PowerUtils.turnOff(schedule.getTurnOffMode());
        if (executor != null)
            executor.shutdown();
    }


    public static void stopQueue(QueueModel qm, MenuItem startItem, MenuItem stopItem) {
        if (startedQueues.contains(qm)) {
            var downloadsByQueue = startedQueues.get(startedQueues.indexOf(qm)).getDownloads();
            downloadsByQueue.forEach(dm -> {
                dm = mainTableUtils.getObservedDownload(dm);
                DownloadOpUtils.pauseDownload(dm);
            });

            startedQueues.remove(qm);
            whenQueueDone(qm, startItem, stopItem, null);
        }
    }

    private static void shutdownSchedulersOnOnceDownload(QueueModel qm) {
        var schedule = qm.getSchedule();
        if (schedule.isEnabled() && schedule.isOnceDownload()) {
            currentSchedules.get(schedule.getId()).getStartScheduler().shutdown();
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
                .forEach(qm -> IOUtils.createFolderInSaveLocation("Queues" + File.separator + qm.getName()));
    }

    public static void deleteQueue(String name) {
        var content = "Are you sure you want to delete '" + name + "' queue?\nFiles are not deleted";
        var header = "Delete '" + name + "' ?";
        if (FxUtils.askWarning(header, content)) {
            moveFilesAndDeleteQueueFolder(name);
            QueuesRepo.deleteQueue(name);
            AppConfigs.deleteQueue(name);
        }
    }


    public static void moveFilesAndDeleteQueueFolder(String queueName) {
        var downloadsByQueueName = DownloadsRepo.getDownloadsByQueueName(queueName);
        downloadsByQueueName.forEach(dm -> {
            var newFilePath = FileType.determineFileType(dm.getName()).getPath() + dm.getName();
            DownloadOpUtils.moveFiles(dm, newFilePath);
        });
        IOUtils.removeFolder("Queues" + File.separator + queueName);
    }

}
