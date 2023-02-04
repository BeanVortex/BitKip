package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicReference;

public class WindowUtils {
    private static double xOffset = 0;
    private static double yOffset = 0;

    public static void toolbarInits(HBox toolbar, Stage stage, Rectangle2D bounds,
                                    Button actionBtn, TableView<DownloadModel> table) {
        toolbar.setOnMousePressed(event -> {
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });
        var boundsCopy = new AtomicReference<>(bounds);
        toolbar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
            Screen.getScreens().forEach(screen -> {
                var currentX = stage.getX();
                if (!isOnPrimaryScreen(currentX))
                    boundsCopy.set(screen.getVisualBounds());
                else
                    boundsCopy.set(bounds);
                if (stage.getWidth() == boundsCopy.get().getWidth())
                    if (stage.getHeight() == boundsCopy.get().getHeight() || stage.getHeight() == boundsCopy.get().getHeight() + boundsCopy.get().getMinY())
                        minimizeWindow(stage, boundsCopy.get());
            });
        });
        toolbar.setOnMouseReleased(event -> {
            var screenY = event.getScreenY();
            if (screenY - boundsCopy.get().getMinY() <= 0)
                maximizeWindow(stage, boundsCopy.get(), actionBtn);
        });
        toolbar.setOnMouseClicked(event -> {
            var doubleClickCondition = event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2;
            var screenY = stage.getY();
            table.getSelectionModel().clearSelection();
            if (doubleClickCondition) {
                if (screenY - boundsCopy.get().getMinY() >= 0 && boundsCopy.get().getHeight() > stage.getHeight())
                    maximizeWindow(stage, boundsCopy.get(), actionBtn);
                else if (screenY - boundsCopy.get().getMinY() <= 0 && boundsCopy.get().getHeight() <= stage.getHeight())
                    minimizeWindow(stage, boundsCopy.get());
            }
        });
    }

    public static Rectangle2D maximizeWindow(Stage stage, Rectangle2D bounds, Button actionBtn) {
        var currentX = stage.getX();
        var boundsCopy = new AtomicReference<>(bounds);
        Screen.getScreens().forEach(screen -> {
            if (!isOnPrimaryScreen(currentX))
                boundsCopy.set(screen.getVisualBounds());
            maximizeStage(stage, boundsCopy.get(), actionBtn);
        });
        return boundsCopy.get();
    }

    public static Rectangle2D minimizeWindow(Stage stage, Rectangle2D bounds) {
        var currentX = stage.getX();
        var boundsCopy = new AtomicReference<>(bounds);
        Screen.getScreens().forEach(screen -> {
            if (!isOnPrimaryScreen(currentX))
                boundsCopy.set(screen.getVisualBounds());
            minimizeStage(stage, boundsCopy.get());
        });
        return boundsCopy.get();
    }

    private static void maximizeStage(Stage stage, Rectangle2D bounds, Button actionBtn) {
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        if (actionBtn.getTranslateY() == 100)
            actionBtn.setTranslateY(0);
    }

    private static void minimizeStage(Stage stage, Rectangle2D bounds) {
        var width = 853;
        var height = 515;
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setY((bounds.getMaxY() - height) / 2);
        stage.setX((bounds.getMaxX() - width) / 2);
    }

    public static boolean isOnPrimaryScreen(double x) {
        var bounds = Screen.getPrimary().getVisualBounds();
        return x < bounds.getMaxX();
    }


}
