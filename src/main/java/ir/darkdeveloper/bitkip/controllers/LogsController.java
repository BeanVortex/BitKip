package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import static ir.darkdeveloper.bitkip.config.AppConfigs.dataPath;
import static ir.darkdeveloper.bitkip.config.AppConfigs.log;

public class LogsController implements FXMLController {

    @FXML
    private TextArea logArea;

    private Stage stage;

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    @Override
    public void initAfterStage() {
        logArea.setPrefHeight(stage.getHeight());
        stage.widthProperty().addListener((o, ol, n) -> logArea.setPrefWidth(n.doubleValue()));
        stage.heightProperty().addListener((o, ol, n) -> logArea.setPrefHeight(n.doubleValue()));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            var logs = Files.readAllLines(Path.of(dataPath + "BitKip.log"))
                    .stream().reduce((s1, s2) -> String.join("\n", s1, s2));
            logArea.setText(logs.orElse("No logs"));
        } catch (IOException e) {
            log.severe(e.getLocalizedMessage());
            Notifications.create()
                    .title("Failed to read log file")
                    .text(e.getLocalizedMessage())
                    .showError();
        }
    }

    @Override
    public Stage getStage() {
        return stage;
    }
}
