package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.utils.DownloadOpUtils;
import ir.darkdeveloper.bitkip.utils.InputValidations;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static ir.darkdeveloper.bitkip.repo.DownloadsRepo.COL_URL;

public class RefreshController implements FXMLController {

    @FXML
    private Label errorLabel;
    @FXML
    private Button saveBtn;
    @FXML
    private Button resumeBtn;
    @FXML
    private Label nameLbl;
    @FXML
    private TextField urlField;

    private Stage stage;
    private DownloadModel dm;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }


    @Override
    public void initAfterStage() {
        urlField.textProperty().addListener((o, ol, n) -> {
            errorLabel.setVisible(false);
            saveBtn.setDisable(false);
            resumeBtn.setDisable(false);
            if (n.equals(dm.getUrl()))
                NewDownloadUtils.disableControlsAndShowError("Same URL", errorLabel, saveBtn, resumeBtn);
            if (n.isBlank())
                NewDownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, saveBtn, resumeBtn);
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
        urlField.setText(dm.getUrl());
        InputValidations.prepareLinkFromClipboard(urlField);
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
        DownloadOpUtils.resumeDownloads(List.of(dm), null, null);
        stage.close();
    }

    private void saveToDB() {
        var url = urlField.getText();
        dm.setUrl(url);
        DownloadsRepo.updateDownloadProperty(COL_URL, url, dm.getId());
    }
}
