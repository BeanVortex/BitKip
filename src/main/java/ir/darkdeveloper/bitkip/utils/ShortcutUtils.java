package ir.darkdeveloper.bitkip.utils;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

public class ShortcutUtils {

    public static final KeyCodeCombination NEW_DOWNLOAD_KEY = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
    public static final KeyCodeCombination DOWNLOADING_STAGE_KEY = new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN);
    public static final KeyCodeCombination NEW_BATCH_KEY = new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN);
    public static final KeyCodeCombination SETTINGS_KEY = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
    public static final KeyCodeCombination QUIT_KEY = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);
    public static final KeyCodeCombination RESUME_KEY = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);
    public static final KeyCodeCombination PAUSE_KEY = new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN);
    public static final KeyCodeCombination RESTART_KEY = new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN);
    public static final KeyCodeCombination DELETE_FILE_KEY = new KeyCodeCombination(KeyCode.DELETE, KeyCombination.CONTROL_DOWN);
    public static final KeyCodeCombination DELETE_KEY = new KeyCodeCombination(KeyCode.DELETE);
    public static final KeyCodeCombination NEW_QUEUE_KEY = new KeyCodeCombination(KeyCode.N, KeyCombination.SHIFT_DOWN);
    public static final KeyCodeCombination START_QUEUE_KEY = new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN);
    public static final KeyCodeCombination ADD_QUEUE_KEY = new KeyCodeCombination(KeyCode.A, KeyCombination.SHIFT_DOWN);
    public static final KeyCodeCombination STOP_QUEUE_KEY = new KeyCodeCombination(KeyCode.D, KeyCombination.SHIFT_DOWN);


    public static void initMainTableShortcut(MainTableUtils mainTableUtils, Stage stage) {

        Runnable downloadingStage = () -> mainTableUtils.getSelected()
                .forEach(dm -> FxUtils.newDownloadingStage(dm, mainTableUtils));

        Runnable newDownload = () -> DownloadOpUtils.newDownload(mainTableUtils, true);
        Runnable newBatchDownload = () -> DownloadOpUtils.newDownload(mainTableUtils, false);
        Runnable resume = () -> DownloadOpUtils.resumeDownloads(mainTableUtils,
                mainTableUtils.getSelected(), null, null);
        Runnable pause = () -> DownloadOpUtils.pauseDownloads(mainTableUtils);
        Runnable delete = () -> DownloadOpUtils.deleteDownloads(mainTableUtils, false);
        Runnable shiftDelete = () -> DownloadOpUtils.deleteDownloads(mainTableUtils, true);

        Runnable settings = () -> {
            //
            System.out.println("settings");
        };


        Runnable restart = () -> {
            System.out.println("restart");
//            var selectedItems = mainTableUtils.getSelected();
//            selectedItems.forEach(DownloadOpUtils::pauseDownload);
        };

        Runnable addToQueue = () -> {
            //
            System.out.println("add to queue");
        };
        Runnable startQueue = () -> {
            //
            System.out.println("start to queue");
        };

        Runnable stopQueue = () -> {
            //
            System.out.println("stop to queue");
        };


        stage.getScene().getAccelerators().put(DOWNLOADING_STAGE_KEY, downloadingStage);
        stage.getScene().getAccelerators().put(NEW_DOWNLOAD_KEY, newDownload);
        stage.getScene().getAccelerators().put(NEW_BATCH_KEY, newBatchDownload);
        stage.getScene().getAccelerators().put(SETTINGS_KEY, settings);
        stage.getScene().getAccelerators().put(QUIT_KEY, Platform::exit);
        stage.getScene().getAccelerators().put(RESUME_KEY, resume);
        stage.getScene().getAccelerators().put(RESTART_KEY, restart);
        stage.getScene().getAccelerators().put(PAUSE_KEY, pause);
        stage.getScene().getAccelerators().put(DELETE_FILE_KEY, shiftDelete);
        stage.getScene().getAccelerators().put(DELETE_KEY, delete);
        stage.getScene().getAccelerators().put(NEW_QUEUE_KEY, FxUtils::newQueueStage);
        stage.getScene().getAccelerators().put(ADD_QUEUE_KEY, addToQueue);
        stage.getScene().getAccelerators().put(START_QUEUE_KEY, startQueue);
        stage.getScene().getAccelerators().put(STOP_QUEUE_KEY, stopQueue);
    }
}
