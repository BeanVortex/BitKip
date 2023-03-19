package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.task.DownloadTask;
import ir.darkdeveloper.bitkip.utils.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.List;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;

public class DownloadingController implements FXMLController {

    @FXML
    private ToggleSwitch openSwitch;
    @FXML
    private ToggleSwitch showSwitch;
    @FXML
    private Accordion accordion;
    @FXML
    private TitledPane advancedPane;
    @FXML
    private ImageView logoImg;
    @FXML
    private Label titleLbl;
    @FXML
    private Label progressLbl;
    @FXML
    private ProgressBar downloadProgress;
    @FXML
    private Label nameLbl;
    @FXML
    private Label queueLbl;
    @FXML
    private Label statusLbl;
    @FXML
    private Label speedLbl;
    @FXML
    private Label downloadedOfLbl;
    @FXML
    private Label remainingLbl;
    @FXML
    private Button controlBtn;
    @FXML
    private HBox toolbar;
    @FXML
    private Button hideBtn;
    @FXML
    private Button closeBtn;

    private Stage stage;
    private DownloadModel downloadModel;
    private Rectangle2D bounds;

    private final BooleanProperty isPaused = new SimpleBooleanProperty(true);
    private boolean isComplete = false;
    private MainTableUtils mainTableUtils;


