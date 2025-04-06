package io.beanvortex.bitkip.utils;

import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.exceptions.DeniedException;
import io.beanvortex.bitkip.models.Credentials;
import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.models.QueueModel;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import io.beanvortex.bitkip.repo.QueuesRepo;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static io.beanvortex.bitkip.config.AppConfigs.*;
import static io.beanvortex.bitkip.utils.Defaults.OTHERS_QUEUE;
import static io.beanvortex.bitkip.utils.Defaults.extensions;
import static io.beanvortex.bitkip.utils.Validations.maxChunks;

public class DownloadUtils {


    public static HttpURLConnection connect(String uri, Credentials credentials) throws IOException {
        if (uri.isBlank())
            throw new IllegalArgumentException("URL is blank");
        uri = Validations.fixURIChars(uri);
        var url = URI.create(uri).toURL();
        var conn = (HttpURLConnection) url.openConnection();
        if (credentials != null && credentials.isOk())
            conn.setRequestProperty("Authorization", "Basic " + credentials.base64Encoded());
        conn.setConnectTimeout(connectionTimeout);
        conn.setReadTimeout(readTimeout);
        if (userAgentEnabled)
            conn.setRequestProperty("User-Agent", userAgent);
        return conn;
    }


    public static HttpURLConnection connectWithInternetCheck(String uri, Credentials credentials, boolean showErrors) throws IOException {
        try {
            if (uri.isBlank())
                throw new IllegalArgumentException("URL is blank");
            uri = Validations.fixURIChars(uri);
            var url = URI.create(uri).toURL();
            var testCon = (HttpURLConnection) url.openConnection();
            testCon.setConnectTimeout(2000);
            testCon.connect();
            return connect(uri, credentials);
        } catch (IOException e) {
            var msg = "Connection or read timeout. Connect to the internet or check the url: " + e.getMessage();
            log.error(e.toString());
            if (showErrors)
                Platform.runLater(() -> Notifications.create()
                        .title("Bad Connection")
                        .text(msg)
                        .showError());
            throw new IOException(msg);
        }

    }

    public static long getFileSize(HttpURLConnection connection) {
        var fileSize = connection.getContentLengthLong();
        if (fileSize == -1)
            log.warn("Can't fetch file size");
        return fileSize;
    }

    /**
     * @return new file name like file(1), file(2). if not existed in db, returns fileName
     */
    public static String getNewFileNameIfExists(String fileName, String path) {
        var pathToFind = path + fileName;
        var nextNum = DownloadsRepo.getNextNumberOfExistedDownload(pathToFind);
        var containsDot = false;
        if ((nextNum == 0 || nextNum == 1) && fileName.contains(".")) {
            containsDot = true;
            pathToFind = pathToFind.substring(0, pathToFind.lastIndexOf('.'));
        }

        nextNum = DownloadsRepo.getNextNumberOfExistedDownload(pathToFind);
        if (nextNum == 0)
            return fileName;

        var newFileName = new StringBuilder();
        if (containsDot)
            newFileName.append(fileName, 0, fileName.lastIndexOf('.'));
        else
            newFileName.append(fileName);

        newFileName.append("(")
                .append(nextNum)
                .append(")");

        if (containsDot)
            newFileName.append(fileName.substring(fileName.lastIndexOf('.')));

        return String.valueOf(newFileName);
    }

    public static boolean canResume(HttpURLConnection connection) {
        var rangeSupport = connection.getHeaderField("Accept-Ranges");
        return rangeSupport != null && !rangeSupport.equals("none");
    }

