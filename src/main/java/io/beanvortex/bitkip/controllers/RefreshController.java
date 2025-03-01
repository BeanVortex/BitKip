package io.beanvortex.bitkip.controllers;

import io.beanvortex.bitkip.controllers.interfaces.FXMLController;
import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import io.beanvortex.bitkip.utils.DownloadOpUtils;
import io.beanvortex.bitkip.utils.DownloadUtils;
import io.beanvortex.bitkip.utils.Validations;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static io.beanvortex.bitkip.config.AppConfigs.log;

public class RefreshController implements FXMLController {

    @FXML
    private Label errorLabel,nameLbl;
    @FXML
    private Button saveBtn,resumeBtn;
    @FXML
    private TextField urlField;

    private Stage stage;
    private DownloadModel dm;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }


    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
        urlField.textProperty().addListener((o, ol, n) -> {
            errorLabel.setVisible(false);
            saveBtn.setDisable(false);
            resumeBtn.setDisable(false);
            if (n.equals(dm.getUri()))
                DownloadUtils.disableControlsAndShowError("Same URL", errorLabel, saveBtn, resumeBtn);
            if (n.isBlank())
                DownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, saveBtn, resumeBtn);
        });
    }


    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    public void setDownload(DownloadModel dm) {
        this.dm = dm;
        setDownloadData();
    }

    private void setDownloadData() {
        nameLbl.setText("Name: " + dm.getName());
        urlField.setText(dm.getUri());
        Validations.prepareLinkFromClipboard(urlField);
    }

    @Override
    public Stage getStage() {
        return stage;
    }


    @FXML
    private void onSave() {
        saveToDB();
        stage.close();
    }

    @FXML
    private void onResume() {
        saveToDB();
        DownloadOpUtils.resumeDownloads(List.of(dm), 0, 0);
        stage.close();
    }

    private void saveToDB() {
        var url = urlField.getText();
        dm.setUri(url);
        DownloadsRepo.updateDownloadProperty(DownloadsRepo.COL_URL, url, dm.getId());
        log.info("URL refreshed for : " + dm.getName());
    }
}
