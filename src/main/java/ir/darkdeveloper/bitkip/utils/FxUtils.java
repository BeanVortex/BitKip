package ir.darkdeveloper.bitkip.utils;


import ir.darkdeveloper.bitkip.controllers.MainController;
import ir.darkdeveloper.bitkip.controllers.NewDownloadFxmlController;
import ir.darkdeveloper.bitkip.controllers.NewQueueController;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

import static ir.darkdeveloper.bitkip.BitKip.getResource;

public class FxUtils {


    public static void switchSceneToMain(Stage stage, String fxmlFilename) {
        try {
            var loader = new FXMLLoader(getResource("fxml/" + fxmlFilename));
            Parent root = loader.load();
            var scene = new Scene(root, stage.getWidth(), stage.getHeight());
            MainController controller = loader.getController();
            controller.setStage(stage);
            stage.setScene(scene);
            stage.setTitle("BitKip");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public static Stage getStageFromEvent(ActionEvent e) {
        if (e.getSource() instanceof Node n
                && n.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    public static void newDownloadStage(String fxmlFilename, double minWidth,
                                        double minHeight, TableUtils tableUtils) {
        FXMLLoader loader;
        Stage stage = new Stage();
        Parent root;
        try {
            loader = new FXMLLoader(getResource("fxml/" + fxmlFilename));
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setMaxHeight(600);
        stage.setTitle("New Download");
        NewDownloadFxmlController controller = loader.getController();
        controller.setStage(stage);
        controller.setTableUtils(tableUtils);
        stage.show();
    }

    public static void newQueueStage() {
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/newQueue.fxml"));
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getPrefWidth());
        stage.setMinHeight(root.getPrefHeight());
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("New Queue");
        NewQueueController controller = loader.getController();
        controller.setStage(stage);
        stage.showAndWait();
    }

}

