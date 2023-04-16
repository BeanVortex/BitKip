package ir.darkdeveloper.bitkip.utils;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public class ShortcutUtils {

    public static final KeyCodeCombination NEW_DOWNLOAD_KEY = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
    public static final KeyCodeCombination OPEN_KEY = new KeyCodeCombination(KeyCode.ENTER);
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

}
