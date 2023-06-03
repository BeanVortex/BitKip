package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import static ir.darkdeveloper.bitkip.config.AppConfigs.downloadPath;

public class SettingsController implements FXMLController {

    @FXML
    private Label lblLocation;
    @FXML
    private Button btnChangeDir;
    @FXML
    private Circle circleTheme;
    @FXML
    private Line line1;
    @FXML
    private CheckBox serverCheck;
    @FXML
    private TextField portField;
    @FXML
    private VBox parent;
    @FXML
    private CheckBox newFileCheck;


    private Stage stage;


    @Override
    public void initAfterStage() {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

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
            lblLocation.setText(downloadPath);
            var header = "Move downloaded files?";
            var content = "Would you also like to move download files to the new location?";
            if (FxUtils.askWarning(header, content)){
                IOUtils.moveAndDeletePreviousData(prevDownloadPath, downloadPath);
                BooksRepo.updateBooksPath(Configs.getSaveLocation());
            }

        }
    }
}
