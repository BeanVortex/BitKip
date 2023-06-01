package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements FXMLController {

    public Label lblLocation;
    public Button btnChangeDir;
    public Circle circleTheme;
    public Line line1;
    public CheckBox serverCheck;
    public TextField portField;
    public VBox parent;
    public CheckBox newFileCheck;
    private Stage stage;


    @Override
    public void initAfterStage() {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

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

    public void changeSaveDir() {
    }
}
