package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.controllers.DownloadingController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentDownloadings;
import static ir.darkdeveloper.bitkip.config.AppConfigs.openDownloadings;

public class DownloadOpUtils {

    public static void resumeDownloads(MainTableUtils mainTableUtils) {
        var selectedItems = mainTableUtils.getSelected();
        selectedItems
                .filtered(dm -> !currentDownloadings.contains(dm))
                .forEach(dm -> {
                    dm.setLastTryDate(LocalDateTime.now());
                    dm.setDownloadStatus(DownloadStatus.Trying);
                    DownloadsRepo.updateDownloadLastTryDate(dm);
                    mainTableUtils.refreshTable();
                    NewDownloadUtils.startDownload(dm, mainTableUtils, null, null, true);
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                            .forEach(DownloadingController::initDownloadListeners);
                });
    }

    public static void pauseDownloads(MainTableUtils mainTableUtils) {
        mainTableUtils.getSelected().forEach(dm -> {
            var index = currentDownloadings.indexOf(dm);
            if (index != -1) {
                var download = currentDownloadings.get(index);
                download.getDownloadTask().pause();
            }
        });
    }

    public static void deleteDownloads(MainTableUtils mainTableUtils, boolean withFiles) {
        var selectedItems = mainTableUtils.getSelected();
        selectedItems.forEach(dm -> {
            var index = currentDownloadings.indexOf(dm);

            if (index != -1)
                dm = currentDownloadings.get(index);
            if (dm.getDownloadTask() != null && dm.getDownloadTask().isRunning())
                dm.getDownloadTask().pause();
            DownloadsRepo.deleteDownload(dm);
            if (withFiles)
                IOUtils.deleteDownload(dm);
            var finalDm = dm;
            var openDownloadingsCopy = new ArrayList<>(openDownloadings);
            openDownloadingsCopy.stream().filter(dc -> dc.getDownloadModel().equals(finalDm))
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
}
