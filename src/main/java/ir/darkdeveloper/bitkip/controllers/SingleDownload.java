package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.interfaces.NewDownloadFxmlController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.DownloadOpUtils;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import ir.darkdeveloper.bitkip.utils.MainTableUtils;
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
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class SingleDownload implements NewDownloadFxmlController {

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
    private MainTableUtils mainTableUtils;

    private final DownloadModel dm = new DownloadModel();


    @Override
    public void setMainTableUtils(MainTableUtils mainTableUtils) {
        this.mainTableUtils = mainTableUtils;
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
                "File is seperated into parts and will be downloaded concurrently"
        };
        NewDownloadUtils.initPopOvers(questionBtns, contents);
        NewDownloadUtils.validInputChecks(chunksField, bytesField, speedField);
        NewDownloadUtils.prepareLinkFromClipboard(urlField);
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
        autoFillLocationAndSizeAndName(true);

        bytesField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                bytesField.setText(newValue.replaceAll("\\D", ""));
            chunksField.setDisable(!bytesField.getText().equals(dm.getSize() + ""));
            speedField.setDisable(!bytesField.getText().equals(dm.getSize() + ""));
        });

    }

    private void autoFillLocationAndSizeAndName(boolean prepareFileName) {
        try {
            var link = urlField.getText();
            var connection = NewDownloadUtils.connect(link, 3000, 3000);
            var executor = Executors.newFixedThreadPool(2);
            CompletableFuture<Void> fileNameLocationFuture = CompletableFuture.completedFuture(null);
            if (prepareFileName)
                fileNameLocationFuture = NewDownloadUtils.prepareFileNameAndFieldsAsync(connection, link, nameField, executor)
                        .thenAccept(fileName -> NewDownloadUtils.determineLocationAndQueue(locationField, fileName, dm));
            var sizeFuture = NewDownloadUtils.prepareFileSizeAndFieldsAsync(connection, urlField, sizeLabel, chunksField, bytesField, dm, executor);
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
                        executor.shutdown();
                    })
                    .exceptionally(throwable -> {
                        if (!executor.isShutdown())
                            executor.shutdown();
                        errorLabel.setVisible(true);
                        downloadBtn.setDisable(true);
                        addBtn.setDisable(true);
                        var errorMsg = throwable.getCause().getLocalizedMessage();
                        Platform.runLater(() -> errorLabel.setText(errorMsg));
                        return null;
                    });
        } catch (Exception e) {
            errorLabel.setVisible(true);
            downloadBtn.setDisable(true);
            addBtn.setDisable(true);
            var errorMsg = e.getLocalizedMessage();
            errorLabel.setText(errorMsg);
        }
    }


    @FXML
    private void onSelectLocation(ActionEvent e) {
        NewDownloadUtils.selectLocation(e, locationField);
    }

    @FXML
    private void onNewQueue() {
        FxUtils.newQueueStage();
    }

    @FXML
    private void onCancel() {
        stage.close();
    }

    @FXML
    private void onAdd() {
        prepareDownload();
        dm.setDownloadStatus(DownloadStatus.Paused);
        DownloadsRepo.insertDownload(dm);
        mainTableUtils.addRow(dm);
        stage.close();
    }

    @FXML
    private void onDownload() {
        prepareDownload();
        dm.setLastTryDate(LocalDateTime.now());
        NewDownloadUtils.startDownload(dm, mainTableUtils, speedField.getText(), bytesField.getText(), false);
        DownloadOpUtils.openDownloadingStage(dm, mainTableUtils);
        stage.close();
    }

    private void prepareDownload() {
        dm.setUrl(urlField.getText());
        var fileName = nameField.getText();
        var path = locationField.getText();
        if (path.endsWith(File.separator))
            dm.setFilePath(path + fileName);
        else
            dm.setFilePath(path + File.separator + fileName);
        dm.setProgress(0);
        dm.setName(fileName);
        dm.setChunks(Integer.parseInt(chunksField.getText()));
        dm.setAddDate(LocalDateTime.now());
        var selectedQueue = queueCombo.getSelectionModel().getSelectedItem();
        var allDownloadsQueue = QueuesRepo.findByName("All Downloads");
        dm.getQueue().add(allDownloadsQueue);
        if (selectedQueue.getId() != allDownloadsQueue.getId())
            dm.getQueue().add(selectedQueue);

        dm.setDownloadStatus(DownloadStatus.Trying);
    }


    @Override
    public void updateQueue() {
        var queues = AppConfigs.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getQueues();
        queues = queues.stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().clear();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
    }
}
