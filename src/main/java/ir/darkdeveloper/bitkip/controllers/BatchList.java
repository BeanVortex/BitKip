package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import javafx.stage.Stage;

public class BatchList implements FXMLController {
    private Stage stage;

    @Override
    public void initAfterStage() {

    }

    @Override
    public void initialize() {

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

}
