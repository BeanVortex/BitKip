package ir.darkdeveloper.bitkip.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class MainController implements FXMLController {

    @FXML
    private HBox mainBox;
    @FXML
    private HBox toolbar;
    @FXML
    private Button closeBtn;
    @FXML
    private Button fullWindowBtn;
    @FXML
    private Button hideBtn;

    private Stage stage;
    private static double xOffset = 0;
    private static double yOffset = 0;
    private Rectangle2D bounds;


    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void initialize() {
        closeBtn.setGraphic(new FontIcon());
        fullWindowBtn.setGraphic(new FontIcon());
        hideBtn.setGraphic(new FontIcon());
        var screen = Screen.getPrimary();
        bounds = screen.getVisualBounds();
        mainBox.setPrefHeight(bounds.getHeight());
        toolbarInits();
    }

    private void toolbarInits() {
        toolbar.setOnMousePressed(event -> {
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });
        toolbar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
            if (stage.getWidth() == bounds.getWidth() && stage.getHeight() == bounds.getHeight())
                minimizeWindow();
        });
        toolbar.setOnMouseReleased(event -> {
            var screenY = event.getScreenY();
            if (screenY <= 0)
                maximizeWindow();
        });
        toolbar.setOnMouseClicked(event -> {
            var doubleClickCondition = event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2;
            var screenY = stage.getY();
            if (screenY > 0 && doubleClickCondition)
                maximizeWindow();
            else if (screenY == 0 && doubleClickCondition)
                minimizeWindow();
        });
    }

    private void maximizeWindow() {
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    private void minimizeWindow() {
        var width = 850;
        var height = 512;
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setY((bounds.getMaxY() - height) / 2);
        stage.setX((bounds.getMaxX() - width) / 2);
    }


    public void closeApp() {
        Platform.exit();
    }

    public void hideWindowApp() {
        stage.setIconified(true);
    }

    public void toggleFullWindowApp() {
        var screenY = stage.getY();
        if (screenY > 0)
            maximizeWindow();
        else if (screenY == 0)
            minimizeWindow();

    }
}
