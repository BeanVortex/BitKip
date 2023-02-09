package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class SingleDownload implements FXMLController {

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

    private DownloadModel downloadModel;

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
        openLocation.setGraphic(new FontIcon());
        questionBtnSpeed.setGraphic(new FontIcon());
        questionBtnChunks.setGraphic(new FontIcon());
        questionBtnBytes.setGraphic(new FontIcon());
        var queues = QueuesRepo.getQueues();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
        NewDownloadUtils.validInputChecks(chunksField, bytesField, speedField);
    }


    @FXML
    private void onSelectLocation() {
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
}
