package ir.darkdeveloper.bitkip;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.ResizeUtil;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

public class BitKip extends Application {

    // todo: limited download in chunks
    // todo: scrollview in new download
    // todo: sync db file size and actual file size
    // todo: downloading stage
    // todo: ask user if also wants to delete the file itself
    // todo: queue scheduler
    // todo: menu items' actions

    @Override
    public void start(Stage stage) {
        IOUtils.createSaveLocations();
        DownloadsRepo.createTable();
        QueuesRepo.createTableAndDefaultRecords();
        FxUtils.switchSceneToMain(stage, "main.fxml");
        AppConfigs.setHostServices(getHostServices());
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setMinHeight(515);
        stage.setMinWidth(883);
        stage.setResizable(true);
        ResizeUtil.addResizeListener(stage);
        stage.show();
    }


    @Override
    public void stop() {
        AppConfigs.downloadTaskList.forEach(Task::cancel);
        AppConfigs.currentDownloading.forEach(DownloadsRepo::updateDownloadProgress);
    }


    public static void main(String[] args) {
        launch();
    }

    public static URL getResource(String path) {
        return BitKip.class.getResource(path);
    }
}