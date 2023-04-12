package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.task.DownloadInChunksTask;
import ir.darkdeveloper.bitkip.task.DownloadLimitedTask;
import ir.darkdeveloper.bitkip.task.DownloadTask;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentDownloadings;
import static ir.darkdeveloper.bitkip.config.AppConfigs.showCompleteDialog;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.*;
import static ir.darkdeveloper.bitkip.utils.IOUtils.getBytesFromString;

public class NewDownloadUtils {


    public static HttpURLConnection connect(String uri, int connectTimeout, int readTimeout) {
        try {
            if (uri.isBlank())
                throw new IllegalArgumentException("Link is blank");
            var url = new URL(uri);
            var conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            return conn;
        } catch (IOException e) {
            throw new RuntimeException("Connection or read timeout. Connect to the internet");
        }
    }

    public static long getFileSize(HttpURLConnection connection) {
        var fileSize = connection.getContentLengthLong();
        if (fileSize == -1)
            throw new RuntimeException("Connection failed");
        return fileSize;
    }

    public static void checkFieldsAfterSizePreparation(long fileSize, Label sizeLabel, TextField chunksField,
                                                       TextField bytesField, HttpURLConnection connection) {
        if (!canResume(connection)) {
            chunksField.setText("0");
            chunksField.setDisable(true);
        } else
            chunksField.setDisable(false);
        Platform.runLater(() -> {
            sizeLabel.setText(IOUtils.formatBytes(fileSize));
            bytesField.setText(fileSize + "");
        });
    }

    public static boolean canResume(HttpURLConnection connection) {
        var rangeSupport = connection.getHeaderField("Accept-Ranges");
        return rangeSupport != null && !rangeSupport.equals("none");
    }

    public static CompletableFuture<Long> prepareFileSizeAndFieldsAsync(HttpURLConnection connection, TextField urlField,
                                                                        Label sizeLabel, TextField chunksField,
                                                                        TextField bytesField, DownloadModel dm,
                                                                        Executor executor) {
        final HttpURLConnection[] finalConnection = {connection};
        return CompletableFuture.supplyAsync(() -> {
            if (finalConnection[0] == null)
                finalConnection[0] = connect(urlField.getText(), 3000, 3000);
            var fileSize = getFileSize(finalConnection[0]);
            checkFieldsAfterSizePreparation(fileSize, sizeLabel, chunksField, bytesField, finalConnection[0]);
            dm.setResumeable(canResume(finalConnection[0]));
            dm.setSize(fileSize);
            return fileSize;
        }, executor);
    }


    public static String extractFileName(String link, HttpURLConnection connection) {
        var raw = connection.getHeaderField("Content-Disposition");
        if (raw != null && raw.contains("="))
            return raw.split("=")[1].replaceAll("\"", "");

        var hasParameter = link.lastIndexOf('?') != -1;
        var extractedFileName = "";
        if (hasParameter)
            extractedFileName = link.substring(link.lastIndexOf('/') + 1, link.lastIndexOf('?'));
        else
            extractedFileName = link.substring(link.lastIndexOf('/') + 1);

        if (!extractedFileName.isBlank())
            return extractedFileName;

        return UUID.randomUUID().toString();
    }

    public static CompletableFuture<String> prepareFileNameAndFieldsAsync(HttpURLConnection connection, String link,
                                                                          TextField nameField, Executor executor) {
        final HttpURLConnection[] finalConnection = {connection};
        return CompletableFuture.supplyAsync(() -> {
            if (finalConnection[0] == null)
                finalConnection[0] = connect(link, 3000, 3000);
            var fileName = extractFileName(link, finalConnection[0]);
            if (nameField != null)
                Platform.runLater(() -> nameField.setText(fileName));
            return fileName;
        }, executor);
    }


