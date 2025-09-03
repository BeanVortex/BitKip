package io.beanvortex.bitkip.controllers;

import io.beanvortex.bitkip.BitKip;
import io.beanvortex.bitkip.controllers.interfaces.FXMLController;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import io.beanvortex.bitkip.utils.IOUtils;
import io.beanvortex.bitkip.utils.MoreUtils;
import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.config.observers.ThemeObserver;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements FXMLController, ThemeObserver {

    @FXML
    private Text patchText;
    @FXML
    private ImageView logoImg;
    @FXML
    private Label versionLbl, downloadedLbl;

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        var image = new Image(BitKip.getResource("icons/logo.png").toExternalForm());
        logoImg.setImage(image);
        versionLbl.setText("v" + AppConfigs.VERSION);
        downloadedLbl.setText("Downloaded: " + IOUtils.formatBytes(DownloadsRepo.sumDownloaded()));
        initPatchNote();
    }

    private void initPatchNote() {
        patchText.setText(IOUtils.readUpdateDescription());
    }

    @Override
    public void initAfterStage() {
        updateTheme(getStage().getScene());
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