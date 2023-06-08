package ir.darkdeveloper.bitkip.utils;


import ir.darkdeveloper.bitkip.config.observers.ThemeObserver;
import ir.darkdeveloper.bitkip.controllers.*;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.*;
import ir.darkdeveloper.bitkip.task.FileMoveTask;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.config.observers.QueueSubject.getQueueSubject;
import static ir.darkdeveloper.bitkip.config.observers.ThemeSubject.getThemeSubject;
import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.Defaults.staticQueueNames;

public class FxUtils {

    // these constants are used to prevent these stages to show more than 1
    private static final String QUEUE_SETTING_STAGE = "QueueSetting";
    private static final String ABOUT_STAGE = "About";
    private static final String LOGS_STAGE = "Log";
    private static final Map<String, Stage> openStages = new LinkedHashMap<>();


    public static void startMainStage(Stage stage) {
        try {
            var loader = new FXMLLoader(getResource("fxml/main.fxml"));
            VBox root = loader.load();
            var scene = new Scene(root, stage.getWidth(), stage.getHeight());
            MainController controller = loader.getController();
            getQueueSubject().addObserver(controller);
            getThemeSubject().addObserver(controller, scene);
            stage.setScene(scene);
            stage.setMinWidth(root.getMinWidth());
            stage.setMinHeight(root.getMinHeight());
            controller.setStage(stage);
            stage.setTitle("BitKip");
            stage.show();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }


    public static Stage getStageFromEvent(ActionEvent e) {
        if (e.getSource() instanceof Node n
                && n.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    public static void newDownloadStage(boolean isSingle, SingleURLModel urlModel) {
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/newDownload.fxml"));
            root = loader.load();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getMinWidth());
        stage.setMinHeight(root.getMinHeight());
        stage.setWidth(root.getPrefWidth());
        stage.setHeight(root.getPrefHeight());
        stage.setTitle("New Download");
        NewDownloadController controller = loader.getController();
        controller.setStage(stage);
        getThemeSubject().addObserver(controller, scene);
        controller.setUrlModel(urlModel);
        controller.setIsSingle(isSingle);
        // onclose request has set in controller
        stage.show();
        stage.setAlwaysOnTop(true);
        stage.setAlwaysOnTop(false);
    }

    public static void newQueueStage() {
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/newQueue.fxml"));
            root = loader.load();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
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
        FXMLController controller = loader.getController();
        getThemeSubject().addObserver(controller, scene);
        controller.setStage(stage);
        stage.setOnCloseRequest(e -> getThemeSubject().removeObserver(controller));
        stage.showAndWait();
    }

    public static void newAboutStage() {
        newOnceStageWindow(ABOUT_STAGE, "fxml/about.fxml", "About");
    }

    public static void newLogsStage() {
        newOnceStageWindow(LOGS_STAGE, "fxml/logs.fxml", "Logs");
    }

    public static void newOnceStageWindow(String key, String resource, String title) {
        if (openStages.containsKey(key)) {
            openStages.get(key).toFront();
            return;
        }
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource(resource));
            root = loader.load();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getMinWidth());
        stage.setMinHeight(root.getMinHeight());
        stage.setTitle(title);
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        FXMLController controller = loader.getController();
        getThemeSubject().addObserver(controller, scene);
        controller.setStage(stage);
        stage.setOnCloseRequest(e -> {
            openStages.remove(key);
            getThemeSubject().removeObserver(controller);
        });
        stage.show();
        openStages.put(key, stage);
    }

