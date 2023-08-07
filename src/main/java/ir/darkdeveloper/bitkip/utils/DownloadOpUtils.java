package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.controllers.DetailsController;
import ir.darkdeveloper.bitkip.exceptions.DeniedException;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.SingleURLModel;
import ir.darkdeveloper.bitkip.models.TurnOffMode;
import ir.darkdeveloper.bitkip.repo.DatabaseHelper;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.task.ChunksDownloadTask;
import ir.darkdeveloper.bitkip.task.SpecialDownloadTask;
import ir.darkdeveloper.bitkip.task.DownloadTask;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import org.controlsfx.control.Notifications;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.sun.jna.Platform.*;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.repo.DownloadsRepo.*;
import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.DownloadUtils.getNewFileNameIfExists;
import static ir.darkdeveloper.bitkip.utils.Validations.maxChunks;

public class DownloadOpUtils {

    /**
     * @param blocking of course, it should be done in concurrent environment otherwise it will block the main thread.
     *                 mostly using for queue downloading
     */
    public static void triggerDownload(DownloadModel dm, long speed, long bytes, boolean resume, boolean blocking,
                                       ExecutorService executor) throws ExecutionException, InterruptedException {

        try {
            Validations.fillNotFetchedData(dm);
            IOUtils.checkAvailableSpace(dm.getFilePath(), dm.getSize());
        } catch (IOException e) {
            log.error(e.getMessage());
            var observedDownload = mainTableUtils.getObservedDownload(dm);
            dm.setDownloadStatus(DownloadStatus.Paused);
            if (observedDownload != null)
                observedDownload.setDownloadStatus(dm.getDownloadStatus());

            return;
        }

        DownloadTask downloadTask;
        if (dm.getChunks() == 0)
            downloadTask = new SpecialDownloadTask(dm);
        else {
            try {
                if (dm.getSize() > 0)
                    bytes = dm.getSize();
                downloadTask = new ChunksDownloadTask(dm, speed, bytes);
            } catch (DeniedException e) {
                log.error(e.getMessage());
                return;
            }
        }

        if (dm.getSize() == -1)
            downloadTask.valueProperty().addListener((ob, o, n) -> mainTableUtils.updateDownloadedNoSize(n, dm));
        else
            downloadTask.valueProperty().addListener((ob, o, n) -> {
                if (o == null)
                    o = n;
                var currentSpeed = (n - o);
                if (n <= 0)
                    currentSpeed = 0;
                mainTableUtils.updateDownloadSpeedAndRemaining(currentSpeed, dm, n);
            });

        downloadTask.progressProperty().addListener((ob, o, n) ->
                mainTableUtils.updateDownloadProgress(n.floatValue() * 100, dm));
        downloadTask.setBlocking(blocking);
        dm.setDownloadTask(downloadTask);
        currentDownloadings.add(dm);
        openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                .forEach(DetailsController::initDownloadListeners);
        if (!resume) {
            DownloadsRepo.insertDownload(dm);
            mainTableUtils.addRow(dm);
        }
        if (executor == null)
            executor = Executors.newCachedThreadPool();
        downloadTask.setExecutor(executor);
        log.info(("Starting download in " + (blocking ? "blocking" : "non-blocking") + ": %s").formatted(dm));
        if (blocking)
            downloadTask.runBlocking();
        else
            executor.submit(downloadTask);

    }

