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
    // todo: scrollview in new download
    // todo: sync db file size and actual file size
    // todo: downloading stage
    // todo: double click a download in table and open Downloading stage
    // todo: queue scheduler
    // todo: menu items' actions
    // todo: delete files with chunks
    // todo: retry if connection is lost in a period
    // todo: refactor db codes
    // todo: chunk download more than one
    // todo: bug in dates in table
    // todo: download in chunks: check if host supports range byte

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
        AppConfigs.currentDownloading.forEach(dm -> dm.getDownloadTask().pause());
        AppConfigs.currentDownloading.forEach(DownloadsRepo::updateDownloadProgress);
    }


    public static void main(String[] args) {
        launch();
    }

    public static URL getResource(String path) {
        return BitKip.class.getResource(path);
    }
}