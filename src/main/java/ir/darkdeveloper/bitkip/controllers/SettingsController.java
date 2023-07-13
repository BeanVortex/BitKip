package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.task.FileMoveTask;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.Validations;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.config.observers.QueueSubject.getQueueSubject;
import static ir.darkdeveloper.bitkip.config.observers.ThemeSubject.getThemeSubject;
import static ir.darkdeveloper.bitkip.config.observers.ThemeSubject.setTheme;

public class SettingsController implements FXMLController {

    @FXML
    private CheckBox immediateCheck, triggerOffCheck, agentCheck, addDownCheck,
            continueCheck, completeDialogCheck, serverCheck;
    @FXML
    private VBox root, actionArea, queueContainer;
    @FXML
    private TabPane tabPane;
    @FXML
    private Label agentDesc, lblLocation, savedLabel;
    @FXML
    private TextField agentField, connectionField, readField, rateLimitField, retryField, portField;


    private Stage stage;
    private QueueSetting queueController;
    private VBox queueRoot;

    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
        initQueues();
        stage.setTitle("Settings");
        stage.setResizable(true);
        stage.setOnCloseRequest(e -> {
            getThemeSubject().removeObserver(this);
            getQueueSubject().removeObserver(queueController);
        });
        var defaultWidth = stage.getMinWidth() + 50;
        var defaultHeight = stage.getMinHeight() + 50;
        tabPane.getSelectionModel().selectedIndexProperty().addListener((ob, o, n) -> {
            // if queue tab is selected
            if (n.intValue() == 2) {
                root.getChildren().remove(actionArea);
                var width = queueRoot.getPrefWidth() + 100;
                var height = queueRoot.getPrefHeight() + 100;
                stage.setWidth(width);
                stage.setHeight(height);
                stage.setMinWidth(width);
                stage.setMinHeight(height);
            } else if (!root.getChildren().contains(actionArea)) {
                root.getChildren().add(actionArea);
                stage.setWidth(defaultWidth);
                stage.setHeight(defaultHeight);
                stage.setMinWidth(defaultWidth);
                stage.setMinHeight(defaultHeight);
                stage.setTitle("Settings");
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Validations.validateIntInputCheck(portField, (long) serverPort);
        Validations.validateIntInputCheck(retryField, (long) downloadRetryCount);
        Validations.validateIntInputCheck(rateLimitField, (long) downloadRateLimitCount);
        Validations.validateIntInputCheck(connectionField, (long) connectionTimeout);
        Validations.validateIntInputCheck(readField, (long) readTimeout);
        agentDesc.setText("Note: If you enter wrong agent, your downloads may not start. Your agent will update when you use extension");
        initElements();
    }

    private void initElements() {
        lblLocation.setText(downloadPath);
        serverCheck.setSelected(serverEnabled);
        triggerOffCheck.setSelected(triggerTurnOffOnEmptyQueue);
        immediateCheck.setSelected(downloadImmediately);
        addDownCheck.setSelected(addSameDownload);
        portField.setText(String.valueOf(serverPort));
        retryField.setText(String.valueOf(downloadRetryCount));
        rateLimitField.setText(String.valueOf(downloadRateLimitCount));
        completeDialogCheck.setSelected(showCompleteDialog);
        continueCheck.setSelected(continueOnLostConnectionLost);
        retryField.setDisable(continueOnLostConnectionLost);
        rateLimitField.setDisable(continueOnLostConnectionLost);
        agentField.setText(userAgent);
        agentCheck.setSelected(userAgentEnabled);
        agentField.setDisable(!userAgentEnabled);
        connectionField.setText(String.valueOf(connectionTimeout));
        readField.setText(String.valueOf(readTimeout));
    }

    private void initQueues() {
        try {
            var loader = new FXMLLoader(getResource("fxml/queueSetting.fxml"));
            queueRoot = loader.load();
            queueController = loader.getController();
            queueController.setStage(stage);
            queueContainer.getChildren().add(queueRoot);
            getQueueSubject().addObserver(queueController);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
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

    @FXML
    private void changeSaveDir(ActionEvent e) {
        var dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select download location");
        dirChooser.setInitialDirectory(new File(downloadPath));
        var selectedDir = dirChooser.showDialog(FxUtils.getStageFromEvent(e));
        if (selectedDir != null) {
            var prevDownloadPath = downloadPath;
            downloadPath = selectedDir.getPath() + File.separator + "BitKip" + File.separator;
            IOUtils.createSaveLocations();
            IOUtils.saveConfigs();
            AppConfigs.initPaths();
            lblLocation.setText(downloadPath);
            var header = "Move downloaded files and folders?";
            var content = "Would you also like to move download files and folders to the new location?" +
                    " This might take some time to move files, some downloads that are saved outside BitKip folders, might not be accessed through the app";
            if (FxUtils.askWarning(header, content)) {
                try {
                    var size = IOUtils.getFolderSize(prevDownloadPath);
                    var executor = Executors.newCachedThreadPool();
                    var fileMoveTask = new FileMoveTask(prevDownloadPath, downloadPath, size, executor);
                    executor.submit(fileMoveTask);
                    FxUtils.fileTransferDialog(fileMoveTask);
                } catch (IOException ex) {
                    log.error("Failed to move files and folders: " + ex.getMessage());
                    Notifications.create()
                            .title("Failed to move")
                            .text("Failed to move files and folders")
                            .showError();
                }
            }

        }
    }

    @FXML
    private void onThemeChange(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() != MouseButton.PRIMARY)
            return;
        if (theme.equals("light")) setTheme("dark");
        else setTheme("light");
        IOUtils.saveConfigs();
    }

    @FXML
    private void onServerCheck() {
        serverEnabled = serverCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    private void onCompleteDialogCheck() {
        showCompleteDialog = completeDialogCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    public void onSave() {
        serverPort = Integer.parseInt(portField.getText());
        downloadRetryCount = Integer.parseInt(retryField.getText());
        downloadRateLimitCount = Integer.parseInt(rateLimitField.getText());
        readTimeout = Integer.parseInt(readField.getText());
        connectionTimeout = Integer.parseInt(connectionField.getText());
        userAgent = agentField.getText();
        IOUtils.saveConfigs();
        showSavedMessage();
    }

    private void showSavedMessage() {
        var executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Platform.runLater(() -> savedLabel.setText("Successfully Saved"));
                savedLabel.setTextFill(Paint.valueOf("#009688"));
                savedLabel.setVisible(true);
                Thread.sleep(2500);
                savedLabel.setVisible(false);
            } catch (InterruptedException ignore) {
            }
            executor.shutdown();
        });
    }

    @FXML
    private void onContinueCheck() {
        continueOnLostConnectionLost = continueCheck.isSelected();
        IOUtils.saveConfigs();
        retryField.setDisable(continueOnLostConnectionLost);
        rateLimitField.setDisable(continueOnLostConnectionLost);
    }

    @FXML
    private void onDefaults() {
        theme = defaultTheme;
        setTheme(theme);
        serverEnabled = defaultServerEnabled;
        serverPort = defaultServerPort;
        showCompleteDialog = defaultShowCompleteDialog;
        triggerTurnOffOnEmptyQueue = defaultTriggerTurnOffOnEmptyQueue;
        continueOnLostConnectionLost = defaultContinueOnLostConnectionLost;
        downloadRetryCount = defaultDownloadRetryCount;
        downloadRateLimitCount = defaultDownloadRateLimitCount;
        downloadImmediately = defaultDownloadImmediately;
        userAgent = defaultUserAgent;
        userAgentEnabled = defaultUserAgentEnabled;
        connectionTimeout = defaultConnectionTimeout;
        readTimeout = defaultReadTimeout;
        IOUtils.saveConfigs();
        initElements();
        showSavedMessage();
    }

    @FXML
    private void onAgentCheck() {
        userAgentEnabled = agentCheck.isSelected();
        IOUtils.saveConfigs();
        agentField.setDisable(!userAgentEnabled);
    }

    @FXML
    private void onTurnOffCheck() {
        triggerTurnOffOnEmptyQueue = triggerOffCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    private void onImmediateCheck() {
        downloadImmediately = immediateCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    private void onAddDownCheck() {
        addSameDownload = addDownCheck.isSelected();
        IOUtils.saveConfigs();
    }

    public void setQueue(QueueModel selectedQueue) {
        tabPane.getSelectionModel().select(2);
        queueController.setSelectedQueue(selectedQueue);
    }
}
