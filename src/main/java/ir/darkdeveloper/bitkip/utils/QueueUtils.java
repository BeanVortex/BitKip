package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import javafx.application.Platform;
import javafx.scene.control.MenuItem;
import org.controlsfx.control.Notifications;

import java.util.Comparator;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.startedQueues;

public class QueueUtils {
    public static void startQueue(QueueModel qm, MenuItem startItem, MenuItem stopItem, MainTableUtils mainTableUtils) {
        if (!startedQueues.contains(qm)) {
            var downloadsByQueue = DownloadsRepo.getDownloadsByQueueName(qm.getName())
                    .stream().filter(dm -> dm.getDownloadStatus() == DownloadStatus.Paused)
                    .sorted(Comparator.comparing(DownloadModel::getAddToQueueDate))
                    .toList();
            if (downloadsByQueue.isEmpty()) {
                queueDoneNotif(qm);
                return;
            }
            startItem.setDisable(true);
            stopItem.setDisable(false);
            startedQueues.add(qm);
            var executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                for (var dm : downloadsByQueue) {
                    dm = mainTableUtils.getObservedDownload(dm);
                    DownloadOpUtils.startDownload(mainTableUtils, dm, null, null, true, true, executor);
                    if (!startedQueues.contains(qm))
                        break;
                }
                startItem.setDisable(false);
                stopItem.setDisable(true);
                if (startedQueues.contains(qm))
                    queueDoneNotif(qm);
                startedQueues.remove(qm);
                executor.shutdown();
            });
        }
    }


    public static void stopQueue(QueueModel qm, MenuItem startItem, MenuItem stopItem, MainTableUtils mainTableUtils) {
        if (startedQueues.contains(qm)) {
            var downloadsByQueue = DownloadsRepo.getDownloadsByQueueName(qm.getName())
                    .stream().filter(dm -> dm.getDownloadStatus() != DownloadStatus.Completed).toList();
            startItem.setDisable(false);
            stopItem.setDisable(true);
            startedQueues.remove(qm);
            downloadsByQueue.forEach(dm -> {
                dm = mainTableUtils.getObservedDownload(dm);
                DownloadOpUtils.pauseDownload(dm);
            });
            queueDoneNotif(qm);
        }
    }

    private static void queueDoneNotif(QueueModel qm) {
        Runnable showNotif = () -> Notifications.create()
                .title("Queue finished")
                .text("Queue %s finished or stopped".formatted(qm))
                .showInformation();

        if (Platform.isFxApplicationThread())
            showNotif.run();
        else
            Platform.runLater(showNotif);
    }
}
