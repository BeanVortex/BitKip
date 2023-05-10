package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.task.UpdateCheckTask;
import org.controlsfx.control.Notifications;

import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;

public class MoreUtils {

    public static void checkUpdates(boolean showNoUpdatesNotification) {
        var updateChecker = new UpdateCheckTask();
        var executor = Executors.newCachedThreadPool();
        updateChecker.setExecutor(executor);
        executor.submit(updateChecker);

        updateChecker.valueProperty().addListener((obs, old, newVal) -> {
            var version = newVal.version();
            if (!AppConfigs.VERSION.equals(version)) {
                log.info("New update available: " + version);
                FxUtils.showUpdateDialog(newVal);
            }
            else if (showNoUpdatesNotification)
                Notifications.create()
                        .title("Checked for updates")
                        .text("No updates available")
                        .showInformation();

        });
    }

}
