package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.SingleURLModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
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
import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.InputValidations.maxChunks;

public class SingleDownload implements QueueObserver {

    @FXML
    private Label errorLabel;
    @FXML
    private Label resumableLabel;
    @FXML
    private Button newQueue;
    @FXML
    private Button addBtn;
    @FXML
    private Button refreshBtn;
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

    private final DownloadModel dm = new DownloadModel();
    private Stage stage;
    private SingleURLModel urlModel;


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
        refreshBtn.setGraphic(new FontIcon());
        refreshBtn.setDisable(true);
        refreshBtn.setVisible(false);
        refreshBtn.setOnAction(e -> {
            refreshBtn.setDisable(true);
            refreshBtn.setVisible(false);
            autoFillLocationAndSizeAndName();
        });
        var questionBtns = new Button[]{questionBtnSpeed, questionBtnBytes, questionBtnChunks};
        var contents = new String[]{
                "You can limit downloading speed. calculated in MB. (0.8 means 800KB)",
                "You can specify how many bytes of the file to download (Disabled in chunks downloading mode)",
                "File is seperated into parts and will be downloaded concurrently"
        };

        NewDownloadUtils.initPopOvers(questionBtns, contents);
        InputValidations.validInputChecks(chunksField, bytesField, speedField, dm);

    }

    private void initAfterUrlModel() {
        if (urlModel != null)
            setInputValuesFromExtension(urlModel);
        else
            InputValidations.prepareLinkFromClipboard(urlField);

        queueCombo.getSelectionModel().selectedIndexProperty().addListener(observable -> onQueueChanged());
        urlField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                NewDownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            else autoFillLocationAndSizeAndName();
        });
        nameField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                NewDownloadUtils.disableControlsAndShowError("Name is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            else onOfflineFieldsChanged();
        });
        locationField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                NewDownloadUtils.disableControlsAndShowError("Location is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            else onOfflineFieldsChanged();
        });
        if (urlModel == null)
            autoFillLocationAndSizeAndName();
    }

    private void setInputValuesFromExtension(SingleURLModel urlModel) {
        urlField.setText(urlModel.url());
        nameField.setText(urlModel.filename());
        setLocation(urlModel.filename());
        sizeLabel.setText(IOUtils.formatBytes(urlModel.fileSize()));
        bytesField.setText(urlModel.fileSize() + "");
        chunksField.setDisable(!urlModel.resumable());
        chunksField.setText(urlModel.resumable() ? maxChunks() + "" : "0");
        speedField.setDisable(false);
        var resumable = urlModel.resumable();
        resumableLabel.setTextFill(resumable ? Paint.valueOf("#388E3C") : Paint.valueOf("#EF5350"));
        resumableLabel.setText(resumable ? "Yes" : "No");
        dm.setResumable(resumable);
        dm.setSize(urlModel.fileSize());
        dm.setAgent(urlModel.agent());
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
            var sizeFuture = NewDownloadUtils.prepareFileSizeAndFieldsAsync(connection, urlField, sizeLabel,
                    resumableLabel, chunksField, bytesField, dm, executor);
            CompletableFuture.allOf(fileNameLocationFuture, sizeFuture)
                    .whenComplete((unused, throwable) -> {
                        NewDownloadUtils.checkIfFileIsOKToSave(locationField.getText(), nameField.getText(), errorLabel, downloadBtn, addBtn);
                        executor.shutdown();
                    })
                    .exceptionally(throwable -> {
                        if (!executor.isShutdown())
                            executor.shutdown();
                        var errMsg = throwable.getCause().getLocalizedMessage();
                        Platform.runLater(() ->
                                NewDownloadUtils.disableControlsAndShowError(errMsg, errorLabel, downloadBtn, addBtn, refreshBtn));
                        return null;
                    });
        } catch (Exception e) {
            NewDownloadUtils.disableControlsAndShowError(e.getLocalizedMessage(), errorLabel, downloadBtn, addBtn, refreshBtn);
        }
    }

    private void setLocation(String fileName) {
        NewDownloadUtils.determineLocationAndQueue(locationField, fileName, dm);
    }

    @FXML
    private void onSelectLocation(ActionEvent e) {
        NewDownloadUtils.selectLocation(e, locationField);
        NewDownloadUtils.checkIfFileIsOKToSave(locationField.getText(), nameField.getText(), errorLabel, downloadBtn, addBtn);
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
            NewDownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            return false;
        }
        if (fileName.isBlank()) {
            log.warn("Name is blank");
            NewDownloadUtils.disableControlsAndShowError("Name is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            return false;
        }
        if (path.isBlank()) {
            log.warn("Location is blank");
            NewDownloadUtils.disableControlsAndShowError("Location is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            return false;
        }

        var byURL = DownloadsRepo.findByURL(url);
        if (!byURL.isEmpty()) {
            var found = byURL.stream()
                    .filter(dm -> {
                        var p = Paths.get(dm.getFilePath()).getParent().toString() + File.separator;
                        var n = dm.getName();
                        return p.equals(path) && n.equals(fileName);
                    })
                    .findFirst();
            if (found.isPresent()) {
                var msg = "This url and name exists for this location. Change location or name";
                log.error(msg);
                NewDownloadUtils.disableControlsAndShowError(msg, errorLabel, downloadBtn, addBtn, refreshBtn);
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
        dm.setShowCompleteDialog(showCompleteDialog);
        dm.setOpenAfterComplete(false);
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


    public void setUrlModel(SingleURLModel urlModel) {
        this.urlModel = urlModel;
        initAfterUrlModel();
    }
}
