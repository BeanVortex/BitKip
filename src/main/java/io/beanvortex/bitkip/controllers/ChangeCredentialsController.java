package io.beanvortex.bitkip.controllers;

import io.beanvortex.bitkip.controllers.interfaces.FXMLController;
import io.beanvortex.bitkip.models.Credentials;
import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ChangeCredentialsController implements FXMLController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    private Stage stage;
    private ObservableList<DownloadModel> dms;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }


    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
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
    private void onSave() {
        var credentials = new Credentials(usernameField.getText(), passwordField.getText());
        dms.forEach(dm -> dm.setCredentials(credentials));
        DownloadsRepo.updateDownloadsCredential(dms);
        stage.close();
    }

    @FXML
    private void onCancel() {
        stage.close();
    }

    public void setDownloads(ObservableList<DownloadModel> dms) {
        this.dms = dms;
    }
}
