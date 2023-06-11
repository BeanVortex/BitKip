package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.task.FileMoveTask;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.Validations;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.config.observers.ThemeSubject.setTheme;

public class SettingsController implements FXMLController {


    @FXML
    private VBox parent;
    @FXML
    private CheckBox completeDialogCheck;
    @FXML
    private Label lblLocation;
    @FXML
    private Line line1;
    @FXML
    private Line line2;
    @FXML
    private Line line3;
    @FXML
    private CheckBox serverCheck;
    @FXML
    private TextField portField;
    @FXML
    private TextField retryField;
    @FXML
    private TextField rateLimitField;
    @FXML
    private Label savedLabel;


    private Stage stage;


    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
        parent.prefWidthProperty().bind(stage.widthProperty());
        parent.widthProperty().addListener((ob, o, n) -> {
            var endX = n.doubleValue() - 20;
            line1.setEndX(endX);
            line2.setEndX(endX);
            line3.setEndX(endX);
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Validations.validateIntInputCheck(portField, (long) serverPort);
        Validations.validateIntInputCheck(retryField, (long) downloadRetryCount);
        Validations.validateIntInputCheck(rateLimitField, (long) downloadRateLimitCount);
        lblLocation.setText(downloadPath);
        serverCheck.setSelected(serverEnabled);
        portField.setText(String.valueOf(serverPort));
        retryField.setText(String.valueOf(downloadRetryCount));
        rateLimitField.setText(String.valueOf(downloadRateLimitCount));
        completeDialogCheck.setSelected(showCompleteDialog);
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
    public void onFieldSave() {
        serverPort = Integer.parseInt(portField.getText());
        downloadRetryCount = Integer.parseInt(retryField.getText());
        downloadRateLimitCount = Integer.parseInt(rateLimitField.getText());
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

}
