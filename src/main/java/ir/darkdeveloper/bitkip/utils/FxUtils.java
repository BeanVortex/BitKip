package ir.darkdeveloper.bitkip.utils;


import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.*;
import ir.darkdeveloper.bitkip.models.DownloadModel;
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
import static ir.darkdeveloper.bitkip.config.AppConfigs.openDownloadings;

public class FxUtils {


    public static void switchSceneToMain(Stage stage) {
        try {
            var loader = new FXMLLoader(getResource("fxml/main.fxml"));
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

    public static void newDownloadStage(MainTableUtils mainTableUtils, boolean isSingle) {
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/newDownload.fxml"));
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(AppConfigs.newDownloadMinWidth);
        stage.setMinHeight(AppConfigs.newDownloadMinHeight);
        stage.setWidth(root.getPrefWidth());
        stage.setHeight(root.getPrefHeight());
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

    public static void newDownloadingStage(DownloadModel dm, MainTableUtils mainTableUtils) {
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/downloading.fxml"));
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
        var end = dm.getName().length();
        if (end > 60)
            end = 60;
        stage.setTitle(dm.getName().substring(0, end));
        DownloadingController controller = loader.getController();
        controller.setStage(stage);
        openDownloadings.add(controller);
        controller.setDownloadModel(dm);
        controller.setMainTableUtils(mainTableUtils);
        stage.show();
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

