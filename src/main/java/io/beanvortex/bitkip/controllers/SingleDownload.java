package io.beanvortex.bitkip.controllers;

import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.config.observers.QueueObserver;
import io.beanvortex.bitkip.config.observers.QueueSubject;
import io.beanvortex.bitkip.models.*;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import io.beanvortex.bitkip.repo.QueuesRepo;
import io.beanvortex.bitkip.utils.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static io.beanvortex.bitkip.config.AppConfigs.log;

public class SingleDownload implements QueueObserver {

    @FXML
    private CheckBox authenticatedCheck, lastLocationCheck;
    @FXML
    private Label sizeLabel, resumableLabel, errorLabel;
    @FXML
    private Button questionBtnSpeed, openLocation, questionBtnChunks,
            questionBtnBytes, downloadBtn, refreshBtn, addBtn, newQueue;
    @FXML
    private TextField usernameField, urlField, chunksField, nameField, locationField, speedField, bytesField;
    @FXML
    private PasswordField passwordField;
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
        usernameField.getParent().setManaged(false);
        usernameField.getParent().setVisible(false);
        passwordField.getParent().setManaged(false);
        passwordField.getParent().setVisible(false);
        if (AppConfigs.lastSavedDir == null)
            lastLocationCheck.setDisable(true);
        refreshBtn.setGraphic(new FontIcon());
        refreshBtn.setOnAction(e -> autoFillLocationAndSizeAndName());
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
            urlField.setText(urlModel.url());
        else
            Validations.prepareLinkFromClipboard(urlField);