    public static void newSettingsStage() {
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/settings.fxml"));
            root = loader.load();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getMinWidth());
        stage.setMinHeight(root.getMinHeight());
        stage.setTitle("Settings");
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        FXMLController controller = loader.getController();
        getThemeSubject().addObserver(controller, scene);
        controller.setStage(stage);
        stage.setOnCloseRequest(e -> getThemeSubject().removeObserver(controller));
        stage.show();
    }

    public static void newDownloadingStage(DownloadModel dm) {
        FXMLLoader loader;
        var stage = new Stage();
        ScrollPane root;
        try {
            loader = new FXMLLoader(getResource("fxml/downloading.fxml"));
            root = loader.load();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
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
        getThemeSubject().addObserver(controller, scene);
        controller.setDownloadModel(dm);
        stage.setOnCloseRequest(e -> {
            openDownloadings.remove(controller);
            getThemeSubject().removeObserver(controller);
        });
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
            log.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getPrefWidth());
        stage.setMinHeight(root.getPrefHeight());
        stage.setTitle("Links");
        BatchList controller = loader.getController();
        getThemeSubject().addObserver(controller, scene);
        controller.setStage(stage);
        controller.setData(links);
        stage.setOnCloseRequest(e -> getThemeSubject().removeObserver(controller));
        stage.show();
        stage.setAlwaysOnTop(true);
        stage.setAlwaysOnTop(false);
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
            log.error(e.getLocalizedMessage());
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
        getThemeSubject().addObserver(controller, scene);
        stage.setOnCloseRequest(e -> {
            openStages.remove(QUEUE_SETTING_STAGE);
            getQueueSubject().removeObserver(controller);
            getThemeSubject().removeObserver(controller);
        });
        stage.show();
        stage.setAlwaysOnTop(true);
        stage.setAlwaysOnTop(false);
        openStages.put(QUEUE_SETTING_STAGE, stage);
    }

    public static void newRefreshStage(DownloadModel dm) {
        FXMLLoader loader;
        Stage stage = new Stage();
        VBox root;
        try {
            loader = new FXMLLoader(getResource("fxml/refreshLink.fxml"));
            root = loader.load();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        var scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(root.getPrefWidth());
        stage.setMinHeight(root.getPrefHeight());
        stage.setTitle("Refreshing URL");
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        RefreshController controller = loader.getController();
        controller.setStage(stage);
        getThemeSubject().addObserver(controller, scene);
        stage.setOnCloseRequest(e -> getThemeSubject().removeObserver(controller));
        controller.setDownload(dm);
        stage.showAndWait();
    }

    public static boolean askToMoveFilesForQueues(List<DownloadModel> downloads, QueueModel desQueue) {
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
        return showAlert(yes, no, alert);
    }



    public static boolean askWarning(String header, String content) {
        var yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        var no = new ButtonType("No", ButtonBar.ButtonData.NO);
        return askWarning(header, content, yes, no);
    }

    public static boolean askWarning(String header, String content, ButtonType primary, ButtonType secondary) {
        return askWithDialog(Alert.AlertType.WARNING, header, content, primary, secondary);
    }

    public static boolean askWithDialog(Alert.AlertType alertType, String header, String content,
                                        ButtonType primary, ButtonType secondary) {
        Alert alert;
        if (secondary == null)
            alert = new Alert(alertType, content, primary);
        else
            alert = new Alert(alertType, content, primary, secondary);
        alert.setTitle(alertType.name());
        alert.setHeaderText(header);
        return showAlert(primary, secondary, alert);
    }
    private static boolean showAlert(ButtonType yes, ButtonType no, Alert alert) {
        var stage = (Stage) alert.getDialogPane().getScene().getWindow();
        ThemeObserver.updateThemeNotObserved(stage.getScene());
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        var res = alert.showAndWait();
        return res.orElse(no) == yes;
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
        container.setStyle("-fx-padding: 10");
        container.setAlignment(Pos.CENTER_LEFT);
        container.setSpacing(10);
        container.getChildren().addAll(contentLabel, pwd);
        dialog.getDialogPane().setContent(container);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (pwd.getText().isBlank())
                    return null;
                return pwd.getText();
            }
            return null;
        });

        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(new Image(logoPath.toExternalForm()));
        var result = dialog.showAndWait();
        if (result.isPresent()) {
            userPassword = result.get();
            log.info("Password set");
            return true;
        }
        return false;
    }

    public static boolean askForShutdown(TurnOffMode turnOffMode) {
        var dialog = new Dialog<ButtonType>();
        dialog.setTitle("Your pc is about to " + turnOffMode);
        dialog.setHeaderText("Queue is done or finished and scheduled to " + turnOffMode);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        var countDownLbl = new Label();
        var service = Executors.newCachedThreadPool();
        service.submit(() -> {
            try {
                for (int i = 10; i >= 0; i--) {
                    var finalI = i;
                    Platform.runLater(() -> countDownLbl.setText(String.valueOf(finalI)));
                    Thread.sleep(1000);
                }
                Platform.runLater(() -> {
                    dialog.setResult(ButtonType.APPLY);
                    dialog.close();
                });
            } catch (InterruptedException ignore) {
            }
        });
        dialog.setOnCloseRequest(e -> service.shutdownNow());
        var container = new VBox();
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-padding: 10");
        container.setSpacing(10);
        countDownLbl.setTextFill(Paint.valueOf("#F44336"));
        container.setStyle("-fx-font-size: 18; -fx-font-weight: bold");
        container.getChildren().addAll(countDownLbl);
        dialog.getDialogPane().setContent(container);

        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(new Image(logoPath.toExternalForm()));
        var result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.APPLY;
    }

    public static void showUpdateDialog(UpdateModel updateModel) {
        var dialog = new Dialog<String>();
        dialog.setTitle("New Update Available: " + updateModel.version());
        dialog.setHeaderText(updateModel.description().header());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

        var nodes = new ArrayList<Node>();
        nodes.add(new Label(String.join("\n", updateModel.description().features())));
        updateModel.assets().forEach(asset -> {
            var link = new Hyperlink();
            if (asset.size().isBlank())
                link.setText(asset.title());
            else
                link.setText(asset.title() + " / " + asset.size());
            link.setOnAction(e -> {
                setClipboard(asset.link());
                newDownloadStage(true, null);
                dialog.close();
            });
            nodes.add(link);
        });

        var container = new VBox();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle("-fx-padding: 10");
        container.setSpacing(10);
        container.getChildren().addAll(nodes);
        dialog.getDialogPane().setContent(container);
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(new Image(logoPath.toExternalForm()));
        dialog.showAndWait();
    }

    public static void setClipboard(String value) {
        var clip = Clipboard.getSystemClipboard();
        var content = new ClipboardContent();
        content.putString(value);
        clip.setContent(content);
        log.info("Clipboard set : " + content);
    }

    public static void fileTransferDialog(FileMoveTask fileMoveTask) {
        var dialog = new Dialog<Boolean>();
        dialog.setTitle("Moving files");
        dialog.setHeaderText("Moving files...");
        var progressBar = new ProgressBar();
        progressBar.setPrefWidth(180);
        var contentLabel = new Label();
        contentLabel.setText("Moving files from \n" + fileMoveTask.getPrevPath() + "\nto\n" + fileMoveTask.getNextPath());
        var container = new VBox();
        contentLabel.setWrapText(true);
        container.setStyle("-fx-padding: 10");
        container.setPrefWidth(200);
        container.setSpacing(10);
        container.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().addAll(contentLabel, progressBar);
        dialog.getDialogPane().setContent(container);

        var logoPath = getResource("icons/logo.png");
        var stage = ((Stage) dialog.getDialogPane().getScene().getWindow());
        if (logoPath != null)
            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        ThemeObserver.updateThemeNotObserved(stage.getScene());
        fileMoveTask.progressProperty().addListener((o, ol, n) -> {
            progressBar.setProgress(n.doubleValue());
            if (n.doubleValue() == 1) {
                dialog.setResult(true);
                dialog.hide();
            }
        });
        dialog.show();
    }

    public static boolean showFailedToStart(String header, String content) {
        var dialog = new Dialog<String>();
        dialog.setTitle("Failed to start server");
        dialog.setHeaderText(header);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        var portField = new TextField();
        InputValidations.validIntInputCheck(portField, (long) serverPort);
        portField.setText(String.valueOf(serverPort));
        var contentLabel = new Label();
        contentLabel.setText(content);
        contentLabel.setWrapText(true);
        var container = new VBox();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle("-fx-padding: 10");
        container.setSpacing(10);
        container.getChildren().addAll(contentLabel, portField);
        dialog.getDialogPane().setContent(container);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.APPLY) {
                if (portField.getText().isBlank())
                    return null;
                return portField.getText();
            }
            return null;
        });

        var logoPath = getResource("icons/logo.png");
        if (logoPath != null)
            ((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(new Image(logoPath.toExternalForm()));
        var result = dialog.showAndWait();
        if (result.isPresent()) {
            var port = result.get();
            serverPort = Integer.parseInt(port);
            IOUtils.saveConfigs();
            log.info("Port set to: " + port);
        }
        return true;
    }
}