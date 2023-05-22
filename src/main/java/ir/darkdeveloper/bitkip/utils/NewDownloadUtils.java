package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import javafx.stage.DirectoryChooser;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.Defaults.AGENT;
import static ir.darkdeveloper.bitkip.utils.Defaults.extensions;

public class NewDownloadUtils {


    public static HttpURLConnection connect(String uri, int connectTimeout, int readTimeout) {
        try {
            if (uri.isBlank())
                throw new IllegalArgumentException("URL is blank");
            var url = new URL(uri);
            var conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setRequestProperty("User-Agent", AGENT);
            return conn;
        } catch (IOException e) {
            var msg = "Connection or read timeout. Connect to the internet or check the url";
            log.error(msg);
            Notifications.create()
                    .title("Bad Connection")
                    .text(msg)
                    .showError();
            throw new RuntimeException(msg);
        }
    }

    public static long getFileSize(HttpURLConnection connection) {
        var fileSize = connection.getContentLengthLong();
        if (fileSize == -1)
            throw new RuntimeException("Connection failed");
        return fileSize;
    }

    public static boolean canResume(HttpURLConnection connection) {
        var rangeSupport = connection.getHeaderField("Accept-Ranges");
        return rangeSupport != null && !rangeSupport.equals("none");
    }

    public static CompletableFuture<Long> prepareFileSizeAndFieldsAsync(HttpURLConnection connection, TextField urlField,
                                                                        Label sizeLabel, Label resumableLabel, TextField chunksField,
                                                                        TextField bytesField, DownloadModel dm,
                                                                        Executor executor) {
        final HttpURLConnection[] finalConnection = {connection};
        return CompletableFuture.supplyAsync(() -> {
            if (finalConnection[0] == null)
                finalConnection[0] = connect(urlField.getText(), 3000, 3000);
            var fileSize = getFileSize(finalConnection[0]);
            var resumable = canResume(finalConnection[0]);

            if (!resumable) {
                chunksField.setText("0");
                chunksField.setDisable(true);
                bytesField.setDisable(true);
            } else {
                chunksField.setText("" + InputValidations.maxChunks());
                chunksField.setDisable(false);
            }
            Platform.runLater(() -> {
                resumableLabel.setTextFill(resumable ? Paint.valueOf("#388E3C") : Paint.valueOf("#EF5350"));
                resumableLabel.setText(resumable ? "Yes" : "No");
                sizeLabel.setText(IOUtils.formatBytes(fileSize));
                bytesField.setText(fileSize + "");
            });
            dm.setResumable(resumable);
            dm.setSize(fileSize);
            return fileSize;
        }, executor);
    }


