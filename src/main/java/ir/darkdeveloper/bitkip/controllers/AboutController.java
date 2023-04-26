package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.BitKip;
import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.utils.MoreUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements FXMLController {


    @FXML
    private ImageView logoImg;
    @FXML
    private Label versionLbl;

    private Stage stage;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var image = new Image(BitKip.getResource("icons/logo.png").toExternalForm());
        logoImg.setImage(image);
        versionLbl.setText("v" + AppConfigs.VERSION);
    }

    @Override
    public void initAfterStage() {

    }


    @FXML
    private void openGithubPage(ActionEvent e) {
        var hyperlink = (Hyperlink) e.getSource();
        AppConfigs.hostServices.showDocument(hyperlink.getText());
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
    private void checkForUpdates() {
        MoreUtils.checkUpdates(true);
    }

}