    @Override
    public void initAfterStage() {
        stage.widthProperty().addListener((ob, o, n) -> toolbar.setPrefWidth(n.longValue()));

        var logoPath = getResource("icons/logo.png");
        if (logoPath != null) {
            var img = new Image(logoPath.toExternalForm());
            logoImg.setImage(img);
            stage.getIcons().add(img);
        }

        stage.xProperty().addListener((observable, oldValue, newValue) -> {
            if (WindowUtils.isOnPrimaryScreen(newValue.doubleValue()))
                bounds = Screen.getPrimary().getVisualBounds();
        });

        WindowUtils.toolbarInits(toolbar, stage, bounds, downloadingMinWidth, downloadingMinHeight);
        ResizeUtil.addResizeListener(stage);


        advancedPane.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue)
                stage.setHeight(downloadingMinHeight + advancedPane.getHeight() + accordion.getHeight());
            else
                stage.setHeight(downloadingMinHeight);

        });
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setOnCloseRequest(event -> closeStage());
        initAfterStage();
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    public void setMainTableUtils(MainTableUtils mainTableUtils) {
        this.mainTableUtils = mainTableUtils;
    }

    public void setDownloadModel(DownloadModel downloadModel) {
        this.downloadModel = downloadModel;
        initDownloadData();
        initDownloadListeners();
    }

    private void initDownloadData() {
        var end = downloadModel.getName().length();
        if (end > 60)
            end = 60;
        titleLbl.setText(downloadModel.getName().substring(0, end));
        nameLbl.setText("Name: " + downloadModel.getName());
        var queues = downloadModel.getQueue().toString();
        queueLbl.setText("Queues: " + queues.substring(1, queues.length() - 1));
        statusLbl.setText("Status: " + downloadModel.getDownloadStatus().name());
        var downloadOf = "%s / %s"
                .formatted(IOUtils.formatBytes(downloadModel.getDownloaded()),
                        IOUtils.formatBytes(downloadModel.getSize()));
        downloadedOfLbl.setText(downloadOf);
        progressLbl.setText("Progress: %.2f%%".formatted(downloadModel.getProgress()));
        openSwitch.setSelected(downloadModel.isOpenAfterComplete());
        showSwitch.setSelected(downloadModel.isShowCompleteDialog());
        onComplete(downloadModel);
    }

    public void initDownloadListeners() {
        var dt = getDownloadTask();
        if (dt != null) {
            isPaused.set(false);
            bytesDownloadedListener(dt);
            progressListener(dt);
        } else
            isPaused.set(true);
    }

    private DownloadTask getDownloadTask() {
        return currentDownloadings.stream()
                .filter(c -> c.equals(downloadModel))
                .findAny()
                .map(DownloadModel::getDownloadTask).orElse(null);
    }

    private void bytesDownloadedListener(DownloadTask dt) {
        dt.valueProperty().addListener((o, oldValue, bytesDownloaded) -> {
            if (!isPaused.get()) {
                if (oldValue == null)
                    oldValue = bytesDownloaded;
                var speed = (bytesDownloaded - oldValue);
                if (bytesDownloaded == 0)
                    speed = 0;

                downloadModel.setSpeed(speed);
                downloadModel.setDownloadStatus(DownloadStatus.Downloading);
                downloadModel.setDownloaded(bytesDownloaded);

                speedLbl.setText(IOUtils.formatBytes(speed));
                statusLbl.setText("Status: " + DownloadStatus.Downloading);
                var downloadOf = "%s / %s"
                        .formatted(IOUtils.formatBytes(bytesDownloaded),
                                IOUtils.formatBytes(downloadModel.getSize()));
                downloadedOfLbl.setText(downloadOf);
                if (speed != 0) {
                    long delta = downloadModel.getSize() - bytesDownloaded;
                    var remaining = DurationFormatUtils.formatDuration((delta / speed) * 1000, "dd:HH:mm:ss");
                    remainingLbl.setText("Remaining: " + remaining);
                }
            }
        });
    }

    private void progressListener(DownloadTask dt) {
        dt.progressProperty().addListener((o, old, progress) -> {
            downloadProgress.setProgress(progress.floatValue());
            progressLbl.setText("Progress: %.2f%%".formatted(progress.floatValue() * 100));
        });
    }


    @Override
    public void initialize() {
        closeBtn.setGraphic(new FontIcon());
        hideBtn.setGraphic(new FontIcon());
        bounds = Screen.getPrimary().getVisualBounds();
        controlBtn.setText(isPaused.get() ? "Resume" : "Pause");
        remainingLbl.setText("Remaining: Paused");
        isPaused.addListener((o, ol, newValue) -> {
            controlBtn.setText(newValue ? "Resume" : "Pause");
            statusLbl.setText("Status: " + (newValue ? DownloadStatus.Paused : DownloadStatus.Downloading));
            var downloadOf = "%s / %s"
                    .formatted(IOUtils.formatBytes(downloadModel.getDownloaded()),
                            IOUtils.formatBytes(downloadModel.getSize()));
            downloadedOfLbl.setText(downloadOf);
            if (newValue)
                remainingLbl.setText("Remaining: Paused");
        });

        openSwitch.selectedProperty().addListener((o, old, newVal) -> {
            downloadModel.setOpenAfterComplete(newVal);
            DownloadsRepo.updateDownloadOpenAfterComplete(downloadModel);
        });
        showSwitch.selectedProperty().addListener((o, old, newVal) -> {
            downloadModel.setShowCompleteDialog(newVal);
            DownloadsRepo.updateDownloadShowCompleteDialog(downloadModel);
        });
    }

    @FXML
    private void onClose() {
        closeStage();
    }

    @FXML
    private void onControl() {

        if (isComplete) {
            if (!new File(downloadModel.getFilePath()).exists()) {
                Notifications.create()
                        .title("File not found")
                        .text("File has been moved or removed")
                        .showError();
                return;
            }
            hostServices.showDocument(downloadModel.getFilePath());
            return;
        }

        if (isPaused.get()) {
            statusLbl.setText("Status: " + DownloadStatus.Trying);
            DownloadOpUtils.resumeDownloads(mainTableUtils, List.of(downloadModel), null, null);
            isPaused.set(false);
        } else {
            var dt = getDownloadTask();
            if (dt != null)
                dt.pause();
            isPaused.set(true);
        }

    }

    public void onPause() {
        Platform.runLater(() -> isPaused.set(true));
    }

    public void onComplete(DownloadModel download) {
        if (download.getDownloadStatus() == DownloadStatus.Completed) {
            isComplete = true;
            remainingLbl.setText("Remaining: Done");
            controlBtn.setText("Open");
            downloadProgress.setProgress(100);
            statusLbl.setText("Status: Complete");
            progressLbl.setText("Progress: 100%");
            var downloadOf = "%s / %s"
                    .formatted(IOUtils.formatBytes(downloadModel.getSize()),
                            IOUtils.formatBytes(downloadModel.getSize()));
            downloadedOfLbl.setText(downloadOf);
            stage.requestFocus();
        }
    }

    @FXML
    private void hideWindowApp() {
        stage.setIconified(true);
    }

    @FXML
    public void closeStage() {
        openDownloadings.remove(this);
        stage.close();
    }

    public DownloadModel getDownloadModel() {
        return downloadModel;
    }

}