    public static String extractFileName(String link, HttpURLConnection connection) {
        var raw = connection.getHeaderField("Content-Disposition");
        if (raw != null && raw.contains("="))
            return raw.split("=")[1].replaceAll("\"", "");
        var extractedFileName = link.substring(link.lastIndexOf('/') + 1);
        var lastIndexParameter = extractedFileName.lastIndexOf('?');
        var hasParameter = lastIndexParameter != -1;
        if (hasParameter)
            extractedFileName = extractedFileName.substring(0, lastIndexParameter);

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
            if (fileName == null || fileName.isBlank())
                return;
            for (var entry : extensions.entrySet()) {
                var matched = entry.getValue().stream().anyMatch(fileName::endsWith);
                if (matched) {
                    var path = defaultDownloadPaths.stream().filter(p -> p.contains(entry.getKey()))
                            .findFirst().orElse(othersPath);
                    if (!locationField.getText().equals(path))
                        locationField.setText(path);
                    determineQueue(dm, entry.getKey());
                    return;
                }
                // empty is set for others
                if (entry.getValue().isEmpty()) {
                    if (!locationField.getText().equals(othersPath))
                        locationField.setText(othersPath);
                    determineQueue(dm, entry.getKey());
                    return;
                }
            }
        });
    }

    private static void determineQueue(DownloadModel dm, String queueName) {
        if (dm != null)
            dm.setQueues(new CopyOnWriteArrayList<>(List.of(QueuesRepo.findByName(queueName, false))));
    }

    public static void initPopOvers(Button[] questionButtons, String[] contents) {
        var pop = new PopOver();
        pop.setAnimated(true);
        pop.setAutoHide(true);
        for (int i = 0; i < questionButtons.length; i++) {
            var finalI = i;
            questionButtons[i].setOnAction(e -> {
                pop.setContentNode(new Label(contents[finalI]));
                if (pop.isShowing()) pop.hide();
                else pop.show(questionButtons[finalI]);
            });
            questionButtons[i].setOnMouseExited(e -> pop.hide());
        }
    }



    public static void selectLocation(ActionEvent e, TextField locationField) {
        var dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select download save location");
        dirChooser.setInitialDirectory(new File(AppConfigs.downloadPath));
        var selectedDir = dirChooser.showDialog(FxUtils.getStageFromEvent(e));
        if (selectedDir != null) {
            var path = selectedDir.getPath();
            if (!path.endsWith(File.separator))
                path += File.separator;
            locationField.setText(path);
            return;
        }
        Notifications.create()
                .title("No Directory")
                .text("Location is wrong!")
                .showError();
    }

    public static void checkIfFileExists(String location, String name,
                                         Label errorLabel, Button downloadBtn, Button addBtn) {
        var file = new File(location + name);
        var chunkFile = new File(location + name + "#0");
        if (file.exists() || chunkFile.exists()) {
            errorLabel.setVisible(true);
            if (downloadBtn != null)
                downloadBtn.setDisable(true);
            addBtn.setDisable(true);
            Platform.runLater(() -> errorLabel.setText("At least one file with this name exists in this location"));
        } else {
            errorLabel.setVisible(false);
            if (downloadBtn != null)
                downloadBtn.setDisable(false);
            addBtn.setDisable(false);
        }
    }

    public static void onOfflineFieldsChanged(TextField locationField, String filename, DownloadModel dm,
                                              ComboBox<QueueModel> queueCombo, Label errorLabel,
                                              Button downloadBtn, Button addBtn, Button openLocation) {
        // when saving outside BitKip folder
        var selectedQueue = queueCombo.getSelectionModel().getSelectedItem();
        if (!locationField.getText().contains("BitKip")
                && (selectedQueue == null || !selectedQueue.hasFolder())) {
            openLocation.setDisable(false);
            return;
        }

        if (selectedQueue != null && selectedQueue.hasFolder()) {
            var folder = new File(queuesPath + selectedQueue.getName());
            if (!folder.exists())
                folder.mkdir();
            var path = folder.getAbsolutePath();
            if (!path.endsWith(File.separator))
                path += File.separator;
            if (!locationField.getText().equals(path))
                locationField.setText(path);
            openLocation.setDisable(true);
        } else {
            determineLocationAndQueue(locationField, filename, dm);
            openLocation.setDisable(false);
        }
        checkIfFileExists(locationField.getText(), filename, errorLabel, downloadBtn, addBtn);
    }

    public static void disableControlsAndShowError(String error, Label errorLbl,
                                                   Button btn1, Button btn2, Button refreshBtn) {
        errorLbl.setVisible(true);
        btn1.setDisable(true);
        if (refreshBtn != null){
            refreshBtn.setDisable(false);
            refreshBtn.setVisible(true);
        }
        if (btn2 != null)
            btn2.setDisable(true);
        errorLbl.setText(error);
        log.error(error);
    }

    public static String determineLocation(String fileName) {
        if (fileName == null || fileName.isBlank())
            return null;
        for (var entry : extensions.entrySet()) {
            var matched = entry.getValue().stream().anyMatch(fileName::endsWith);
            if (matched)
                return defaultDownloadPaths.stream().filter(p -> p.contains(entry.getKey()))
                        .findFirst().orElse(othersPath);
        }
        return null;
    }
}

