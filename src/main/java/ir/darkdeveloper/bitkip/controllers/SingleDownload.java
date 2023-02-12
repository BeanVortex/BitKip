package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.task.DownloadInChunksTask;
import ir.darkdeveloper.bitkip.task.DownloadLimitedTask;
import ir.darkdeveloper.bitkip.task.DownloadTask;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import ir.darkdeveloper.bitkip.utils.TableUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SingleDownload implements FXMLController, NewDownloadFxmlController {

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
    private TableUtils tableUtils;

    private final DownloadModel downloadModel = new DownloadModel();
    private final List<DownloadTask> downloadTaskList = AppConfigs.downloadTaskList;


    @Override
    public void setTableUtils(TableUtils tableUtils) {
        this.tableUtils = tableUtils;
    }


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
    public void initialize() {
        var queues = QueuesRepo.getQueues().stream().filter(QueueModel::isCanAddDownload).toList();
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
                "File is seperated into parts and will be downloaded concurrently (Needs 2* disk space after downloading complete)"};
        NewDownloadUtils.initPopOvers(questionBtns, contents);
        NewDownloadUtils.validInputChecks(chunksField, bytesField, speedField);
        NewDownloadUtils.prepareLinkFromClipboard(urlField);
        autoFillLocationAndSizeAndName(true);

        bytesField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                bytesField.setText(newValue.replaceAll("\\D", ""));
            chunksField.setDisable(!bytesField.getText().equals(downloadModel.getSize() + ""));
            speedField.setDisable(!bytesField.getText().equals(downloadModel.getSize() + ""));
        });

        urlField.textProperty().addListener((o, oldValue, newValue) -> {
            if (!newValue.isBlank())
                autoFillLocationAndSizeAndName(true);
        });
        nameField.textProperty().addListener((o, oldValue, newValue) -> {
            if (!newValue.isBlank())
                autoFillLocationAndSizeAndName(false);
        });
        locationField.textProperty().addListener((o, oldValue, newValue) -> {
            if (!newValue.isBlank())
                autoFillLocationAndSizeAndName(false);
        });
    }

    private void autoFillLocationAndSizeAndName(boolean prepareFileName) {
        CompletableFuture<Void> fileNameLocationFuture = CompletableFuture.completedFuture(null);
        if (prepareFileName)
            fileNameLocationFuture = NewDownloadUtils.prepareFileName(urlField, nameField)
                    .thenAccept(fileName -> NewDownloadUtils.determineLocation(locationField, fileName, downloadModel));
        var sizeFuture = NewDownloadUtils.prepareSize(urlField, sizeLabel, bytesField, downloadModel);
        CompletableFuture.allOf(fileNameLocationFuture, sizeFuture)
                .whenComplete((unused, throwable) -> {
                    var file = new File(locationField.getText() + nameField.getText());
                    var chunkFile = new File(locationField.getText() + nameField.getText() + "#0");
                    if (file.exists() || chunkFile.exists()) {
                        errorLabel.setVisible(true);
                        downloadBtn.setDisable(true);
                        addBtn.setDisable(true);
                        Platform.runLater(() -> errorLabel.setText("File with this name exists in this location"));
                    } else {
                        errorLabel.setVisible(false);
                        downloadBtn.setDisable(false);
                        addBtn.setDisable(false);
                    }
                });
    }


    @FXML
    private void onSelectLocation(ActionEvent e) {
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

    @FXML
    private void onNewQueue() {
    }

    @FXML
    private void onCancel() {
        stage.close();
    }

    @FXML
    private void onAdd() {
    }

    @FXML
    private void onDownload() {
        downloadModel.setUrl(urlField.getText());
        var fileName = nameField.getText();
        var path = locationField.getText();
        if (path.endsWith(File.separator))
            downloadModel.setFilePath(path + fileName);
        else
            downloadModel.setFilePath(path + File.separator + fileName);
        downloadModel.setProgress(0);
        downloadModel.setName(fileName);
        downloadModel.setChunks(Integer.parseInt(chunksField.getText()));
        downloadModel.setAddDate(LocalDateTime.now());
        downloadModel.setLastTryDate(LocalDateTime.now());
        var selectedQueue = queueCombo.getSelectionModel().getSelectedItem();
        var allDownloadsQueue = QueuesRepo.findByName("All Downloads");
        downloadModel.getQueue().add(allDownloadsQueue);
        downloadModel.setDownloadStatus(DownloadStatus.Downloading);
        if (selectedQueue.getId() != allDownloadsQueue.getId())
            downloadModel.getQueue().add(selectedQueue);
        DownloadTask downloadTask;
        if (downloadModel.getChunks() == 0) {
            if (speedField.getText().equals("0")) {
                if (bytesField.getText().equals(downloadModel.getSize() + ""))
                    downloadTask = new DownloadLimitedTask(downloadModel, Long.MAX_VALUE, false);
                else
                    downloadTask = new DownloadLimitedTask(downloadModel, getBytesFromField(bytesField.getText()), false);
            } else
                downloadTask = new DownloadLimitedTask(downloadModel, getBytesFromField(speedField.getText()), true);
        } else {
            downloadTask = new DownloadInChunksTask(downloadModel);
            downloadTask.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (oldValue == null)
                    oldValue = 0L;
                var speed = (newValue - oldValue) * 2;
                // todo :remaining time
                if (newValue == 0)
                    speed = 0;
                downloadModel.setDownloadStatus(DownloadStatus.Downloading);
                tableUtils.updateDownloadSpeed(speed, downloadModel.getId());
            });
        }

        downloadTask.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue == null)
                oldValue = 0L;
            var speed = (newValue - oldValue);
            // todo :remaining time
            if (newValue == 0)
                speed = 0;
            downloadModel.setDownloadStatus(DownloadStatus.Downloading);
            tableUtils.updateDownloadSpeed(speed, downloadModel.getId());
        });
        downloadTask.progressProperty().addListener((o, old, newV) -> {
            downloadModel.setDownloadStatus(DownloadStatus.Downloading);
            tableUtils.updateDownloadProgress(newV.floatValue() * 100, downloadModel.getId());
        });

        DownloadsRepo.insertDownload(downloadModel);
        tableUtils.addRow(downloadModel);
        downloadTaskList.add(downloadTask);
        var t = new Thread(downloadTask);
        t.setDaemon(true);
        t.start();
        stage.close();
    }

    private long getBytesFromField(String mb) {
        var mbVal = Double.parseDouble(mb);
        return (long) (mbVal * Math.pow(2, 20));
    }


}