    public static void determineLocationAndQueue(TextField locationField, String fileName, DownloadModel dm) {
        Platform.runLater(() -> {
            if (fileName.isBlank())
                return;
            var compressedMatch = FileExtensions.compressedEx.stream().anyMatch(fileName::endsWith);
            if (compressedMatch) {
                locationField.setText(AppConfigs.compressedPath);
                dm.setQueues(new ArrayList<>(List.of(QueuesRepo.findByName(COMPRESSED_QUEUE, false))));
                return;
            }
            var videoMatch = FileExtensions.videoEx.stream().anyMatch(fileName::endsWith);
            if (videoMatch) {
                locationField.setText(AppConfigs.videosPath);
                dm.setQueues(new ArrayList<>(List.of(QueuesRepo.findByName(VIDEOS_QUEUE, false))));
                return;
            }
            var programMatch = FileExtensions.programEx.stream().anyMatch(fileName::endsWith);
            if (programMatch) {
                locationField.setText(AppConfigs.programsPath);
                dm.setQueues(new ArrayList<>(List.of(QueuesRepo.findByName(PROGRAMS_QUEUE, false))));
                return;
            }
            var musicMatch = FileExtensions.musicEx.stream().anyMatch(fileName::endsWith);
            if (musicMatch) {
                locationField.setText(AppConfigs.musicPath);
                dm.setQueues(new ArrayList<>(List.of(QueuesRepo.findByName(MUSIC_QUEUE, false))));
                return;
            }
            var documentMatch = FileExtensions.documentEx.stream().anyMatch(fileName::endsWith);
            if (documentMatch) {
                locationField.setText(AppConfigs.documentPath);
                dm.setQueues(new ArrayList<>(List.of(QueuesRepo.findByName(DOCS_QUEUE, false))));
                return;
            }
            locationField.setText(AppConfigs.othersPath);
            dm.setQueues(new ArrayList<>(List.of(QueuesRepo.findByName(OTHERS_QUEUE, false))));
        });
    }

    public static void initPopOvers(Button[] questionButtons, String[] contents) {
        for (int i = 0; i < questionButtons.length; i++) {
            var pop = new PopOver();
            pop.setAnimated(true);
            pop.setContentNode(new Label(contents[i]));
            var finalI = i;
            questionButtons[i].hoverProperty().addListener((o, old, newVal) -> {
                if (newVal)
                    pop.show(questionButtons[finalI]);
                else
                    pop.hide();
            });
        }
    }


    /**
     * @param blocking of course, it should be done in concurrent environment otherwise it will block the main thread.
     *                 mostly using for queue downloading
     */
    public static void startDownload(DownloadModel dm, MainTableUtils mainTableUtils,
                                     String speed, String bytes, boolean resume, boolean blocking,
                                     ExecutorService executor) {
        if (!dm.isResumeable())
            return;
        DownloadTask downloadTask = new DownloadLimitedTask(dm, Long.MAX_VALUE, false, mainTableUtils);
        if (dm.getChunks() == 0) {
            if (speed != null) {
                if (speed.equals("0")) {
                    if (bytes != null) {
                        if (bytes.equals(dm.getSize() + ""))
                            downloadTask = new DownloadLimitedTask(dm, Long.MAX_VALUE, false, mainTableUtils);
                        else
                            downloadTask = new DownloadLimitedTask(dm, Long.parseLong(bytes), false, mainTableUtils);
                    }
                } else
                    downloadTask = new DownloadLimitedTask(dm, getBytesFromString(speed), true, mainTableUtils);
            }
        } else {
            if (speed != null) {
                if (speed.equals("0"))
                    downloadTask = new DownloadInChunksTask(dm, mainTableUtils, null);
                else
                    downloadTask = new DownloadInChunksTask(dm, mainTableUtils, getBytesFromString(speed));
            } else
                downloadTask = new DownloadInChunksTask(dm, mainTableUtils, null);
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
        dm.setShowCompleteDialog(showCompleteDialog);
        dm.setOpenAfterComplete(false);
        if (!resume) {
            DownloadsRepo.insertDownload(dm);
            mainTableUtils.addRow(dm);
        }
        currentDownloadings.add(dm);
        if (executor == null)
            executor = Executors.newCachedThreadPool();
        downloadTask.setExecutor(executor);

        if (blocking)
            downloadTask.run();
        else
            executor.submit(downloadTask);

    }


    public static void selectLocation(ActionEvent e, TextField locationField) {
        var dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select download save location");
        dirChooser.setInitialDirectory(new File(AppConfigs.downloadPath));
        var selectedDir = dirChooser.showDialog(FxUtils.getStageFromEvent(e));
        if (selectedDir != null) {
            var path = selectedDir.getPath();
            locationField.setText(path);
            return;
        }
        Notifications.create()
                .title("No Directory")
                .text("Location is wrong!")
                .showError();
    }
}