        queueCombo.getSelectionModel().selectedIndexProperty().addListener(observable -> onQueueChanged());
        urlField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                DownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, downloadBtn, addBtn);
            else autoFillLocationAndSizeAndName();
        });
        nameField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                DownloadUtils.disableControlsAndShowError("Name is blank", errorLabel, downloadBtn, addBtn);
            else onOfflineFieldsChanged();
        });
        locationField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                DownloadUtils.disableControlsAndShowError("Location is blank", errorLabel, downloadBtn, addBtn);
            else onOfflineFieldsChanged();
        });
        autoFillLocationAndSizeAndName();
    }


    private void autoFillLocationAndSizeAndName() {
        try {
            downloadBtn.setDisable(true);
            addBtn.setDisable(true);
            // firing select event
            queueCombo.getSelectionModel().select(queueCombo.getSelectionModel().getSelectedIndex());
            var url = urlField.getText();
            var credential = new Credentials(usernameField.getText(), passwordField.getText());
            var connection = DownloadUtils.connect(url, credential);
            var executor = Executors.newVirtualThreadPerTaskExecutor();
            var fileNameLocationFuture =
                    DownloadUtils.prepareFileNameAndFieldsAsync(connection, url, nameField, dm, executor)
                            .thenAccept(this::setLocation);
            var sizeFuture = DownloadUtils.prepareFileSizeAndFieldsAsync(connection,
                    urlField, sizeLabel, resumableLabel, speedField, chunksField, bytesField, dm, executor);
            CompletableFuture.allOf(fileNameLocationFuture, sizeFuture)
                    .whenComplete((unused, throwable) -> {
                        DownloadUtils.handleError(() -> DownloadUtils.checkIfFileIsOKToSave(locationField.getText(),
                                nameField.getText(), downloadBtn, addBtn, lastLocationCheck), errorLabel);
                        executor.shutdown();
                    })
                    .exceptionally(throwable -> {
                        if (!executor.isShutdown())
                            executor.shutdown();
                        var errMsg = throwable.toString();
                        log.error(throwable.toString());
                        Platform.runLater(() ->
                                DownloadUtils.disableControlsAndShowError(errMsg, errorLabel, downloadBtn, addBtn));
                        return null;
                    });
        } catch (Exception e) {
            DownloadUtils.disableControlsAndShowError(e.toString(), errorLabel, downloadBtn, addBtn);
        }
    }

    private void setLocation(String fileName) {
        DownloadUtils.setLocationAndQueue(locationField, fileName, dm);
    }

    @FXML
    private void onSelectLocation(ActionEvent e) {
        var path = DownloadUtils.selectLocation(FxUtils.getStageFromEvent(e));
        if (path != null)
            locationField.setText(path);
        DownloadUtils.handleError(() -> DownloadUtils.checkIfFileIsOKToSave(locationField.getText(),
                nameField.getText(), downloadBtn, addBtn, lastLocationCheck), errorLabel);
    }

    @FXML
    private void onLastLocationCheck() {
        if (lastLocationCheck.isSelected())
            locationField.setText(AppConfigs.lastSavedDir);
        else
            setLocation(dm.getName());
    }

    @FXML
    private void onNewQueue() {
        FxUtils.newQueueStage();
    }

    @FXML
    private void onCancel() {
        stage.close();
        QueueSubject.getQueueSubject().removeObserver(this);
    }

    @FXML
    private void onAdd() {
        var prepared = prepareDownload();
        if (prepared) {
            DownloadsRepo.insertDownload(dm);
            AppConfigs.mainTableUtils.addRow(dm);
            QueueSubject.getQueueSubject().removeObserver(this);
            stage.close();
        }
    }

    @FXML
    private void onDownload() {
        var prepared = prepareDownload();
        if (prepared) {
            var freeSpace = IOUtils.getFreeSpace(Path.of(dm.getFilePath()).getParent());
            // if after saving, the space left should be above 100MB
            if (freeSpace - dm.getSize() <= Math.pow(2, 20) * 100) {
                var res = FxUtils.askWarning("No Free space",
                        "The location you chose, has not enough space to save the download file." +
                                " Do you want to change location now?");
                if (!res) {
                    DownloadsRepo.insertDownload(dm);
                    AppConfigs.mainTableUtils.addRow(dm);
                    QueueSubject.getQueueSubject().removeObserver(this);
                    stage.close();
                }
                return;
            }
            DownloadOpUtils.startDownload(dm, IOUtils.getBytesFromString(speedField.getText()), Long.parseLong(bytesField.getText()),
                    false, false);
            DownloadOpUtils.openDetailsStage(dm);
            QueueSubject.getQueueSubject().removeObserver(this);
            stage.close();
        }
    }

    private boolean prepareDownload() {

        var url = urlField.getText();
        var fileName = nameField.getText();
        var path = locationField.getText();
        if (url.isBlank()) {
            log.warn("URL is blank");
            DownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, downloadBtn, addBtn);
            return false;
        }
        if (fileName.isBlank()) {
            log.warn("Name is blank");
            DownloadUtils.disableControlsAndShowError("Name is blank", errorLabel, downloadBtn, addBtn);
            return false;
        }
        if (path.isBlank()) {
            log.warn("Location is blank");
            DownloadUtils.disableControlsAndShowError("Location is blank", errorLabel, downloadBtn, addBtn);
            return false;
        }

        var newFileName = DownloadUtils.getNewFileNameIfExists(fileName, path);
        fileName = AppConfigs.addSameDownload ? newFileName : fileName;
        if (!newFileName.equals(fileName)) {
            var msg = "This url and name exists for this location. Change location or name";
            log.error(msg);
            DownloadUtils.disableControlsAndShowError(msg, errorLabel, downloadBtn, addBtn);
            return false;
        }


        dm.setUri(url);
        if (path.endsWith(File.separator))
            dm.setFilePath(path + fileName);
        else
            dm.setFilePath(path + File.separator + fileName);
        dm.setProgress(0);
        dm.setName(fileName);
        dm.setChunks(Integer.parseInt(chunksField.getText()));
        dm.setAddDate(LocalDateTime.now());
        dm.setAddToQueueDate(LocalDateTime.now());
        dm.setShowCompleteDialog(AppConfigs.showCompleteDialog);
        dm.setTurnOffMode(TurnOffMode.NOTHING);
        dm.setOpenAfterComplete(false);
        dm.setSpeedLimit(IOUtils.getBytesFromString(speedField.getText()));
        dm.setByteLimit(Long.parseLong(bytesField.getText()));
        var selectedQueue = queueCombo.getSelectionModel().getSelectedItem();
        var allDownloadsQueue = QueuesRepo.findByName(Defaults.ALL_DOWNLOADS_QUEUE, false);
        dm.getQueues().add(allDownloadsQueue);
        if (selectedQueue.getId() != allDownloadsQueue.getId())
            dm.getQueues().add(selectedQueue);
        dm.setDownloadStatus(DownloadStatus.Paused);
        if (!usernameField.getText().isBlank() && !passwordField.getText().isBlank())
            dm.setCredentials(new Credentials(usernameField.getText(), passwordField.getText()));
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

    @FXML
    private void onAuthenticatedCheck() {
        usernameField.getParent().setManaged(authenticatedCheck.isSelected());
        usernameField.getParent().setVisible(authenticatedCheck.isSelected());
        passwordField.getParent().setManaged(authenticatedCheck.isSelected());
        passwordField.getParent().setVisible(authenticatedCheck.isSelected());
        usernameField.setText("");
        passwordField.setText("");
    }

    private void onOfflineFieldsChanged() {
        DownloadUtils.handleError(() -> DownloadUtils.onOfflineFieldsChanged(locationField, nameField.getText(), dm, queueCombo,
                downloadBtn, addBtn, openLocation, lastLocationCheck), errorLabel);
    }


    public void setUrlModel(SingleURLModel urlModel) {
        this.urlModel = urlModel;
        initAfterUrlModel();
    }

}
