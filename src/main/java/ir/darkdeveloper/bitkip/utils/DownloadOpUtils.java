package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.controllers.DownloadingController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import javafx.collections.ObservableList;
import org.controlsfx.control.Notifications;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.repo.DownloadsRepo.COL_PATH;

public class DownloadOpUtils {

    /*
     * Resumes downloads non-blocking
     * */
    public static void resumeDownloads(List<DownloadModel> dms, String speedLimit, String byteLimit) {
        dms.stream().filter(dm -> !currentDownloadings.contains(dm))
                .forEach(dm -> {
                    dm.setLastTryDate(LocalDateTime.now());
                    dm.setDownloadStatus(DownloadStatus.Trying);
                    DownloadsRepo.updateDownloadLastTryDate(dm);
                    mainTableUtils.refreshTable();
                    if (dm.isResumable())
                        NewDownloadUtils.startDownload(dm, speedLimit, byteLimit, true, false, null);
                    else {
                        dm.setDownloadStatus(DownloadStatus.Restarting);
                        mainTableUtils.refreshTable();
                        restartDownload(dm);
                    }
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                            .forEach(DownloadingController::initDownloadListeners);
                });
    }

    public static void startDownload(DownloadModel dm, String speedLimit, String byteLimit, boolean resume,
                                     boolean blocking, ExecutorService executor) {
        if (!currentDownloadings.contains(dm)) {
            dm.setLastTryDate(LocalDateTime.now());
            dm.setDownloadStatus(DownloadStatus.Trying);
            DownloadsRepo.updateDownloadLastTryDate(dm);
            mainTableUtils.refreshTable();
            NewDownloadUtils.startDownload(dm, speedLimit, byteLimit, resume, blocking, executor);
            openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                    .forEach(DownloadingController::initDownloadListeners);
        }
    }

    public static void restartDownloads(List<DownloadModel> dms) {
        var header = "Restarting download(s)";
        var v = "Are you sure you want to restart ";
        var content = dms.size() == 1 ? v + dms.get(0).getName() + " ?" : v + "selected downloads?";
        content += "\nIf the files exist, they will be deleted";
        if (FxUtils.askWarning(header, content))
            dms.forEach(DownloadOpUtils::restartDownload);
    }

    private static void restartDownload(DownloadModel dm) {
        IOUtils.deleteDownload(dm);
        DownloadsRepo.deleteDownload(dm);
        mainTableUtils.remove(dm);
        startDownload(dm, null, null, false, false, null);
    }

    public static void pauseDownloads(List<DownloadModel> dms) {
        dms.forEach(DownloadOpUtils::pauseDownload);
    }

    public static void pauseDownload(DownloadModel dm) {
        currentDownloadings.stream()
                .filter(c -> c.equals(dm))
                .findFirst().ifPresent(dm2 -> dm2.getDownloadTask().pause());
    }

    public static void deleteDownloads(ObservableList<DownloadModel> dms, boolean withFiles) {
        if (dms.size() == 0)
            return;
        var header = "Delete selected downloads?";
        if (dms.size() == 1)
            header = "Delete " + dms.get(0).getName();
        var content = "Are you sure you want to delete selected download(s)?";
        if (withFiles)
            content += "\nFiles are deleted";
        if (FxUtils.askWarning(header, content)) {
            dms.forEach(dm -> {
                currentDownloadings.stream().filter(c -> c.equals(dm))
                        .findFirst()
                        .ifPresent(dm2 -> dm2.getDownloadTask().pause());
                DownloadsRepo.deleteDownload(dm);
                if (withFiles)
                    IOUtils.deleteDownload(dm);
                var openDownloadingsCopy = new ArrayList<>(openDownloadings);
                openDownloadingsCopy.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                        .forEach(DownloadingController::closeStage);
            });

            mainTableUtils.remove(dms);
        }
    }

    public static void newDownload(boolean isSingle) {
        mainTableUtils.clearSelection();
        FxUtils.newDownloadStage(isSingle);
    }

    public static void openDownloadingStage(DownloadModel dm) {
        FxUtils.newDownloadingStage(dm);
    }

    public static void openFiles(ObservableList<DownloadModel> dms) {
        var desktop = Desktop.getDesktop();
        dms.filtered(dm -> dm.getDownloadStatus() == DownloadStatus.Completed)
                .forEach(dm -> openFile(desktop, dm));
    }

    public static void openFile(Desktop desktop, DownloadModel dm) {
        if (desktop == null)
            desktop = Desktop.getDesktop();
        if (!new File(dm.getFilePath()).exists()) {
            Notifications.create()
                    .title("File not found")
                    .text("%s has been moved or removed".formatted(dm.getName()))
                    .showError();
            return;
        }
        try {
            if (Desktop.isDesktopSupported())
                desktop.open(new File(dm.getFilePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void moveFiles(DownloadModel dm, String newFilePath) {
        if (dm.getProgress() != 100) {
            if (dm.getChunks() != 0)
                for (int i = 0; i < dm.getChunks(); i++)
                    IOUtils.moveFile(dm.getFilePath() + "#" + i, newFilePath + "#" + i);
            else
                IOUtils.moveFile(dm.getFilePath(), newFilePath);
        } else
            IOUtils.moveFile(dm.getFilePath(), newFilePath);
        DownloadsRepo.updateDownloadProperty(COL_PATH, "\"" + newFilePath + "\"", dm.getId());
    }

    public static void refreshDownload(ObservableList<DownloadModel> selected) {
        if (selected.size() != 1)
            return;
        FxUtils.newRefreshStage(selected.get(0));
    }
}
