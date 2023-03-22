package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.controllers.DownloadingController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;

public class DownloadOpUtils {

    /*
     * Resumes downloads non-blocking
     * */
    public static void resumeDownloads(MainTableUtils mainTableUtils, List<DownloadModel> modelList,
                                       String speedLimit, String byteLimit) {
        modelList.stream().filter(dm -> !currentDownloadings.contains(dm))
                .forEach(dm -> {
                    dm.setLastTryDate(LocalDateTime.now());
                    dm.setDownloadStatus(DownloadStatus.Trying);
                    DownloadsRepo.updateDownloadLastTryDate(dm);
                    mainTableUtils.refreshTable();
                    NewDownloadUtils.startDownload(dm, mainTableUtils, speedLimit, byteLimit,
                            true, false, null);
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                            .forEach(DownloadingController::initDownloadListeners);
                });
    }

    /*
     * Starts or Resumes download non-blocking
     * */
    public static void startDownload(MainTableUtils mainTableUtils, DownloadModel dm, String speedLimit,
                                     String byteLimit, boolean resume, boolean blocking, ExecutorService executor) {
        if (!currentDownloadings.contains(dm)) {
            dm.setLastTryDate(LocalDateTime.now());
            dm.setDownloadStatus(DownloadStatus.Trying);
            DownloadsRepo.updateDownloadLastTryDate(dm);
            mainTableUtils.refreshTable();
            NewDownloadUtils.startDownload(dm, mainTableUtils, speedLimit, byteLimit, resume, blocking, executor);
            openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                    .forEach(DownloadingController::initDownloadListeners);
        }
    }

    public static void pauseDownloads(MainTableUtils mainTableUtils) {
        mainTableUtils.getSelected().forEach(dm ->
                currentDownloadings.stream().filter(c -> c.equals(dm))
                        .findAny().ifPresent(dm2 -> dm2.getDownloadTask().pause())
        );
    }

    public static void deleteDownloads(MainTableUtils mainTableUtils, boolean withFiles) {
        var selectedItems = mainTableUtils.getSelected();
        selectedItems.forEach(dm -> {
            currentDownloadings.stream().filter(c -> c.equals(dm))
                    .findAny()
                    .ifPresent(dm2 -> dm2.getDownloadTask().pause());
            DownloadsRepo.deleteDownload(dm);
            if (withFiles)
                IOUtils.deleteDownload(dm);
            var openDownloadingsCopy = new ArrayList<>(openDownloadings);
            openDownloadingsCopy.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                    .forEach(DownloadingController::closeStage);
        });

        mainTableUtils.remove(selectedItems);
    }

    public static void newDownload(MainTableUtils mainTableUtils, boolean isSingle) {
        mainTableUtils.clearSelection();
        FxUtils.newDownloadStage(mainTableUtils, isSingle);
    }

    public static void openDownloadingStage(DownloadModel dm, MainTableUtils mainTableUtils) {
        FxUtils.newDownloadingStage(dm, mainTableUtils);
    }

    public static void openFiles(ObservableList<DownloadModel> selected) {
        selected.filtered(dm -> dm.getDownloadStatus() == DownloadStatus.Completed)
                .forEach(dm -> {
                    if (!new File(dm.getFilePath()).exists()) {
                        Notifications.create()
                                .title("File not found")
                                .text("%s has been moved or removed".formatted(dm.getName()))
                                .showError();
                        return;
                    }
                    hostServices.showDocument(dm.getFilePath());
                });
    }

    public static void startDownloadsInQueue(LinkedHashMap<MenuItem, QueueModel> startQueueItems,
                                             MenuItem menuItem, MainTableUtils mainTableUtils) {
        var executor = Executors.newCachedThreadPool();
        executor.submit(() -> {
            var qm = startQueueItems.get(menuItem);
            var downloadsByQueue = DownloadsRepo.getDownloadsByQueue(qm.getId())
                    .stream().filter(dm -> dm.getDownloadStatus() == DownloadStatus.Paused).toList();
            downloadsByQueue.forEach(dm -> {
                dm = mainTableUtils.getObservedDownload(dm);
                startDownload(mainTableUtils, dm, null, null, true, true, executor);
            });
        });
    }
}
