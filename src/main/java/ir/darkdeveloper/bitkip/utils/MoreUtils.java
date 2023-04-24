package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.BitKip;
import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.task.UpdateCheckTask;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.util.concurrent.Executors;

public class MoreUtils {

    public static void checkUpdates(boolean showNoUpdatesNotification) {
        var updateChecker = new UpdateCheckTask();
        var executor = Executors.newCachedThreadPool();
        updateChecker.setExecutor(executor);
        executor.submit(updateChecker);

        updateChecker.valueProperty().addListener((obs, old, newVal) -> {
            var version = newVal.version();
            var description = newVal.description();
            System.out.println(newVal.assets());
            if (!AppConfigs.VERSION.equals(version)) {
                var alert = new Alert(Alert.AlertType.INFORMATION,
                        "", ButtonType.YES, ButtonType.NO);
                var alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                var logoPath = BitKip.getResource("icons/logo.png");
                if (logoPath != null)
                    alertStage.getIcons().add(new Image(logoPath.toExternalForm()));
                alert.setTitle("Update Notifier");
                alert.setHeaderText("New update available: " + version);
                alert.setContentText(description + "\nWould you like to download the new update?");
                var buttonType = alert.showAndWait();
                buttonType.ifPresent(type -> {
                    if (type == ButtonType.YES)
                        AppConfigs.hostServices
                                .showDocument("https://github.com/DarkDeveloper-arch/BitKip/releases/tag/v" + version);
                    else if (type == ButtonType.NO)
                        alert.close();
                });
            } else {
                if (showNoUpdatesNotification)
                    Notifications.create()
                            .title("Checked for updates")
                            .text("No updates available")
                            .showInformation();
            }
        });
    }

}
