package io.beanvortex.bitkip.controllers;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import io.beanvortex.bitkip.BitKip;
import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.config.observers.QueueSubject;
import io.beanvortex.bitkip.config.observers.ThemeSubject;
import io.beanvortex.bitkip.controllers.interfaces.FXMLController;
import io.beanvortex.bitkip.exceptions.DeniedException;
import io.beanvortex.bitkip.models.QueueModel;
import io.beanvortex.bitkip.task.FileMoveTask;
import io.beanvortex.bitkip.utils.FxUtils;
import io.beanvortex.bitkip.utils.IOUtils;
import io.beanvortex.bitkip.utils.Validations;
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

public class SettingsController implements FXMLController {

    @FXML
    private CheckBox immediateCheck, startupCheck, triggerOffCheck, agentCheck, addDownCheck,
            continueCheck, completeDialogCheck, errorNotificationCheck, startFastQueueCheck, trustAllServersCheck, serverCheck, lessCpuCheck;
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
            ThemeSubject.getThemeSubject().removeObserver(this);
            QueueSubject.getQueueSubject().removeObserver(queueController);
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
        Validations.validateIntInputCheck(portField, (long) AppConfigs.serverPort, 0, null);
        Validations.validateIntInputCheck(retryField, (long) AppConfigs.downloadRetryCount, 1, null);
        Validations.validateIntInputCheck(rateLimitField, (long) AppConfigs.downloadRateLimitCount, 1, null);
        Validations.validateIntInputCheck(connectionField, (long) AppConfigs.connectionTimeout, 0, null);
        Validations.validateIntInputCheck(readField, (long) AppConfigs.readTimeout, 0, null);
        agentDesc.setText("Note: If you enter wrong agent, your downloads may not start. Your agent will update when you use extension");
        initElements();
    }

    private void initElements() {
        lblLocation.setText(AppConfigs.downloadPath);
        serverCheck.setSelected(AppConfigs.serverEnabled);
        startupCheck.setSelected(AppConfigs.startup);
        triggerOffCheck.setSelected(AppConfigs.triggerTurnOffOnEmptyQueue);
        immediateCheck.setSelected(AppConfigs.downloadImmediately);
        addDownCheck.setSelected(AppConfigs.addSameDownload);
        lessCpuCheck.setSelected(AppConfigs.lessCpuIntensive);
        portField.setText(String.valueOf(AppConfigs.serverPort));
        retryField.setText(String.valueOf(AppConfigs.downloadRetryCount));
        rateLimitField.setText(String.valueOf(AppConfigs.downloadRateLimitCount));
        completeDialogCheck.setSelected(AppConfigs.showCompleteDialog);
        errorNotificationCheck.setSelected(AppConfigs.showErrorNotifications);
        startFastQueueCheck.setSelected(AppConfigs.startFastQueue);
        trustAllServersCheck.setSelected(AppConfigs.trustAllServers);
        continueCheck.setSelected(AppConfigs.continueOnLostConnectionLost);
        retryField.setDisable(AppConfigs.continueOnLostConnectionLost);
        rateLimitField.setDisable(AppConfigs.continueOnLostConnectionLost);
        agentField.setText(AppConfigs.userAgent);
        agentCheck.setSelected(AppConfigs.userAgentEnabled);
        agentField.setDisable(!AppConfigs.userAgentEnabled);
        connectionField.setText(String.valueOf(AppConfigs.connectionTimeout));
        readField.setText(String.valueOf(AppConfigs.readTimeout));
    }

    private void initQueues() {
        try {
            var loader = new FXMLLoader(BitKip.getResource("fxml/queueSetting.fxml"));
            queueRoot = loader.load();
            queueController = loader.getController();
            queueController.setStage(stage);
            queueContainer.getChildren().add(queueRoot);
            QueueSubject.getQueueSubject().addObserver(queueController);
        } catch (Exception e) {
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
        dirChooser.setInitialDirectory(new File(AppConfigs.downloadPath));
        var selectedDir = dirChooser.showDialog(FxUtils.getStageFromEvent(e));
        if (selectedDir != null) {
            var prevDownloadPath = AppConfigs.downloadPath;
            AppConfigs.downloadPath = selectedDir.getPath() + File.separator + "BitKip" + File.separator;
            IOUtils.createSaveLocations();
            IOUtils.saveConfigs();
            AppConfigs.initPaths();
            lblLocation.setText(AppConfigs.downloadPath);
            var header = "Move downloaded files and folders?";
            var content = "Would you also like to move download files and folders to the new location?" +
                    " This might take some time to move files, some downloads that are saved outside BitKip folders, might not be accessed through the app";
            if (FxUtils.askWarning(header, content)) {
                try {
                    var size = IOUtils.getFolderSize(prevDownloadPath);
                    var executor = Executors.newCachedThreadPool();
                    var fileMoveTask = new FileMoveTask(prevDownloadPath, AppConfigs.downloadPath, size, executor);
                    executor.submit(fileMoveTask);
                    FxUtils.fileTransferDialog(fileMoveTask);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

        }
    }

    @FXML
    private void onThemeChange(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() != MouseButton.PRIMARY)
            return;
        if (AppConfigs.theme.equals("light")) ThemeSubject.setTheme("dark");
        else ThemeSubject.setTheme("light");
        IOUtils.saveConfigs();
    }

    @FXML
    private void onServerCheck() {
        AppConfigs.serverEnabled = serverCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    private void onCompleteDialogCheck() {
        AppConfigs.showCompleteDialog = completeDialogCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    private void onErrorNotificationCheck() {
        AppConfigs.showErrorNotifications = errorNotificationCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    private void onStartFastQueueCheck() {
        AppConfigs.startFastQueue = startFastQueueCheck.isSelected();
        IOUtils.saveConfigs();
    }
    @FXML
    private void onTrustAllServersCheck() {
        AppConfigs.trustAllServers = trustAllServersCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    public void onSave() {
        AppConfigs.serverPort = Integer.parseInt(portField.getText());
        AppConfigs.downloadRetryCount = Integer.parseInt(retryField.getText());
        AppConfigs.downloadRateLimitCount = Integer.parseInt(rateLimitField.getText());
        AppConfigs.readTimeout = Integer.parseInt(readField.getText());
        AppConfigs.connectionTimeout = Integer.parseInt(connectionField.getText());
        AppConfigs.userAgent = agentField.getText();
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
        AppConfigs.continueOnLostConnectionLost = continueCheck.isSelected();
        IOUtils.saveConfigs();
        retryField.setDisable(AppConfigs.continueOnLostConnectionLost);
        rateLimitField.setDisable(AppConfigs.continueOnLostConnectionLost);
    }

    @FXML
    private void onDefaults() {
        AppConfigs.theme = AppConfigs.defaultTheme;
        ThemeSubject.setTheme(AppConfigs.theme);
        AppConfigs.serverEnabled = AppConfigs.defaultServerEnabled;
        AppConfigs.serverPort = AppConfigs.defaultServerPort;
        AppConfigs.showCompleteDialog = AppConfigs.defaultShowCompleteDialog;
        AppConfigs.showErrorNotifications = AppConfigs.defaultShowErrorNotifications;
        AppConfigs.triggerTurnOffOnEmptyQueue = AppConfigs.defaultTriggerTurnOffOnEmptyQueue;
        AppConfigs.continueOnLostConnectionLost = AppConfigs.defaultContinueOnLostConnectionLost;
        AppConfigs.downloadRetryCount = AppConfigs.defaultDownloadRetryCount;
        AppConfigs.downloadRateLimitCount = AppConfigs.defaultDownloadRateLimitCount;
        AppConfigs.downloadImmediately = AppConfigs.defaultDownloadImmediately;
        AppConfigs.userAgent = AppConfigs.defaultUserAgent;
        AppConfigs.userAgentEnabled = AppConfigs.defaultUserAgentEnabled;
        AppConfigs.connectionTimeout = AppConfigs.defaultConnectionTimeout;
        AppConfigs.readTimeout = AppConfigs.defaultReadTimeout;
        AppConfigs.lessCpuIntensive = AppConfigs.defaultLessCpuIntensive;
        IOUtils.saveConfigs();
        initElements();
        showSavedMessage();
    }

    @FXML
    private void onAgentCheck() {
        AppConfigs.userAgentEnabled = agentCheck.isSelected();
        IOUtils.saveConfigs();
        agentField.setDisable(!AppConfigs.userAgentEnabled);
    }

    @FXML
    private void onTurnOffCheck() {
        AppConfigs.triggerTurnOffOnEmptyQueue = triggerOffCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    private void onImmediateCheck() {
        AppConfigs.downloadImmediately = immediateCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    private void onAddDownCheck() {
        AppConfigs.addSameDownload = addDownCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    private void onLessCpuCheck() {
        AppConfigs.lessCpuIntensive = lessCpuCheck.isSelected();
        IOUtils.saveConfigs();
    }

    @FXML
    private void onStartupCheck() {
        AppConfigs.startup = startupCheck.isSelected();
        initStartup();
    }

    private void initStartup() {
        try {
            if (AppConfigs.startup && !existsOnStartup())
                addToStartup();
            else
                removeFromStartup();
            IOUtils.saveConfigs();
        } catch (DeniedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addToStartup() throws DeniedException {
        if (com.sun.jna.Platform.isWindows()) {
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER,
                    "Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "BitKip",
                    System.getProperty("user.dir") + "\\BitKip.exe");
        } else if (com.sun.jna.Platform.isLinux()) {
            var userHome = System.getProperty("user.home");
            var configPath = userHome + File.separator + ".config" + File.separator + "autostart" + File.separator;
            var path = configPath + "BitKip.d";
            var changedNamePath = configPath + "BitKip.desktop";
            var of = Path.of(changedNamePath);

            try {
                if (!new File(path).exists())
                    Files.copy(Path.of("/usr/share/applications/BitKip.desktop"), of, StandardCopyOption.REPLACE_EXISTING);
                else
                    Files.move(Path.of(path), of, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new DeniedException("Failed to add BitKip to startup");
            }
        }
    }

    public static boolean existsOnStartup() {
        if (com.sun.jna.Platform.isWindows())
            return Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER,
                    "Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "BitKip");
        else if (com.sun.jna.Platform.isLinux()) {
            var userHome = System.getProperty("user.home");
            var path = userHome + File.separator + ".config" + File.separator + "autostart" + File.separator + "BitKip.desktop";
            return new File(path).exists();
        }
        return false;
    }

    public static void removeFromStartup() throws DeniedException {
        if (com.sun.jna.Platform.isWindows()) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER,
                    "Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "BitKip");
        } else if (com.sun.jna.Platform.isLinux()) {
            var userHome = System.getProperty("user.home");
            var path = userHome + File.separator + ".config" + File.separator + "autostart" + File.separator + "BitKip.desktop";
            var changedNamePath = userHome + File.separator + ".config" + File.separator + "autostart" + File.separator + "BitKip.d";
            try {
                Files.move(Path.of(path), Path.of(changedNamePath), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new DeniedException("Failed to remove BitKip from startup");
            }
        }
    }

    public void setQueue(QueueModel selectedQueue) {
        tabPane.getSelectionModel().select(2);
        queueController.setSelectedQueue(selectedQueue);
    }
}