    public static void startDownload(DownloadModel dm, long speedLimit, long byteLimit, boolean resume,
                                     boolean blocking, ExecutorService executor) {
        if (!currentDownloadings.contains(dm)) {
            dm.setLastTryDate(LocalDateTime.now());
            dm.setDownloadStatus(DownloadStatus.Trying);
            dm.setTurnOffMode(TurnOffMode.NOTHING);
            DownloadsRepo.updateDownloadLastTryDate(dm);
            mainTableUtils.refreshTable();
            try {
                triggerDownload(dm, speedLimit, byteLimit, resume, blocking, executor);
            } catch (ExecutionException | InterruptedException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * Resumes downloads non-blocking
     */
    public static void resumeDownloads(List<DownloadModel> dms, long speedLimit, long byteLimit) {
        dms.stream().filter(dm -> !currentDownloadings.contains(dm))
                .forEach(dm -> {
                    dm.setLastTryDate(LocalDateTime.now());
                    dm.setDownloadStatus(DownloadStatus.Trying);
                    dm.setShowCompleteDialog(showCompleteDialog);
                    dm.setOpenAfterComplete(false);
                    dm.setSpeedLimit(speedLimit);
                    DownloadsRepo.updateDownloadLastTryDate(dm);
                    mainTableUtils.refreshTable();
                    if (dm.isResumable()) {
                        log.info("Resuming download : " + dm);
                        startDownload(dm, speedLimit, byteLimit, true, false, null);
                    } else
                        restartDownload(dm);
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                            .forEach(DetailsController::initDownloadListeners);
                });
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
        log.info("Restarting download : " + dm);
        IOUtils.deleteDownload(dm);
        var lastTryDate = LocalDateTime.now();
        var dmId = dm.getId();
        dm.setDownloaded(0);
        dm.setProgress(0);
        dm.setCompleteDate(null);
        dm.setLastTryDate(lastTryDate);
        dm.setDownloadStatus(DownloadStatus.Restarting);
        mainTableUtils.refreshTable();
        String[] cols = {COL_DOWNLOADED, COL_PROGRESS, COL_COMPLETE_DATE, COL_LAST_TRY_DATE};
        String[] values = {"0", "0", "NULL", lastTryDate.toString()};
        DatabaseHelper.updateCols(cols, values, DatabaseHelper.DOWNLOADS_TABLE_NAME, dmId);
        try {
            triggerDownload(dm, 0, dm.getSize(), true, false, null);
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage());
        }
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
                var logMsg = "download deleted: ";
                DownloadsRepo.deleteDownload(dm);
                if (withFiles) {
                    IOUtils.deleteDownload(dm);
                    logMsg = "download deleted with file: ";
                }
                log.info(logMsg + dm);
                var openDownloadingsCopy = new ArrayList<>(openDownloadings);
                openDownloadingsCopy.stream().filter(dc -> dc.getDownloadModel().equals(dm))
                        .forEach(DetailsController::closeStage);
            });

            mainTableUtils.remove(dms);
        }
    }

    public static void newDownload(boolean isSingle) {
        mainTableUtils.clearSelection();
        FxUtils.newDownloadStage(isSingle, null);
    }

    public static void openDetailsStage(DownloadModel dm) {
        if (!Platform.isFxApplicationThread())
            Platform.runLater(() -> FxUtils.newDetailsStage(dm));
        else
            FxUtils.newDetailsStage(dm);
    }

    public static void openFiles(ObservableList<DownloadModel> dms) {
        dms.filtered(dm -> dm.getDownloadStatus() == DownloadStatus.Completed)
                .forEach(DownloadOpUtils::openFile);
    }

    public static void openFile(DownloadModel dm) {
        log.info("Opening file : " + dm.getFilePath());
        if (!new File(dm.getFilePath()).exists()) {
            Platform.runLater(() -> Notifications.create()
                    .title("File not found")
                    .text("%s has been moved or removed".formatted(dm.getName()))
                    .showError());
            log.error("File does not exists : " + dm.getFilePath());
            return;
        }
        try {
            var desktop = Desktop.getDesktop();
            var filePath = dm.getFilePath();
            var os = System.getProperty("os.name").toLowerCase();
            var isLinux = os.contains("nix") || os.contains("nux") || os.contains("bsd");
            if (Desktop.isDesktopSupported() && !isLinux)
                desktop.open(new File(filePath));
            else {
                ProcessBuilder processBuilder;
                if (os.contains("win"))
                    // For Windows
                    processBuilder = new ProcessBuilder("cmd.exe", "/c", "start", filePath);
                else if (os.contains("mac"))
                    processBuilder = new ProcessBuilder("open", filePath);
                else if (isLinux)
                    processBuilder = new ProcessBuilder("xdg-open", filePath);
                else
                    throw new UnsupportedOperationException("Unsupported operating system: " + os);
                processBuilder.start();
            }
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

    public static void importLinks(ActionEvent e) {
        var links = IOUtils.readLinksFromFile(e);
        if (links == null || links.isEmpty()) {
            log.warn("No links found in the file");
            Notifications.create()
                    .title("No links found")
                    .text("The file you choose does not have any http url, make sure that links are separated by enter character")
                    .showWarning();
            return;
        }
        FxUtils.newBatchListStage(links);
    }

    public static void exportLinks(String queue) {
        try {
            var urls = DownloadsRepo.getDownloadsByQueueName(queue)
                    .stream().map(DownloadModel::getUrl)
                    .toList();
            IOUtils.writeLinksToFile(urls, queue);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            Notifications.create()
                    .title("Some unexpected thing happened")
                    .text(e.getLocalizedMessage())
                    .showError();
            return;
        }
        Notifications.create()
                .title("Export successful")
                .text("File exported successfully to " + exportedLinksPath)
                .showInformation();
    }

    public static void exportLinks(List<String> urls) {
        try {
            IOUtils.writeLinksToFile(urls, "selected");
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            Notifications.create()
                    .title("Some unexpected thing happened")
                    .text(e.getLocalizedMessage())
                    .showError();
            return;
        }
        Notifications.create()
                .title("Export successful")
                .text("File exported successfully to " + exportedLinksPath)
                .showInformation();
    }

    public static void downloadImmediately(SingleURLModel urlModel) {
        var dm = new DownloadModel();
        var url = urlModel.url();
        var fileName = urlModel.filename();
        var fileSize = urlModel.fileSize();
        dm.setUrl(url);
        try {
            var conn = DownloadUtils.connect(url);
            var canResume = DownloadUtils.canResume(conn);
            dm.setResumable(canResume);
            dm.setChunks(canResume ? maxChunks(fileSize) : 0);
            dm.setProgress(0);
            dm.setSize(fileSize);
            dm.setAddDate(LocalDateTime.now());
            dm.setAddToQueueDate(LocalDateTime.now());
            dm.setShowCompleteDialog(showCompleteDialog);
            dm.setOpenAfterComplete(false);
            var path = DownloadUtils.determineLocation(fileName);
            if (addSameDownload)
                fileName = getNewFileNameIfExists(fileName, path);
            dm.setFilePath(path + fileName);
            var newFileName = getNewFileNameIfExists(fileName, path);
            fileName = addSameDownload ? newFileName : fileName;
            if (!newFileName.equals(fileName))
                throw new DeniedException("This url and name exists for this location. Change location or name");

            dm.setName(fileName);

            var queue = DownloadUtils.determineQueue(fileName);
            var allDownloadsQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, false);
            dm.getQueues().add(allDownloadsQueue);
            dm.getQueues().add(queue);
            dm.setDownloadStatus(DownloadStatus.Trying);
            DownloadOpUtils.startDownload(dm, 0, dm.getSize(), false, false, null);
            Notifications.create()
                    .title("Downloading now ...")
                    .text(dm.getName())
                    .showInformation();
        } catch (IOException | DeniedException e) {
            log.error(e.getMessage());
            Notifications.create()
                    .title("Failed to download : " + dm.getName())
                    .text(e.getMessage())
                    .showWarning();
        }

    }

    public static void pauseAllDownloads() {
        var cpyStartedQueues = new ArrayList<>(startedQueues);
        cpyStartedQueues.forEach(q -> QueueUtils.stopQueue(q, false));
        pauseDownloads(currentDownloadings);
    }
}
