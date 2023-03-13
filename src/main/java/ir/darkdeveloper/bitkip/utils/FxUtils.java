package ir.darkdeveloper.bitkip.utils;


import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.BatchList;
import ir.darkdeveloper.bitkip.controllers.MainController;
import ir.darkdeveloper.bitkip.controllers.NewDownload;
import ir.darkdeveloper.bitkip.controllers.NewQueueController;
import ir.darkdeveloper.bitkip.models.LinkModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.List;

import static ir.darkdeveloper.bitkip.BitKip.getResource;

public class FxUtils {


    public static void switchSceneToMain(Stage stage, String fxmlFilename) {
        try {
            var loader = new FXMLLoader(getResource("fxml/" + fxmlFilename));
            Parent root = loader.load();
            var scene = new Scene(root, stage.getWidth(), stage.getHeight());
            MainController controller = loader.getController();
            AppConfigs.getQueueSubject().addObserver(controller);
            stage.setScene(scene);
            stage.setMinWidth(AppConfigs.mainMinWidth);
            stage.setMinHeight(AppConfigs.mainMinHeight);
            controller.setStage(stage);
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

    public static void newDownloadStage(String fxmlFilename, MainTableUtils mainTableUtils, boolean isSingle) {
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
        stage.setMinWidth(AppConfigs.newDownloadMinWidth);
        stage.setMinHeight(AppConfigs.newDownloadMinHeight);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("New Download");
        NewDownload controller = loader.getController();
        controller.setStage(stage);
        controller.setMainTableUtils(mainTableUtils);
        controller.setIsSingle(isSingle);
        AppConfigs.getQueueSubject().addObserver(controller);
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
        AppConfigs.getQueueSubject().addObserver(controller);
        stage.showAndWait();
    }

    public static void newBatchListStage(List<LinkModel> links, MainTableUtils mainTableUtils) {
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/batchList.fxml"));
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
        stage.setTitle("Links");
        BatchList controller = loader.getController();
        controller.setStage(stage);
        controller.setData(links);
        controller.setMainTableUtils(mainTableUtils);
        stage.show();
    }

}

