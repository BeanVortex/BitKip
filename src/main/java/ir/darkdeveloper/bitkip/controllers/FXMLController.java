package ir.darkdeveloper.bitkip.controllers;

import javafx.fxml.FXML;
import javafx.stage.Stage;

public interface FXMLController {

    @FXML
    void initialize();

    void setStage(Stage stage);
    Stage getStage();
}

