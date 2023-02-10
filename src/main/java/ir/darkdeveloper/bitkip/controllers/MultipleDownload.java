package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import ir.darkdeveloper.bitkip.utils.TableUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class MultipleDownload implements FXMLController, NewDownloadFxmlController {
    @FXML
    private Button newQueue;
    @FXML
    private TextField startField;
    @FXML
    private TextField endField;
    @FXML
    private TextField locationField;
    @FXML
    private Button openLocation;
    @FXML
    private ComboBox<QueueModel> queueCombo;
    @FXML
    private TextField speedField;
    @FXML
    private Button questionBtnSpeed;
    @FXML
    private TextField chunksField;
    @FXML
    private Button questionBtnChunks;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button addBtn;
    @FXML
    private Button questionBtnUrl;
    @FXML
    private TextField urlField;

    private Stage stage;
    private TableUtils tableUtils;

    @Override
    public void initialize() {
        addBtn.setGraphic(new FontIcon());
        cancelBtn.setGraphic(new FontIcon());
        questionBtnChunks.setGraphic(new FontIcon());
        openLocation.setGraphic(new FontIcon());
        questionBtnSpeed.setGraphic(new FontIcon());
        questionBtnUrl.setGraphic(new FontIcon());
        newQueue.setGraphic(new FontIcon());
        var queues = QueuesRepo.getQueues().stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
        NewDownloadUtils.validInputChecks(chunksField, null, speedField);
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    @Override
    public void initAfterStage() {

    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @FXML
    private void onQuestionUrl(ActionEvent actionEvent) {
    }


    @FXML
    private void onSelectLocation(ActionEvent actionEvent) {
    }

    @FXML

    private void onQueueChanged(ActionEvent actionEvent) {
    }

    @FXML

    private void onQuestionSpeed(ActionEvent actionEvent) {
    }

    @FXML
    private void onQuestionChunks(ActionEvent actionEvent) {
    }

    @FXML
    private void onNewQueue(ActionEvent actionEvent) {
    }

    @Override
    public void setTableUtils(TableUtils tableUtils) {
        this.tableUtils = tableUtils;
    }
}
