package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.application.Platform;
import javafx.scene.control.MenuItem;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;

public class QueueUtils {
    public static void startQueue(QueueModel qm, MenuItem startItem, MenuItem stopItem, MainTableUtils mainTableUtils) {
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
            executor.submit(() -> {
                for (int i = 0; i < qm.getDownloads().size(); i++) {
                    var dm = qm.getDownloads().get(i);
                    if (dm.getDownloadStatus() == DownloadStatus.Paused) {
                        dm = mainTableUtils.getObservedDownload(dm);
                        DownloadOpUtils.startDownload(mainTableUtils, dm, null, null, true,
                                true, executor);
                    }

                    if (!startedQueues.contains(qm))
                        break;
                }
                startItem.setDisable(false);
                stopItem.setDisable(true);
                if (startedQueues.contains(qm))
                    queueDoneNotification(qm);
                startedQueues.remove(qm);
                executor.shutdown();
            });
        }
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
                .forEach(qm -> {
                    var dir = new File(downloadPath + File.separator + qm.getName());
                    if (!dir.exists())
                        dir.mkdir();
                });
    }
}
