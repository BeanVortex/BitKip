package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.NewDownload;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import org.controlsfx.control.PopOver;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NewDownloadUtils {

    public static void validInputChecks(TextField chunksField, TextField bytesField, TextField speedField) {
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
            if (bytesField != null)
                bytesField.setDisable(!chunksField.getText().equals("0"));
        });
        chunksField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && chunksField.getText().isBlank())
                chunksField.setText("0");
        });
        speedField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                chunksField.setText(newValue.replaceAll("\\D", ""));
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

    public static CompletableFuture<Void> prepareSize(TextField urlField, Label sizeLabel, DownloadModel downloadModel) {
        return CompletableFuture.runAsync(() -> {
            var link = urlField.getText();
            try {
                if (!link.isBlank()) {
                    var url = new URL(urlField.getText());
                    var conn = (HttpURLConnection) url.openConnection();
                    var fileSize = conn.getContentLengthLong();
                    downloadModel.setSize(fileSize);
                    Platform.runLater(() -> sizeLabel.setText(IOUtils.formatFileSize(fileSize)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
}
