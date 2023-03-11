package ir.darkdeveloper.bitkip.utils;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.util.ArrayList;

public class ShortcutUtils {

    public static final KeyCodeCombination SHIFT_R = new KeyCodeCombination(KeyCode.R, KeyCombination.SHIFT_DOWN);
    public static final KeyCodeCombination SHIFT_P = new KeyCodeCombination(KeyCode.P, KeyCombination.SHIFT_DOWN);
    public static final KeyCodeCombination SHIFT_DEL = new KeyCodeCombination(KeyCode.DELETE, KeyCombination.SHIFT_DOWN);
    public static final KeyCodeCombination DEL = new KeyCodeCombination(KeyCode.DELETE);

    public static void initMainTableShortcut(MainTableUtils mainTableUtils, Stage stage) {
        Runnable resume = () -> {
            var selectedItems = mainTableUtils.getSelected();
            selectedItems.forEach(dm -> DownloadOpUtils.resumeDownload(dm, mainTableUtils::refreshTable, mainTableUtils));
        };

        Runnable pause = () -> {
            var selectedItems = mainTableUtils.getSelected();
            selectedItems.forEach(DownloadOpUtils::pauseDownload);
        };

        Runnable delete = () -> {
            var selectedItems = mainTableUtils.getSelected();
            var notObservedDms = new ArrayList<>(selectedItems);
            notObservedDms.forEach(DownloadOpUtils::deleteDownloadRecord);
            mainTableUtils.remove(selectedItems);
        };
        Runnable shiftDelete = () -> {
            var selectedItems = mainTableUtils.getSelected();
            var notObservedDms = new ArrayList<>(selectedItems);
            notObservedDms.forEach(dm -> {
                DownloadOpUtils.deleteDownloadRecord(dm);
                IOUtils.deleteDownload(dm);
            });
            mainTableUtils.remove(selectedItems);
        };
        stage.getScene().getAccelerators().put(SHIFT_R, resume);
        stage.getScene().getAccelerators().put(SHIFT_P, pause);
        stage.getScene().getAccelerators().put(SHIFT_DEL, shiftDelete);
        stage.getScene().getAccelerators().put(DEL, delete);
    }
}
