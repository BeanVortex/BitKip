package ir.darkdeveloper.bitkip.controllers;

import javafx.stage.Stage;

public class NewDownload implements FXMLController{
    private Stage stage;
    @Override
    public Stage getStage() {
        return stage;
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
    public void initialize() {

    }
}
