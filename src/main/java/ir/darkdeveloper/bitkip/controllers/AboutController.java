package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.BitKip;
import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.utils.MoreUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements FXMLController {


    @FXML
    private VBox parent;
    @FXML
    private ImageView logoImg;
    @FXML
    private Label versionLbl;
    @FXML
    private Button updateBtn;

    private Stage stage;
//    private List<Label> labels;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
//        labels = FxUtils.getAllNodes(parent, Label.class);
        var image = new Image(BitKip.getResource("icons/logo.png").toExternalForm());
        logoImg.setImage(image);
        versionLbl.setText("v" + AppConfigs.VERSION);
//        updateTheme(Configs.getTheme());
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

//    @Override
//    public void updateTheme(String theme) {
//        if (Configs.getTheme().equals("light")) {
//            parent.setBackground(Background.fill(Paint.valueOf("#fff")));
//            labels.forEach(label -> label.setTextFill(Paint.valueOf("#222")));
//        } else {
//            parent.setBackground(Background.fill(Paint.valueOf("#333")));
//            labels.forEach(label -> label.setTextFill(Paint.valueOf("#fff")));
//        }
//        FxUtils.updateButtonTheme(List.of(updateBtn));
//    }

    @FXML
    private void checkForUpdates() {
        MoreUtils.checkUpdates(true);
    }

}
