package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;

import java.time.LocalDateTime;

public class DownloadOpUtils {

    public static void resumeDownloads(MainTableUtils mainTableUtils) {
        var selectedItems = mainTableUtils.getSelected();
        selectedItems
                .filtered(dm -> !AppConfigs.currentDownloading.contains(dm))
                .forEach(dm -> {
                    dm.setLastTryDate(LocalDateTime.now());
                    dm.setDownloadStatus(DownloadStatus.Trying);
                    DownloadsRepo.updateDownloadLastTryDate(dm);
                    mainTableUtils.refreshTable();
                    NewDownloadUtils.startDownload(dm, mainTableUtils, null, null, true);
                });
    }

    public static void pauseDownloads(MainTableUtils mainTableUtils) {
        mainTableUtils.getSelected().forEach(dm -> {
            var index = AppConfigs.currentDownloading.indexOf(dm);
            if (index != -1) {
                var download = AppConfigs.currentDownloading.get(index);
                download.getDownloadTask().pause();
            }
        });
    }

    public static void deleteDownloads(MainTableUtils mainTableUtils, boolean withFiles) {
        var selectedItems = mainTableUtils.getSelected();
        selectedItems.forEach(dm -> {
            var index = AppConfigs.currentDownloading.indexOf(dm);
            if (index != -1)
                dm = AppConfigs.currentDownloading.get(index);
            if (dm.getDownloadTask() != null && dm.getDownloadTask().isRunning())
                dm.getDownloadTask().pause();
            DownloadsRepo.deleteDownload(dm);
            if (withFiles)
                IOUtils.deleteDownload(dm);

        });

        mainTableUtils.remove(selectedItems);
    }

    public static void newDownload(MainTableUtils mainTableUtils, boolean isBatch) {
        mainTableUtils.clearSelection();
        FxUtils.newDownloadStage("newDownload.fxml", 600, 550, mainTableUtils);
    }
}
