package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.DownloadOpUtils;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.InputValidations;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.ALL_DOWNLOADS_QUEUE;

public class SingleDownload implements QueueObserver {

    @FXML
    private Label errorLabel;
    @FXML
    private Button newQueue;
    @FXML
    private Button addBtn;
    @FXML
    private Button downloadBtn;
    @FXML
    private Label sizeLabel;
    @FXML
    private TextField bytesField;
    @FXML
    private Button questionBtnBytes;
    @FXML
    private TextField chunksField;
    @FXML
    private Button questionBtnChunks;
    @FXML
    private TextField speedField;
    @FXML
    private Button questionBtnSpeed;
    @FXML
    private ComboBox<QueueModel> queueCombo;
    @FXML
    private TextField urlField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField locationField;
    @FXML
    private Button openLocation;
    private Stage stage;

    private final DownloadModel dm = new DownloadModel();


    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void initAfterStage() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var queues = AppConfigs.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, false);
        queues = queues.stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
        openLocation.setGraphic(new FontIcon());
        questionBtnSpeed.setGraphic(new FontIcon());
        questionBtnChunks.setGraphic(new FontIcon());
        questionBtnBytes.setGraphic(new FontIcon());
        newQueue.setGraphic(new FontIcon());
        downloadBtn.setDisable(true);
        addBtn.setDisable(true);
        var questionBtns = new Button[]{questionBtnSpeed, questionBtnBytes, questionBtnChunks};
        var contents = new String[]{
                "You can limit downloading speed. calculated in MB. (0.8 means 800KB)",
                "You can specify how many bytes of the file to download (Disabled in chunks downloading mode)",
                "File is seperated into parts and will be downloaded concurrently"
        };
        NewDownloadUtils.initPopOvers(questionBtns, contents);
        InputValidations.validInputChecks(chunksField, bytesField, speedField, dm);
        InputValidations.prepareLinkFromClipboard(urlField);
        queueCombo.getSelectionModel().selectedIndexProperty().addListener(observable -> onQueueChanged());
        urlField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                NewDownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, downloadBtn, addBtn);
            else autoFillLocationAndSizeAndName();
        });
        nameField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                NewDownloadUtils.disableControlsAndShowError("Name is blank", errorLabel, downloadBtn, addBtn);
            else onOfflineFieldsChanged();
        });
        locationField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                NewDownloadUtils.disableControlsAndShowError("Location is blank", errorLabel, downloadBtn, addBtn);
            else onOfflineFieldsChanged();
        });
        autoFillLocationAndSizeAndName();

    }

    private void autoFillLocationAndSizeAndName() {
        try {
            // firing select event
            queueCombo.getSelectionModel().select(queueCombo.getSelectionModel().getSelectedIndex());
            var url = urlField.getText();
            var connection = NewDownloadUtils.connect(url, 3000, 3000);
            var executor = Executors.newFixedThreadPool(2);
            var fileNameLocationFuture = NewDownloadUtils.prepareFileNameAndFieldsAsync(connection, url, nameField, executor)
                    .thenAccept(this::setLocation);
            var sizeFuture = NewDownloadUtils.prepareFileSizeAndFieldsAsync(connection, urlField, sizeLabel, chunksField, bytesField, dm, executor);
            CompletableFuture.allOf(fileNameLocationFuture, sizeFuture)
                    .whenComplete((unused, throwable) -> {
                        NewDownloadUtils.checkIfFileExists(locationField.getText(), nameField.getText(), errorLabel, downloadBtn, addBtn);
                        executor.shutdown();
                    })
                    .exceptionally(throwable -> {
                        if (!executor.isShutdown())
                            executor.shutdown();
                        var errMsg = throwable.getCause().getLocalizedMessage();
                        Platform.runLater(() ->
                                NewDownloadUtils.disableControlsAndShowError(errMsg, errorLabel, downloadBtn, addBtn));
                        return null;
                    });
        } catch (Exception e) {
            NewDownloadUtils.disableControlsAndShowError(e.getLocalizedMessage(), errorLabel, downloadBtn, addBtn);
        }
    }

    private void setLocation(String fileName) {
        NewDownloadUtils.determineLocationAndQueue(locationField, fileName, dm);
    }

    @FXML
    private void onSelectLocation(ActionEvent e) {
        NewDownloadUtils.selectLocation(e, locationField);
        NewDownloadUtils.checkIfFileExists(locationField.getText(), nameField.getText(), errorLabel, downloadBtn, addBtn);
    }

    @FXML
    private void onNewQueue() {
        FxUtils.newQueueStage();
    }

    @FXML
    private void onCancel() {
        stage.close();
        getQueueSubject().removeObserver(this);
    }

    @FXML
    private void onAdd() {
        var prepared = prepareDownload();
        if (prepared) {
            dm.setDownloadStatus(DownloadStatus.Paused);
            DownloadsRepo.insertDownload(dm);
            mainTableUtils.addRow(dm);
            getQueueSubject().removeObserver(this);
            stage.close();
        }
    }

    @FXML
    private void onDownload() {
        var prepared = prepareDownload();
        if (prepared) {
            DownloadOpUtils.startDownload(dm, speedField.getText(), bytesField.getText(),
                    false, false, null);
            DownloadOpUtils.openDownloadingStage(dm);
            getQueueSubject().removeObserver(this);
            stage.close();
        }
    }

    private boolean prepareDownload() {

        var url = urlField.getText();
        var fileName = nameField.getText();
        var path = locationField.getText();
        if (url.isBlank()) {
            log.warn("URL is blank");
            NewDownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, downloadBtn, addBtn);
            return false;
        }
        if (fileName.isBlank()) {
            log.warn("Name is blank");
            NewDownloadUtils.disableControlsAndShowError("Name is blank", errorLabel, downloadBtn, addBtn);
            return false;
        }
        if (path.isBlank()) {
            log.warn("Location is blank");
            NewDownloadUtils.disableControlsAndShowError("Location is blank", errorLabel, downloadBtn, addBtn);
            return false;
        }

        var byURL = DownloadsRepo.findByURL(url);
        if (!byURL.isEmpty()) {
            var found = byURL.stream()
                    .filter(dm -> {
                        var s = Paths.get(dm.getFilePath()).getParent().toString() + File.separator;
                        return s.equals(path);
                    })
                    .findFirst();
            if (found.isPresent()) {
                var msg = "This url exists for this location. Change location";
                log.error(msg);
                NewDownloadUtils.disableControlsAndShowError(msg, errorLabel, downloadBtn, addBtn);
                return false;
            }
        }


        dm.setUrl(url);
        if (path.endsWith(File.separator))
            dm.setFilePath(path + fileName);
        else
            dm.setFilePath(path + File.separator + fileName);
        dm.setProgress(0);
        dm.setName(fileName);
        dm.setChunks(Integer.parseInt(chunksField.getText()));
        dm.setAddDate(LocalDateTime.now());
        dm.setAddToQueueDate(LocalDateTime.now());
        var selectedQueue = queueCombo.getSelectionModel().getSelectedItem();
        var allDownloadsQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, false);
        dm.getQueues().add(allDownloadsQueue);
        if (selectedQueue.getId() != allDownloadsQueue.getId())
            dm.getQueues().add(selectedQueue);

        dm.setDownloadStatus(DownloadStatus.Trying);
        return true;
    }


    @Override
    public void updateQueue() {
        var queues = AppConfigs.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, false);
        queues = queues.stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().clear();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
    }

    @FXML
    private void onQueueChanged() {
        onOfflineFieldsChanged();
    }

    private void onOfflineFieldsChanged() {
        NewDownloadUtils.onOfflineFieldsChanged(locationField, nameField.getText(), dm, queueCombo,
                errorLabel, downloadBtn, addBtn, openLocation);
    }


}
