package ir.darkdeveloper.bitkip.utils;


import ir.darkdeveloper.bitkip.controllers.*;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.staticQueueNames;

public class FxUtils {

    // these constants are used to prevent these stages to show more than 1
    public static final String SCHEDULER_STAGE = "Scheduler";
    public static final String ABOUT_STAGE = "About";
    public static final Map<String, Stage> openStages = new LinkedHashMap<>();


    public static void switchSceneToMain(Stage stage) {
        try {
            var loader = new FXMLLoader(getResource("fxml/main.fxml"));
            Parent root = loader.load();
            var scene = new Scene(root, stage.getWidth(), stage.getHeight());
            MainController controller = loader.getController();
            getQueueSubject().addObserver(controller);
            stage.setScene(scene);
            stage.setMinWidth(mainMinWidth);
            stage.setMinHeight(mainMinHeight);
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
        stage.setMinWidth(newDownloadMinWidth);
        stage.setMinHeight(newDownloadMinHeight);
        stage.setWidth(root.getPrefWidth());
        stage.setHeight(root.getPrefHeight());
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("New Download");
        NewDownload controller = loader.getController();
        controller.setStage(stage);
        controller.setMainTableUtils(mainTableUtils);
        controller.setIsSingle(isSingle);
        getQueueSubject().addObserver(controller);
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

    public static void newAboutStage() {
        if (openStages.containsKey(ABOUT_STAGE)) {
            openStages.get(ABOUT_STAGE).toFront();
            return;
        }
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/about.fxml"));
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getPrefWidth());
        stage.setMinHeight(root.getPrefHeight());
//        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("About");
        var logoPath = getResource("images/logo.png");
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        FXMLController controller = loader.getController();
        controller.setStage(stage);
        stage.setOnCloseRequest(e -> openStages.remove(ABOUT_STAGE));
        stage.show();
        openStages.put(ABOUT_STAGE, stage);
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

    public static void newQueueSettingStage(QueueModel selectedQueue) {
        if (openStages.containsKey(SCHEDULER_STAGE)) {
            openStages.get(SCHEDULER_STAGE).toFront();
            return;
        }
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/queueSetting.fxml"));
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
        stage.setTitle("Scheduler: %s".formatted(selectedQueue.getName()));
        QueueSetting controller = loader.getController();
        controller.setStage(stage);
        controller.setSelectedQueue(selectedQueue);
        getQueueSubject().addObserver(controller);
        stage.setOnCloseRequest(e -> openStages.remove(SCHEDULER_STAGE));
        stage.show();
        openStages.put(SCHEDULER_STAGE, stage);
    }


    public static boolean askToMoveFiles(List<DownloadModel> downloads, QueueModel desQueue) {
        var downloadsHasFolder = downloads.stream().filter(dm ->
                !dm.getQueues().stream().filter(QueueModel::hasFolder).toList().isEmpty()
        ).toList();

        if (downloadsHasFolder.isEmpty()) {
            if (desQueue != null) {
                if (!desQueue.hasFolder())
                    return false;

                if (!desQueue.hasFolder() && !staticQueueNames.contains(desQueue.getName()))
                    return false;
            } else return false;
        }

        var yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        var no = new ButtonType("No", ButtonBar.ButtonData.NO);
        var alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Would you also like to move download files to the new location?", yes, no);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Move files?");
        var res = alert.showAndWait();
        return res.orElse(no) == yes;
    }
}

