package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.observers.QueueObserver;
import ir.darkdeveloper.bitkip.config.observers.QueueSubject;
import ir.darkdeveloper.bitkip.models.*;
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
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.config.observers.QueueSubject.getQueueSubject;
import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.DownloadUtils.getNewFileNameIfExists;
import static ir.darkdeveloper.bitkip.utils.DownloadUtils.handleError;
import static ir.darkdeveloper.bitkip.utils.IOUtils.getBytesFromString;
import static ir.darkdeveloper.bitkip.utils.IOUtils.getFreeSpace;
import static ir.darkdeveloper.bitkip.utils.Validations.maxChunks;

public class SingleDownload implements QueueObserver {

    @FXML
    private Label sizeLabel, resumableLabel, errorLabel;
    @FXML
    private Button questionBtnSpeed, openLocation, questionBtnChunks,
            questionBtnBytes, downloadBtn, refreshBtn, addBtn, newQueue;
    @FXML
    private TextField urlField, chunksField, nameField, locationField, speedField, bytesField;
    @FXML
    private ComboBox<QueueModel> queueCombo;

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
        updateTheme(stage.getScene());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var queues = QueueSubject.getQueues();
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
                "You can limit downloading speed. calculated in MB. (0.8 means 838KB)",
                "You can specify how many bytes of the file to download (roughly)",
                "File is seperated into parts and will be downloaded concurrently"
        };

        DownloadUtils.initPopOvers(questionBtns, contents);
        Validations.validateInputChecks(chunksField, bytesField, speedField, dm);

    }

    private void initAfterUrlModel() {
        if (urlModel != null)
            setInputValuesFromExtension(urlModel);
        else
            Validations.prepareLinkFromClipboard(urlField);

        queueCombo.getSelectionModel().selectedIndexProperty().addListener(observable -> onQueueChanged());
        urlField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                DownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            else autoFillLocationAndSizeAndName();
        });
        nameField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                DownloadUtils.disableControlsAndShowError("Name is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            else onOfflineFieldsChanged();
        });
        locationField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                DownloadUtils.disableControlsAndShowError("Location is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            else onOfflineFieldsChanged();
        });
        if (urlModel == null)
            autoFillLocationAndSizeAndName();
    }

    private void setInputValuesFromExtension(SingleURLModel urlModel) {
        var fileSize = urlModel.fileSize();
        urlField.setText(urlModel.url());
        nameField.setText(urlModel.filename());
        setLocation(urlModel.filename());
        sizeLabel.setText(IOUtils.formatBytes(fileSize));
        bytesField.setText(String.valueOf(fileSize));
        HttpURLConnection conn;
        try {
            conn = DownloadUtils.connect(urlModel.url());
        } catch (IOException e) {
            log.error(e.getMessage());
            return;
        }
        var resumable = DownloadUtils.canResume(conn);
        chunksField.setText(resumable ? String.valueOf(maxChunks(fileSize)) : "0");
        speedField.setDisable(!resumable);
        bytesField.setDisable(!resumable);
        if (resumable) {
            resumableLabel.setText("Yes");
            resumableLabel.getStyleClass().add("yes");
            resumableLabel.getStyleClass().remove("no");
        } else {
            resumableLabel.setText("No");
            resumableLabel.getStyleClass().add("no");
            resumableLabel.getStyleClass().remove("yes");
        }
        dm.setResumable(resumable);
        dm.setSize(fileSize);
    }

    private void autoFillLocationAndSizeAndName() {
        try {
            downloadBtn.setDisable(true);
            addBtn.setDisable(true);
            // firing select event
            queueCombo.getSelectionModel().select(queueCombo.getSelectionModel().getSelectedIndex());
            var url = urlField.getText();
            var connection = DownloadUtils.connect(url);
            var executor = Executors.newVirtualThreadPerTaskExecutor();
            var fileNameLocationFuture =
                    DownloadUtils.prepareFileNameAndFieldsAsync(connection, url, nameField, executor)
                            .thenAccept(this::setLocation);
            var sizeFuture = DownloadUtils.prepareFileSizeAndFieldsAsync(connection,
                    urlField, sizeLabel, resumableLabel, speedField, chunksField, bytesField, dm, executor);
            CompletableFuture.allOf(fileNameLocationFuture, sizeFuture)
                    .whenComplete((unused, throwable) -> {
                        handleError(() -> DownloadUtils.checkIfFileIsOKToSave(locationField.getText(),
                                nameField.getText(), downloadBtn, addBtn, refreshBtn), errorLabel);
                        executor.shutdown();
                    })
                    .exceptionally(throwable -> {
                        if (!executor.isShutdown())
                            executor.shutdown();
                        var errMsg = throwable.getCause().getLocalizedMessage();
                        Platform.runLater(() ->
                                DownloadUtils.disableControlsAndShowError(errMsg, errorLabel, downloadBtn, addBtn, refreshBtn));
                        return null;
                    });
        } catch (Exception e) {
            DownloadUtils.disableControlsAndShowError(e.getLocalizedMessage(), errorLabel, downloadBtn, addBtn, refreshBtn);
        }
    }

    private void setLocation(String fileName) {
        DownloadUtils.setLocationAndQueue(locationField, fileName, dm);
    }

    @FXML
    private void onSelectLocation(ActionEvent e) {
        DownloadUtils.selectLocation(e, locationField);
        handleError(() -> DownloadUtils.checkIfFileIsOKToSave(locationField.getText(),
                nameField.getText(), downloadBtn, addBtn, refreshBtn), errorLabel);
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
            var freeSpace = getFreeSpace(Path.of(dm.getFilePath()).getParent());
            // if after saving, the space left should be above 100MB
            if (freeSpace - dm.getSize() <= Math.pow(2, 20) * 100) {
                var res = FxUtils.askWarning("No Free space",
                        "The location you chose, has not enough space to save the download file." +
                                " Do you want to change location now?");
                if (!res) {
                    DownloadsRepo.insertDownload(dm);
                    mainTableUtils.addRow(dm);
                    getQueueSubject().removeObserver(this);
                    stage.close();
                }
                return;
            }
            DownloadOpUtils.startDownload(dm, getBytesFromString(speedField.getText()), Long.parseLong(bytesField.getText()),
                    false, false);
            DownloadOpUtils.openDetailsStage(dm);
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
            DownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            return false;
        }
        if (fileName.isBlank()) {
            log.warn("Name is blank");
            DownloadUtils.disableControlsAndShowError("Name is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            return false;
        }
        if (path.isBlank()) {
            log.warn("Location is blank");
            DownloadUtils.disableControlsAndShowError("Location is blank", errorLabel, downloadBtn, addBtn, refreshBtn);
            return false;
        }

        var newFileName = getNewFileNameIfExists(fileName, path);
        fileName = addSameDownload ? newFileName : fileName;
        if (!newFileName.equals(fileName)) {
            var msg = "This url and name exists for this location. Change location or name";
            log.error(msg);
            DownloadUtils.disableControlsAndShowError(msg, errorLabel, downloadBtn, addBtn, refreshBtn);
            return false;
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
        dm.setTurnOffMode(TurnOffMode.NOTHING);
        dm.setOpenAfterComplete(false);
        dm.setSpeedLimit(getBytesFromString(speedField.getText()));
        dm.setByteLimit(Long.parseLong(bytesField.getText()));
        var selectedQueue = queueCombo.getSelectionModel().getSelectedItem();
        var allDownloadsQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, false);
        dm.getQueues().add(allDownloadsQueue);
        if (selectedQueue.getId() != allDownloadsQueue.getId())
            dm.getQueues().add(selectedQueue);
        dm.setDownloadStatus(DownloadStatus.Paused);
        return true;
    }


    @Override
    public void updateQueue() {
        BatchDownload.updateQueueData(queueCombo);
    }

    @FXML
    private void onQueueChanged() {
        onOfflineFieldsChanged();
    }

    private void onOfflineFieldsChanged() {
        handleError(() -> DownloadUtils.onOfflineFieldsChanged(locationField, nameField.getText(), dm, queueCombo,
                downloadBtn, addBtn, openLocation, refreshBtn), errorLabel);
    }


    public void setUrlModel(SingleURLModel urlModel) {
        this.urlModel = urlModel;
        initAfterUrlModel();
    }

}
