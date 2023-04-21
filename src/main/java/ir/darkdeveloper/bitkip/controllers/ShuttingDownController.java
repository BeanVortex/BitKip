package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.TurnOffMode;
import ir.darkdeveloper.bitkip.utils.PowerUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShuttingDownController implements FXMLController {
    @FXML
    private Button cancelBtn;
    @FXML
    private Label modeLbl;
    @FXML
    private Label counterLbl;


    private TurnOffMode turnOffMode;
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @Override
    public void initAfterStage() {
        var service = Executors.newCachedThreadPool();
        service.submit(() -> {
            try {
                for (int i = 4; i >= 0; i--) {
                    var finalI = i;
                    Platform.runLater(() -> counterLbl.setText(finalI + ""));
                    Thread.sleep(1000);
                }
                Platform.runLater(() -> stage.close());
                PowerUtils.turnOff(turnOffMode);
                service.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        stage.setOnCloseRequest(e -> cancel(service));
        cancelBtn.setOnAction(e -> cancel(service));
    }

    private void cancel(ExecutorService service) {
        service.shutdownNow();
        stage.close();
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

    public void setTurnOffMode(TurnOffMode turnOffMode) {
        this.turnOffMode = turnOffMode;
        modeLbl.setText(modeLbl.getText() + turnOffMode.toString().toLowerCase() + " in ");
    }
}
