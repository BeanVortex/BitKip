package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.getQueueSubject;

public class NewDownload implements FXMLController {

    @FXML
    private Button singleButton;
    @FXML
    private Button batchButton;

    private Stage stage;
    private QueueObserver prevController;
    private boolean isSingle = true;

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setOnCloseRequest(e -> getQueueSubject().removeObserver(prevController));
        initAfterStage();
    }

    public void setIsSingle(boolean isSingle) {
        this.isSingle = isSingle;
        if (isSingle)
            switchToSingleDownload();
        else
            switchToMultipleDownload();
    }

    @Override
    public void initAfterStage() {
        stage.widthProperty().addListener((ob, o, n) -> {
            var width = n.longValue();
            var buttonWidth = width / 2;
            batchButton.setPrefWidth(buttonWidth);
            singleButton.setPrefWidth(buttonWidth);
        });

        var logoPath = getResource("icons/logo.png");
        if (logoPath != null) {
            var img = new Image(logoPath.toExternalForm());
            stage.getIcons().add(img);
        }

        var buttonWidth = stage.getWidth() / 2;
        singleButton.setPrefWidth(buttonWidth);
        batchButton.setPrefWidth(buttonWidth);

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }


    @FXML
    private void showSingle() {
        if (!isSingle)
            switchToSingleDownload();
    }

    @FXML
    private void showBatch() {
        if (isSingle)
            switchToMultipleDownload();
    }

    private void switchToSingleDownload() {
        isSingle = true;
        singleButton.getStyleClass().add("tab_btn_selected");
        batchButton.getStyleClass().remove("tab_btn_selected");
        switchDownloadDetails("singleDownload");
    }

    private void switchToMultipleDownload() {
        isSingle = false;
        batchButton.getStyleClass().add("tab_btn_selected");
        singleButton.getStyleClass().remove("tab_btn_selected");
        switchDownloadDetails("batchDownload");
    }

    private void switchDownloadDetails(String fxmlName) {
        try {
            var loader = new FXMLLoader(getResource("fxml/" + fxmlName + ".fxml"));
            Parent details = loader.load();
            var root = (VBox) stage.getScene().getRoot();
            var rootChildren = root.getChildren();
            rootChildren.removeIf(node -> {
                if (node.getId() == null)
                    return false;
                return node.getId().equals("download_details");
            });
            QueueObserver controller = loader.getController();
            if (prevController != null)
                getQueueSubject().removeObserver(prevController);

            var box = new VBox();
            box.getChildren().add(details);
            box.setId("download_details");
            controller.setStage(stage);
            getQueueSubject().addObserver(controller);
            prevController = controller;
            rootChildren.add(box);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
