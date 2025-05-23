package io.beanvortex.bitkip.controllers;

import io.beanvortex.bitkip.BitKip;
import io.beanvortex.bitkip.controllers.interfaces.FXMLController;
import io.beanvortex.bitkip.config.observers.QueueObserver;
import io.beanvortex.bitkip.models.SingleURLModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static io.beanvortex.bitkip.config.AppConfigs.log;
import static io.beanvortex.bitkip.config.observers.QueueSubject.getQueueSubject;
import static io.beanvortex.bitkip.config.observers.ThemeSubject.getThemeSubject;

public class NewDownloadController implements FXMLController {

    @FXML
    private ScrollPane scroll;
    @FXML
    private VBox container;
    @FXML
    private Button singleButton, batchButton;

    private Stage stage;
    private QueueObserver prevController;
    private boolean isSingle = true;
    private SingleURLModel urlModel;

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setOnCloseRequest(e -> {
            getQueueSubject().removeObserver(prevController);
            getThemeSubject().removeObserver(this);
        });
        initAfterStage();
    }

    public void setIsSingle(boolean isSingle) {
        this.isSingle = isSingle;
        if (isSingle)
            switchToSingleDownload();
        else
            switchToMultipleDownload();
    }

    public void setUrlModel(SingleURLModel urlModel) {
        this.urlModel = urlModel;
    }

    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
        stage.widthProperty().addListener((ob, o, n) -> {
            var width = n.longValue();
            var buttonWidth = width / 2;
            batchButton.setPrefWidth(buttonWidth);
            singleButton.setPrefWidth(buttonWidth);
            var diff = scroll.getPrefWidth() - container.getPrefWidth();
            scroll.setPrefWidth(n.doubleValue());
            container.setPrefWidth(n.doubleValue() - diff);
        });

        scroll.prefHeightProperty().bind(stage.heightProperty());

        var logoPath = BitKip.getResource("icons/logo.png");
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
        singleButton.getStyleClass().add("selected_tab");
        batchButton.getStyleClass().remove("selected_tab");
        switchDownloadDetails("singleDownload");
    }

    private void switchToMultipleDownload() {
        isSingle = false;
        batchButton.getStyleClass().add("selected_tab");
        singleButton.getStyleClass().remove("selected_tab");
        switchDownloadDetails("batchDownload");
    }

    private void switchDownloadDetails(String fxmlName) {
        try {
            var loader = new FXMLLoader(BitKip.getResource("fxml/" + fxmlName + ".fxml"));
            VBox details = loader.load();
            container.getChildren().removeIf(node -> {
                if (node.getId() == null)
                    return false;
                return node.getId().equals("download_details");
            });

            QueueObserver controller = loader.getController();
            if (fxmlName.equals("singleDownload"))
                ((SingleDownload)controller).setUrlModel(urlModel);

            if (prevController != null)
                getQueueSubject().removeObserver(prevController);



            details.setId("download_details");
            controller.setStage(stage);
            container.getChildren().add(details);
            getQueueSubject().addObserver(controller);
            prevController = controller;
        } catch (IOException e) {
            log.error(e.toString());
            throw new RuntimeException(e);
        }
    }

}