    public static CompletableFuture<Long> prepareFileSizeAndFieldsAsync(HttpURLConnection connection, TextField urlField,
                                                                        Label sizeLabel, Label resumableLabel,
                                                                        TextField speedField, TextField chunksField,
                                                                        TextField bytesField, DownloadModel dm,
                                                                        Executor executor) {
        final HttpURLConnection[] finalConnection = {connection};
        return CompletableFuture.supplyAsync(() -> {
            if (finalConnection[0] == null) {
                try {
                    finalConnection[0] = connect(urlField.getText(), dm.getCredentials());
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            var fileSize = getFileSize(finalConnection[0]);
            var resumable = canResume(finalConnection[0]);
            Platform.runLater(() -> {
                if (resumable) {
                    chunksField.setText(String.valueOf(maxChunks(fileSize)));
                    bytesField.setDisable(false);
                    resumableLabel.setText("Resumable");
                    resumableLabel.getStyleClass().add("yes");
                    resumableLabel.getStyleClass().remove("no");
                } else {
                    resumableLabel.setText("Not Resumable");
                    resumableLabel.getStyleClass().add("no");
                    resumableLabel.getStyleClass().remove("yes");
                    chunksField.setText("0");
                    bytesField.setDisable(true);
                    speedField.setDisable(true);
                }
                sizeLabel.setText(IOUtils.formatBytes(fileSize));
                bytesField.setText(String.valueOf(fileSize));
            });
            dm.setResumable(resumable);
            dm.setSize(fileSize);
            return fileSize;
        }, executor);
    }


    public static String extractFileName(String link, HttpURLConnection connection) {
        var raw = connection.getHeaderField("Content-Disposition");
        if (raw != null && raw.contains("=")) {
            try {
                return raw.split("=")[1].replaceAll("\"", "");
            } catch (IndexOutOfBoundsException ignore) {
            }
        }

        var extractedFileName = link.substring(link.lastIndexOf('/') + 1);
        var lastIndexParameter = extractedFileName.lastIndexOf('?');
        var hasParameter = lastIndexParameter != -1;
        if (hasParameter)
            extractedFileName = extractedFileName.substring(0, lastIndexParameter);
        if (extractedFileName.contains("%"))
            extractedFileName = URLDecoder.decode(extractedFileName, StandardCharsets.UTF_8);

        if (!extractedFileName.isBlank())
            return extractedFileName;
        return UUID.randomUUID().toString();
    }

    public static CompletableFuture<String> prepareFileNameAndFieldsAsync(HttpURLConnection connection, String link,
                                                                          TextField nameField, DownloadModel dm, Executor executor) {
        final HttpURLConnection[] finalConnection = {connection};
        return CompletableFuture.supplyAsync(() -> {
            if (finalConnection[0] == null) {
                try {
                    finalConnection[0] = connect(link, dm.getCredentials());
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            var fileName = extractFileName(link, finalConnection[0]);
            if (nameField != null)
                Platform.runLater(() -> nameField.setText(fileName));
            dm.setName(fileName);
            return fileName;
        }, executor);
    }


    public static void setLocationAndQueue(TextField locationField, String fileName, DownloadModel dm) {
        Platform.runLater(() -> {
            if (fileName == null || fileName.isBlank())
                return;
            var qm = determineQueue(fileName);
            if (qm.getName().equals(OTHERS_QUEUE)) {
                if (!locationField.getText().equals(othersPath))
                    locationField.setText(othersPath);
            } else {
                var path = defaultDownloadPaths.stream().filter(p -> p.contains(qm.getName()))
                        .findFirst().orElse(othersPath);
                if (!locationField.getText().equals(path))
                    locationField.setText(path);
            }
            if (dm != null && !dm.getQueues().contains(qm))
                dm.getQueues().add(qm);
        });
    }

    public static QueueModel determineQueue(String fileName) {
        if (fileName == null || fileName.isBlank())
            return null;
        for (var entry : extensions.entrySet()) {
            var matched = entry.getValue().stream().anyMatch(fileName::endsWith);
            if (matched)
                return QueuesRepo.findByName(entry.getKey(), false);
        }
        return QueuesRepo.findByName(OTHERS_QUEUE, false);
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


    public static String selectLocation(Stage stage) {
        var dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select file's save location");
        dirChooser.setInitialDirectory(new File(lastSavedDir == null ? System.getProperty("user.home") : lastSavedDir));
        var selectedDir = dirChooser.showDialog(stage);
        if (selectedDir != null) {
            var path = selectedDir.getPath();
            if (!path.endsWith(File.separator))
                path += File.separator;
            if (lastSavedDir == null || !lastSavedDir.equals(path)) {
                lastSavedDir = path;
                IOUtils.saveConfigs();
            }
            return path;
        }
        Notifications.create()
                .title("No Directory")
                .text("Location is wrong!")
                .showError();
        return null;
    }

    public static void checkIfFileIsOKToSave(String location, String name, Button downloadBtn,
                                             Button addBtn, CheckBox lastLocationCheck) throws DeniedException {
        var file = new File(location + name);
        var chunkFile = new File(location + name + "#0");
        if (file.exists() || chunkFile.exists()) {
            if (downloadBtn != null)
                downloadBtn.setDisable(!addSameDownload);
            addBtn.setDisable(!addSameDownload);
            if (!addSameDownload)
                throw new DeniedException("At least one file with this name exists in this location");
        } else {
            if (downloadBtn != null)
                downloadBtn.setDisable(false);
            lastLocationCheck.setDisable(false);
            if (AppConfigs.lastSavedDir == null)
                lastLocationCheck.setDisable(true);
            addBtn.setDisable(false);
        }
    }

    public static void handleError(ERunnable r, Label errorLabel) {
        Runnable run = () -> {
            try {
                r.run();
                errorLabel.setVisible(false);
            } catch (DeniedException e) {
                errorLabel.setVisible(true);
                errorLabel.setText(e.getMessage());
            }
        };
        if (Platform.isFxApplicationThread())
            run.run();
        else Platform.runLater(run);
    }


    public interface ERunnable {
        void run() throws DeniedException;
    }

    public static void onOfflineFieldsChanged(TextField locationField, String filename, DownloadModel dm,
                                              ComboBox<QueueModel> queueCombo, Button downloadBtn, Button addBtn,
                                              Button openLocation, CheckBox lastLocationCheck) throws DeniedException {
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
            setLocationAndQueue(locationField, filename, dm);
            openLocation.setDisable(false);
        }
        checkIfFileIsOKToSave(locationField.getText(), filename, downloadBtn, addBtn, lastLocationCheck);
    }

    public static void disableControlsAndShowError(String error, Label errorLbl, Button btn1, Button btn2) {
        errorLbl.setVisible(true);
        btn1.setDisable(true);
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
        return othersPath;
    }
}

