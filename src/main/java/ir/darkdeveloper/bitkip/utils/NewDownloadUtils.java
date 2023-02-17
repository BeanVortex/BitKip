package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.service.DownloadProgressService;
import ir.darkdeveloper.bitkip.task.DownloadInChunksTask;
import ir.darkdeveloper.bitkip.task.DownloadLimitedTask;
import ir.darkdeveloper.bitkip.task.DownloadTask;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class NewDownloadUtils {

    public static void validInputChecks(TextField chunksField, TextField bytesField,
                                        TextField speedField) {
        chunksField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                chunksField.setText(newValue.replaceAll("\\D", ""));
            if (!chunksField.getText().isBlank()) {
                var chunks = Integer.parseInt(chunksField.getText());
                var cores = Runtime.getRuntime().availableProcessors();
                if (chunks > cores * 2)
                    chunks = cores * 2;
                chunksField.setText(chunks + "");
            }
            bytesField.setDisable(!chunksField.getText().equals("0"));
        });
        chunksField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && chunksField.getText().isBlank())
                chunksField.setText("0");
        });
        speedField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d+\\.?\\d*"))
                speedField.setText(newValue.replaceAll("[a-zA-Z`!@#$%^&*()?<>,;:'\"\\-_+=]*", ""));
            bytesField.setDisable(!newValue.equals("0"));
        });

        speedField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && speedField.getText().isBlank())
                speedField.setText("0");
        });
    }

    public static void prepareLinkFromClipboard(TextField urlField) {
        var clip = Clipboard.getSystemClipboard();
        var clipContent = clip.getString();
        if (clipContent.startsWith("http") || clipContent.startsWith("https"))
            urlField.setText(clipContent);
    }

    public static CompletableFuture<Long> prepareSize(TextField urlField, Label sizeLabel,
                                                      TextField bytesField, DownloadModel downloadModel) {
        return CompletableFuture.supplyAsync(() -> {
            var link = urlField.getText();
            try {
                if (!link.isBlank()) {
                    var url = new URL(urlField.getText());
                    var conn = (HttpURLConnection) url.openConnection();
                    var fileSize = conn.getContentLengthLong();
                    downloadModel.setSize(fileSize);
                    Platform.runLater(() -> {
                        sizeLabel.setText(IOUtils.formatBytes(fileSize));
                        bytesField.setText(fileSize + "");
                    });
                    return fileSize;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }


    public static CompletableFuture<String> prepareFileName(TextField urlField, TextField nameField) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var link = urlField.getText();
                if (!link.isBlank()) {
                    var url = new URL(urlField.getText());
                    var conn = (HttpURLConnection) url.openConnection();
                    var raw = conn.getHeaderField("Content-Disposition");
                    if (raw != null && raw.contains("=")) {
                        var fileName = raw.split("=")[1].replaceAll("\"", "");
                        Platform.runLater(() -> nameField.setText(fileName));
                        return fileName;
                    }
                    var hasParameter = link.lastIndexOf('?') != -1;
                    var extractedFileName = "";
                    if (hasParameter)
                        extractedFileName = link.substring(link.lastIndexOf('/') + 1, link.lastIndexOf('?'));
                    else
                        extractedFileName = link.substring(link.lastIndexOf('/') + 1);
                    if (!extractedFileName.isBlank()) {
                        var finalExtractedFileName = extractedFileName;
                        Platform.runLater(() -> nameField.setText(finalExtractedFileName));
                        return extractedFileName;
                    }
                    var randomName = UUID.randomUUID().toString();
                    Platform.runLater(() -> nameField.setText(randomName));
                    return randomName;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        });
    }

    public static void determineLocation(TextField locationField, String fileName, DownloadModel downloadModel) {
        Platform.runLater(() -> {
            if (fileName.isBlank())
                return;
            var compressedMatch = FileExtensions.compressedEx.stream().anyMatch(fileName::endsWith);
            if (compressedMatch) {
                locationField.setText(AppConfigs.compressedPath);
                downloadModel.setQueue(new ArrayList<>(List.of(QueuesRepo.findByName("Compressed"))));
                return;
            }
            var videoMatch = FileExtensions.videoEx.stream().anyMatch(fileName::endsWith);
            if (videoMatch) {
                locationField.setText(AppConfigs.videosPath);
                downloadModel.setQueue(new ArrayList<>(List.of(QueuesRepo.findByName("Videos"))));
                return;
            }
            var programMatch = FileExtensions.programEx.stream().anyMatch(fileName::endsWith);
            if (programMatch) {
                locationField.setText(AppConfigs.programsPath);
                downloadModel.setQueue(new ArrayList<>(List.of(QueuesRepo.findByName("Programs"))));
                return;
            }
            var musicMatch = FileExtensions.musicEx.stream().anyMatch(fileName::endsWith);
            if (musicMatch) {
                locationField.setText(AppConfigs.musicPath);
                downloadModel.setQueue(new ArrayList<>(List.of(QueuesRepo.findByName("Music"))));
                return;
            }
            var documentMatch = FileExtensions.documentEx.stream().anyMatch(fileName::endsWith);
            if (documentMatch) {
                locationField.setText(AppConfigs.documentPath);
                downloadModel.setQueue(new ArrayList<>(List.of(QueuesRepo.findByName("Docs"))));
                return;
            }
            locationField.setText(AppConfigs.othersPath);
            downloadModel.setQueue(new ArrayList<>(List.of(QueuesRepo.findByName("Others"))));
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


    public static void startDownload(DownloadModel downloadModel, TableUtils tableUtils, String speed, String bytes, boolean resume) {
        DownloadTask downloadTask = new DownloadLimitedTask(downloadModel, Long.MAX_VALUE, false);
        var speedFactor = new AtomicInteger(1);
        if (downloadModel.getChunks() == 0) {
            if (speed != null) {
                if (speed.equals("0")) {
                    if (bytes != null) {
                        if (bytes.equals(downloadModel.getSize() + ""))
                            downloadTask = new DownloadLimitedTask(downloadModel, Long.MAX_VALUE, false);
                        else
                            downloadTask = new DownloadLimitedTask(downloadModel, Long.parseLong(bytes), false);
                    }
                } else
                    downloadTask = new DownloadLimitedTask(downloadModel, getBytesFromField(speed), true);
            }
        } else {
            downloadTask = new DownloadInChunksTask(downloadModel);
            speedFactor.set(2);
        }

        downloadTask.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue == null)
                oldValue = 0L;
            var currentSpeed = (newValue - oldValue) * speedFactor.get();
            if (newValue == 0)
                currentSpeed = 0;
            tableUtils.updateDownloadSpeedAndRemaining(currentSpeed, downloadModel, newValue);
        });
        downloadTask.progressProperty().addListener((o, old, newV) ->
                tableUtils.updateDownloadProgress(newV.floatValue() * 100, downloadModel));
        downloadModel.setDownloadTask(downloadTask);
        if (!resume) {
            DownloadsRepo.insertDownload(downloadModel);
            tableUtils.addRow(downloadModel);
        }
        AppConfigs.downloadTaskList.add(downloadTask);
        AppConfigs.currentDownloading.add(downloadModel);
        var progressService = new DownloadProgressService(downloadModel);
        progressService.setPeriod(Duration.seconds(5));
        downloadTask.setOnSucceeded(event -> progressService.cancel());
        downloadTask.setOnCancelled(event -> progressService.cancel());
        var t = new Thread(downloadTask);
        t.setDaemon(true);
        t.start();
        progressService.start();
    }

    private static long getBytesFromField(String mb) {
        var mbVal = Double.parseDouble(mb);
        return (long) (mbVal * Math.pow(2, 20));
    }


}

