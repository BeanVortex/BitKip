package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.task.FileMoveTask;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.InputValidations;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.config.observers.ThemeSubject.setTheme;

public class SettingsController implements FXMLController {

    @FXML
    private CheckBox completeDialogCheck;
    @FXML
    private Label lblLocation;
    @FXML
    private Circle circleTheme;
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
    private VBox parent;

    private Stage stage;
    private List<Label> labels;


    @Override
    public void initAfterStage() {
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
        labels = FxUtils.getAllNodes(parent, Label.class);
        InputValidations.validIntInputCheck(portField, (long) serverPort);
        lblLocation.setText(downloadPath);
        serverCheck.setSelected(serverEnabled);
        portField.setText(String.valueOf(serverPort));
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
        if (theme.equals("light")) {
            circleTheme.setFill(Paint.valueOf("#fff"));
            circleTheme.setStroke(Paint.valueOf("#333"));
            completeDialogCheck.setTextFill(Paint.valueOf("#fff"));
            serverCheck.setTextFill(Paint.valueOf("#fff"));
            parent.setBackground(Background.fill(Paint.valueOf("#333")));
            labels.forEach(label -> label.setTextFill(Paint.valueOf("#fff")));
            setTheme("dark");
        } else {
            circleTheme.setFill(Paint.valueOf("#333"));
            circleTheme.setStroke(Paint.valueOf("#fff"));
            completeDialogCheck.setTextFill(Paint.valueOf("#333"));
            serverCheck.setTextFill(Paint.valueOf("#333"));
            parent.setBackground(Background.fill(Paint.valueOf("#fff")));
            labels.forEach(label -> label.setTextFill(Paint.valueOf("#111")));
            setTheme("light");
        }
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
    private void onPortSave() {
        serverPort = Integer.parseInt(portField.getText());
        IOUtils.saveConfigs();
    }
}
