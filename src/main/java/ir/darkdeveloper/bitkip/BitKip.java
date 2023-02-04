package ir.darkdeveloper.bitkip;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.ResizeUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

public class BitKip extends Application {

    @Override
    public void start(Stage stage) {
        FxUtils.switchSceneToMain(stage, "main.fxml");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setMinHeight(515);
        stage.setMinWidth(853);
        stage.setResizable(true);
        ResizeUtil.addResizeListener(stage);
//        var logoPath = getResource("images/logo.png");
//        if (logoPath != null)
//            stage.getIcons().add(new Image(logoPath.toExternalForm()));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public static URL getResource(String path) {
        return BitKip.class.getResource(path);
    }
}