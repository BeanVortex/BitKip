package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class SingleDownload implements FXMLController {

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
        var queues = QueuesRepo.getQueues();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
        chunksField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                chunksField.setText(newValue.replaceAll("\\D", ""));
            if (newValue.isBlank())
                chunksField.setText("0");
            var chunks = Integer.parseInt(chunksField.getText());
            var cores = Runtime.getRuntime().availableProcessors();
            if (chunks > cores * 2)
                chunks = cores * 2;
            chunksField.setText(chunks + "");
        });
        speedField.textProperty().addListener((o, old, newValue) -> {
            if (newValue.isBlank())
                speedField.setText("0");
        });
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
    private void onQuestionChunks(ActionEvent actionEvent) {
    }
}
