package ir.darkdeveloper.bitkip.utils;


import ir.darkdeveloper.bitkip.controllers.*;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.TurnOffMode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.sun.jna.Platform.isLinux;
import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.staticQueueNames;

public class FxUtils {

    // these constants are used to prevent these stages to show more than 1
    private static final String QUEUE_SETTING_STAGE = "QueueSetting";
    private static final String ABOUT_STAGE = "About";
    private static final Map<String, Stage> openStages = new LinkedHashMap<>();


    public static void startMainStage(Stage stage) {
        try {
            var loader = new FXMLLoader(getResource("fxml/main.fxml"));
            VBox root = loader.load();
            var scene = new Scene(root, stage.getWidth(), stage.getHeight());
            MainController controller = loader.getController();
            getQueueSubject().addObserver(controller);
            stage.setScene(scene);
            stage.setMinWidth(root.getMinWidth());
            stage.setMinHeight(root.getMinHeight());
            controller.setStage(stage);
            stage.setTitle("BitKip");
            stage.setOnCloseRequest(e -> {
                if (isLinux()) {
                    Platform.exit();
                    System.exit(0);
                } else stage.hide();
            });
            stage.show();
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

    public static void newDownloadStage(boolean isSingle) {
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
        stage.setMinWidth(root.getMinWidth());
        stage.setMinHeight(root.getMinHeight());
        stage.setWidth(root.getPrefWidth());
        stage.setHeight(root.getPrefHeight());
        stage.setTitle("New Download");
        NewDownload controller = loader.getController();
        controller.setStage(stage);
        controller.setIsSingle(isSingle);
        // onclose request has set in controller
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
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getMinWidth());
        stage.setMinHeight(root.getMinHeight());
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
        stage.setMinWidth(root.getMinWidth());
        stage.setMinHeight(root.getMinHeight());
        stage.setTitle("About");
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        FXMLController controller = loader.getController();
        controller.setStage(stage);
        stage.setOnCloseRequest(e -> openStages.remove(ABOUT_STAGE));
        stage.show();
        openStages.put(ABOUT_STAGE, stage);
    }

    public static void newShuttingDownStage(TurnOffMode turnOffMode) {
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/shuttingDown.fxml"));
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getMinWidth());
        stage.setMinHeight(root.getMinHeight());
        stage.setTitle("Shutting down");
        ShuttingDownController controller = loader.getController();
        controller.setStage(stage);
        controller.setTurnOffMode(turnOffMode);
        stage.showAndWait();
    }


    public static void newDownloadingStage(DownloadModel dm) {
        FXMLLoader loader;
        var stage = new Stage();
        ScrollPane root;
        try {
            loader = new FXMLLoader(getResource("fxml/downloading.fxml"));
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getMinWidth());
        stage.setMinHeight(root.getMinHeight());
        var end = dm.getName().length();
        if (end > 60)
            end = 60;
        stage.setTitle(dm.getName().substring(0, end));
        DownloadingController controller = loader.getController();
        controller.setStage(stage);
        openDownloadings.add(controller);
        controller.setDownloadModel(dm);
        stage.setOnCloseRequest(e -> openDownloadings.remove(controller));
        stage.show();
    }

    public static void newBatchListStage(List<LinkModel> links) {
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
        stage.setTitle("Links");
        BatchList controller = loader.getController();
        controller.setStage(stage);
        controller.setData(links);
        stage.show();
    }

    public static void newQueueSettingStage(QueueModel selectedQueue) {
        if (openStages.containsKey(QUEUE_SETTING_STAGE)) {
            openStages.get(QUEUE_SETTING_STAGE).toFront();
            return;
        }
        FXMLLoader loader;
        var stage = new Stage();
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
        var queueName = ALL_DOWNLOADS_QUEUE;
        if (selectedQueue != null)
            queueName = selectedQueue.getName();
        stage.setTitle("Scheduler: %s".formatted(queueName));
        QueueSetting controller = loader.getController();
        controller.setStage(stage);
        controller.setSelectedQueue(selectedQueue);
        getQueueSubject().addObserver(controller);
        stage.setOnCloseRequest(e -> {
            openStages.remove(QUEUE_SETTING_STAGE);
            getQueueSubject().removeObserver(controller);
        });
        stage.show();
        openStages.put(QUEUE_SETTING_STAGE, stage);
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
        var stage = (Stage) alert.getDialogPane().getScene().getWindow();
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        var res = alert.showAndWait();
        return res.orElse(no) == yes;
    }

    public static boolean askWarning(String header, String content) {
        var yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        var no = new ButtonType("No", ButtonBar.ButtonData.NO);
        return askWarning(header, content, yes, no);

    }

    public static boolean askWarning(String header, String content, ButtonType primary, ButtonType secondary) {
        var alert = new Alert(Alert.AlertType.WARNING, content, primary, secondary);
        alert.setTitle("Warning");
        alert.setHeaderText(header);
        var stage = (Stage) alert.getDialogPane().getScene().getWindow();
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        var res = alert.showAndWait();
        return res.orElse(secondary) == primary;
    }

    public static void newRefreshStage(DownloadModel dm) {
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/refreshLink.fxml"));
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getPrefWidth());
        stage.setMinHeight(root.getPrefHeight());
        stage.setTitle("Refreshing Link");
        RefreshController controller = loader.getController();
        controller.setStage(stage);
        controller.setDownload(dm);
        stage.showAndWait();
    }

    public static boolean askForPassword(String header, String content) {
        var dialog = new Dialog<String>();
        dialog.setTitle("Set Password");
        dialog.setHeaderText(header);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        var pwd = new PasswordField();
        pwd.setPromptText("System password");
        var contentLabel = new Label();
        contentLabel.setText(content);
        contentLabel.setWrapText(true);
        var container = new VBox();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle("-fx-padding: 10");
        container.setSpacing(10);
        container.getChildren().addAll(contentLabel, pwd);
        dialog.getDialogPane().setContent(container);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return pwd.getText();
            }
            return null;
        });

        var result = dialog.showAndWait();
        if (result.isPresent()) {
            userPassword = result.get();
            return true;
        }
        return false;
    }
}

