package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
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
import java.util.concurrent.CompletableFuture;

public class SingleDownload implements FXMLController {

    @FXML
    private Button newQueue;
    @FXML
    private Button cancelBtn;
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

    private final DownloadModel downloadModel = new DownloadModel();

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
        NewDownloadUtils.validInputChecks(chunksField, bytesField, speedField);
        NewDownloadUtils.prepareLinkFromClipboard(urlField);
        autoFillLocationAndSizeAndName();
        urlField.textProperty().addListener((o, oldValue, newValue) -> {
            if (!oldValue.isBlank() && !newValue.isBlank())
                autoFillLocationAndSizeAndName();
        });
    }

    private void autoFillLocationAndSizeAndName() {
        var fileNameLocationFuture = NewDownloadUtils.prepareFileName(urlField, nameField)
                .thenAccept(fileName -> NewDownloadUtils.determineLocation(locationField, fileName));
        var sizeFuture = NewDownloadUtils.prepareSize(urlField, sizeLabel, downloadModel);
        CompletableFuture.allOf(fileNameLocationFuture, sizeFuture)
                .whenComplete((unused, throwable) -> {
                    downloadBtn.setDisable(false);
                    addBtn.setDisable(false);
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
    private void onQueueChanged() {

    }

    @FXML
    private void onQuestionSpeed() {
    }

    @FXML
    private void onQuestionChunks() {
    }

    @FXML
    private void onQuestionBytes() {
    }

    @FXML
    private void onNewQueue(ActionEvent actionEvent) {
    }
}
