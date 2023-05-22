package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.controllers.DownloadingController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.task.DownloadInChunksTask;
import ir.darkdeveloper.bitkip.task.DownloadLimitedTask;
import ir.darkdeveloper.bitkip.task.DownloadTask;
import javafx.collections.ObservableList;
import org.controlsfx.control.Notifications;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.sun.jna.Platform.*;
import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.IOUtils.getBytesFromString;

public class DownloadOpUtils {

    /**
     * @param blocking of course, it should be done in concurrent environment otherwise it will block the main thread.
     *                 mostly using for queue downloading
     */
    private static void triggerDownload(DownloadModel dm, String speed, String bytes, boolean resume, boolean blocking,
                                        ExecutorService executor) {
        DownloadTask downloadTask = new DownloadLimitedTask(dm, Long.MAX_VALUE, false);
        if (dm.getChunks() == 0) {
            if (speed != null) {
                if (speed.equals("0")) {
                    if (bytes != null) {
                        if (bytes.equals(dm.getSize() + ""))
                            downloadTask = new DownloadLimitedTask(dm, Long.MAX_VALUE, false);
                        else
                            downloadTask = new DownloadLimitedTask(dm, Long.parseLong(bytes), false);
                    }
                } else
                    downloadTask = new DownloadLimitedTask(dm, getBytesFromString(speed), true);
            }
        } else {
            if (speed != null) {
                if (speed.equals("0"))
                    downloadTask = new DownloadInChunksTask(dm, null);
                else
                    downloadTask = new DownloadInChunksTask(dm, getBytesFromString(speed));
            } else
                downloadTask = new DownloadInChunksTask(dm, null);
        }

        downloadTask.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue == null)
                oldValue = newValue;
            var currentSpeed = (newValue - oldValue);
            if (newValue == 0)
                currentSpeed = 0;
            mainTableUtils.updateDownloadSpeedAndRemaining(currentSpeed, dm, newValue);
        });
        downloadTask.progressProperty().addListener((o, old, newV) ->
                mainTableUtils.updateDownloadProgress(newV.floatValue() * 100, dm));
        downloadTask.setBlocking(blocking);
        dm.setDownloadTask(downloadTask);
        if (!resume) {
            DownloadsRepo.insertDownload(dm);
            mainTableUtils.addRow(dm);
        }
        currentDownloadings.add(dm);
        if (executor == null)
            executor = Executors.newCachedThreadPool();
        downloadTask.setExecutor(executor);
        log.info(("Starting %s download in " + (blocking ? "blocking" : "non-blocking")).formatted(dm.getName()));
        if (blocking)
            downloadTask.run();
        else
            executor.submit(downloadTask);

    }
    public static void startDownload(DownloadModel dm, String speedLimit, String byteLimit, boolean resume,
                                     boolean blocking, ExecutorService executor) {
        if (!currentDownloadings.contains(dm)) {
            log.info("Starting download : " + dm);
            dm.setLastTryDate(LocalDateTime.now());
            dm.setDownloadStatus(DownloadStatus.Trying);
            DownloadsRepo.updateDownloadLastTryDate(dm);
            mainTableUtils.refreshTable();
            triggerDownload(dm, speedLimit, byteLimit, resume, blocking, executor);
            openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                    .forEach(DownloadingController::initDownloadListeners);
        }
    }

    /**
     * Resumes downloads non-blocking
     * */
    public static void resumeDownloads(List<DownloadModel> dms, String speedLimit, String byteLimit) {
        dms.stream().filter(dm -> !currentDownloadings.contains(dm))
                .forEach(dm -> {
                    dm.setLastTryDate(LocalDateTime.now());
                    dm.setDownloadStatus(DownloadStatus.Trying);
                    dm.setShowCompleteDialog(showCompleteDialog);
                    dm.setOpenAfterComplete(false);
                    DownloadsRepo.updateDownloadLastTryDate(dm);
                    mainTableUtils.refreshTable();
                    if (dm.isResumable()) {
                        log.info("Resuming download : " + dm);
                        startDownload(dm, speedLimit, byteLimit, true, false, null);
                    } else {
                        dm.setDownloadStatus(DownloadStatus.Restarting);
                        mainTableUtils.refreshTable();
                        restartDownload(dm);
                    }
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                            .forEach(DownloadingController::initDownloadListeners);
                });
    }

    public static void restartDownloads(List<DownloadModel> dms) {
        var header = "Restarting download(s)";
        var v = "Are you sure you want to restart ";
        var content = dms.size() == 1 ? v + dms.get(0).getName() + " ?" : v + "selected downloads?";
        content += "\nIf the files exist, they will be deleted";
        var copiedDms = new ArrayList<>(dms);
        if (FxUtils.askWarning(header, content))
            copiedDms.forEach(DownloadOpUtils::restartDownload);
    }

    private static void restartDownload(DownloadModel dm) {
        log.info("Restarting download : " + dm);
        IOUtils.deleteDownload(dm);
        DownloadsRepo.deleteDownload(dm);
        mainTableUtils.remove(dm);
        triggerDownload(dm, null, null, false, false, null);
    }

    public static void pauseDownloads(List<DownloadModel> dms) {
        dms.forEach(DownloadOpUtils::pauseDownload);
    }

    public static void pauseDownload(DownloadModel dm) {
        log.info("Pausing download : " + dm);
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
                var logMsg = "download deleted: ";
                DownloadsRepo.deleteDownload(dm);
                if (withFiles) {
                    IOUtils.deleteDownload(dm);
                    logMsg = "download deleted with file: ";
                }
                log.info(logMsg + dm);
                var openDownloadingsCopy = new ArrayList<>(openDownloadings);
                openDownloadingsCopy.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                        .forEach(DownloadingController::closeStage);
            });

            mainTableUtils.remove(dms);
        }
    }

    public static void newDownload(boolean isSingle) {
        mainTableUtils.clearSelection();
        FxUtils.newDownloadStage(isSingle, null);
    }

    public static void openDownloadingStage(DownloadModel dm) {
        FxUtils.newDownloadingStage(dm);
    }

    public static void openFiles(ObservableList<DownloadModel> dms) {
        dms.filtered(dm -> dm.getDownloadStatus() == DownloadStatus.Completed)
                .forEach(DownloadOpUtils::openFile);
    }

    public static void openFile(DownloadModel dm) {
        var desktop = Desktop.getDesktop();
        log.info("Opening file : " + dm.getFilePath());
        if (!new File(dm.getFilePath()).exists()) {
            Notifications.create()
                    .title("File not found")
                    .text("%s has been moved or removed".formatted(dm.getName()))
                    .showError();
            log.error("File does not exists : " + dm.getFilePath());
            return;
        }
        try {
            if (Desktop.isDesktopSupported())
                desktop.open(new File(dm.getFilePath()));
            else
                log.warn("Desktop is not supported to open file");
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    public static void openContainingFolder(DownloadModel dm) {
        try {
            var desktop = Desktop.getDesktop();
            File file = new File(dm.getFilePath());
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                log.info("Opening containing folder");
                if (isWindows())
                    Runtime.getRuntime().exec(new String[]{"explorer", "/select,", file.getAbsolutePath()});
                else if (isLinux() || isSolaris() || isFreeBSD() || isOpenBSD())
                    Runtime.getRuntime().exec(new String[]{"xdg-open", file.getParentFile().getAbsolutePath()});
                else if (isMac())
                    Runtime.getRuntime().exec(new String[]{"osascript", "-e",
                            "tell app \"Finder\" to reveal POSIX file \"" + file.getAbsolutePath() + "\""});
            } else {
                log.warn("Desktop is not supported to open containing folder");
                Notifications.create()
                        .title("Not Supported")
                        .text("Your operating system does not support this action")
                        .showError();
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            Notifications.create()
                    .title("Error opening containing folder")
                    .text(e.getMessage())
                    .showError();
        }
    }

    public static void refreshDownload(ObservableList<DownloadModel> selected) {
        if (selected.size() != 1)
            return;
        FxUtils.newRefreshStage(selected.get(0));
    }
}
