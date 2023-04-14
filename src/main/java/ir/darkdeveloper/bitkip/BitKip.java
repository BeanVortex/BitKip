package ir.darkdeveloper.bitkip;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.repo.ScheduleRepo;
import ir.darkdeveloper.bitkip.task.ScheduleTask;
import ir.darkdeveloper.bitkip.utils.*;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.ArrayList;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentDownloadings;
import static ir.darkdeveloper.bitkip.config.AppConfigs.startedQueues;

public class BitKip extends Application {

    // todo bean for selected queue in side

    @Override
    public void start(Stage stage) {
        DownloadsRepo.createTable();
        ScheduleRepo.createSchedulesTable();
        QueuesRepo.createTable();
        var queues = QueuesRepo.getAllQueues(false, true);
        if (queues == null)
            queues = QueuesRepo.createDefaultRecords();
        queues = ScheduleRepo.createDefaultSchedulesForQueues(queues);
        AppConfigs.addAllQueues(queues);
        IOUtils.createSaveLocations();
        FxUtils.switchSceneToMain(stage);
        ScheduleTask.startSchedules();
        AppConfigs.setHostServices(getHostServices());
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(true);
        ResizeUtil.addResizeListener(stage);
        MoreUtils.checkUpdates(false);
        stage.show();
    }


    @Override
    public void stop() {
        var notObservedDms = new ArrayList<>(currentDownloadings);
        notObservedDms.forEach(dm -> dm.getDownloadTask().pause());
        startedQueues.clear();
    }


    public static void main(String[] args) {
        launch();
    }

    public static URL getResource(String path) {
        return BitKip.class.getResource(path);
    }
}