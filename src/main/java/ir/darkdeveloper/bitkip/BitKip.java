package ir.darkdeveloper.bitkip;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.ResizeUtil;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

public class BitKip extends Application {

    // todo: limited download in chunks
    // todo: remaining time
    // todo: scrollview in new download
    // todo: save table data on db before termination of app
    // todo: save download in db after pause
    // todo: pause and resume operations
    // todo: downloading stage
    // todo: ask user if also wants to delete the file itself
    // todo: bug in downloading limited bytes

    @Override
    public void start(Stage stage) {
        FxUtils.switchSceneToMain(stage, "main.fxml");
        AppConfigs.setHostServices(getHostServices());
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setMinHeight(515);
        stage.setMinWidth(883);
        stage.setResizable(true);
        ResizeUtil.addResizeListener(stage);
        IOUtils.createSaveLocations();
        DownloadsRepo.createTable();
        QueuesRepo.createTableAndDefaultRecords();
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