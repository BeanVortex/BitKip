package io.beanvortex.bitkip.utils;

import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.task.ExtensionInstallTask;
import io.beanvortex.bitkip.task.UpdateCheckTask;
import javafx.application.Platform;
import org.controlsfx.control.Notifications;

import java.io.IOException;
import java.util.concurrent.Executors;

import static io.beanvortex.bitkip.config.AppConfigs.log;

public class MoreUtils {

    public static void checkUpdates(boolean showNoUpdatesNotification) {
        var updateChecker = new UpdateCheckTask();
        var executor = Executors.newCachedThreadPool();
        updateChecker.setExecutor(executor);
        updateChecker.valueProperty().addListener((obs, old, newVal) -> {
            var version = newVal.version();
            if (!AppConfigs.VERSION.equals(version)) {
                log.info("New update available: {}", version);
                IOUtils.writeUpdateDescription(newVal.description());
                FxUtils.showUpdateDialog(newVal);
            }
        });
        updateChecker.setOnCancelled(e -> {
            if (showNoUpdatesNotification)
                Platform.runLater(() -> Notifications.create()
                        .title("Checked for updates")
                        .text("No updates available")
                        .showInformation());
        });
        updateChecker.exceptionProperty().addListener((ob, o, ex) -> {
            if (ex instanceof IOException && showNoUpdatesNotification)
                Platform.runLater(() -> Notifications.create()
                        .title("Failed to check updates")
                        .text("Could not connect to source, check your connection")
                        .showWarning());

        });
        executor.submit(updateChecker);

    }

    public static void downloadExtension() {
        var exTask = new ExtensionInstallTask();
        var executor = Executors.newCachedThreadPool();
        exTask.setExecutor(executor);
        exTask.exceptionProperty().addListener((ob, o, ex) -> {
            if (ex instanceof IOException)
                Platform.runLater(() -> Notifications.create()
                        .title("Failed to install extension")
                        .text("Could not connect to source, check your connection")
                        .showWarning());

        });
        executor.submit(exTask);
    }
}
