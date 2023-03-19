package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.controllers.DownloadingController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentDownloadings;
import static ir.darkdeveloper.bitkip.config.AppConfigs.openDownloadings;

public class DownloadOpUtils {

    public static void resumeDownloads(MainTableUtils mainTableUtils, List<DownloadModel> modelList,
                                       String speedLimit, String byteLimit) {
        modelList.stream().filter(dm -> !currentDownloadings.contains(dm))
                .forEach(dm -> {
                    dm.setLastTryDate(LocalDateTime.now());
                    dm.setDownloadStatus(DownloadStatus.Trying);
                    DownloadsRepo.updateDownloadLastTryDate(dm);
                    mainTableUtils.refreshTable();
                    NewDownloadUtils.startDownload(dm, mainTableUtils, speedLimit, byteLimit, true);
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                            .forEach(DownloadingController::initDownloadListeners);
                });
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
}
