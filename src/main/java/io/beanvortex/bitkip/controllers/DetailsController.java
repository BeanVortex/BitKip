package io.beanvortex.bitkip.controllers;

import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.controllers.interfaces.FXMLController;
import io.beanvortex.bitkip.models.Credentials;
import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.models.DownloadStatus;
import io.beanvortex.bitkip.models.TurnOffMode;
import io.beanvortex.bitkip.repo.DatabaseHelper;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import io.beanvortex.bitkip.repo.QueuesRepo;
import io.beanvortex.bitkip.task.ChunksDownloadTask;
import io.beanvortex.bitkip.task.DownloadTask;
import io.beanvortex.bitkip.utils.DownloadOpUtils;
import io.beanvortex.bitkip.utils.FxUtils;
import io.beanvortex.bitkip.utils.IOUtils;
import io.beanvortex.bitkip.utils.Validations;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static io.beanvortex.bitkip.config.AppConfigs.mainTableUtils;

public class DetailsController implements FXMLController {

    @FXML
    private CheckBox authenticatedCheck;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button speedApplyBtn, allBytesBtn;
    @FXML
    private ComboBox<TurnOffMode> turnOffCombo;
    @FXML
    private Hyperlink link;
    @FXML
    private TextField bytesField, speedField, usernameField;
    @FXML
    private ToggleSwitch openSwitch, showSwitch;
    @FXML
    private ProgressBar downloadProgress;
    @FXML
    private Label nameLbl, queueLbl, remainingLbl, locationLbl, progressLbl, resumableLbl, chunksLbl,
            statusLbl, speedLbl, downloadedOfLbl, downloadedBytes;
    @FXML
    private Button controlBtn, openFolderBtn;
    @FXML
    HBox drag;

    private Stage stage;
    private DownloadModel dm;
    private boolean isComplete = false;
    private final BooleanProperty isPaused = new SimpleBooleanProperty(true);
    private final PopOver linkPopover = new PopOver();
    private final ClipboardContent filesToCopyClipboard = new ClipboardContent();
    private final List<File> dragFiles = new ArrayList<>();


    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    public void setDownloadModel(DownloadModel dm) {
        this.dm = dm;
        initDownloadData();
        initDownloadListeners();
    }

    private void initDownloadData() {
        Validations.validateInputChecks(null, bytesField, speedField, dm);
        var byteLimit = String.valueOf(dm.getByteLimit());
        if (byteLimit.equals("0"))
            byteLimit = String.valueOf(dm.getSize());
        bytesField.setText(byteLimit);
        bytesField.setDisable(dm.getSize() < 0 || !dm.isResumable());
        speedField.setDisable(!dm.isResumable());
        var mbOfBytes = IOUtils.getMbOfBytes(dm.getSpeedLimit());
        speedField.setText(dm.getSpeedLimit() == 0 ? "0.0" : String.valueOf(mbOfBytes));
        downloadedBytes.setText(String.valueOf(dm.getDownloaded()));
        link.setText(dm.getUri());
        locationLbl.setText("Path: " + new File(dm.getFilePath()).getParentFile().getAbsolutePath());
        var end = dm.getName().length();
        if (end > 60)
            end = 60;
        stage.setTitle(dm.getName().substring(0, end));
        nameLbl.setText("Name: " + dm.getName());
        var queues = QueuesRepo.findQueuesOfADownload(dm.getId(), false, false).toString();
        queueLbl.setText("Queues: " + queues.substring(1, queues.length() - 1));
        statusLbl.setText("Status: " + dm.getDownloadStatus().name());
        var downloadOf = "%s / %s"
                .formatted(IOUtils.formatBytes(dm.getDownloaded()),
                        IOUtils.formatBytes(dm.getSize()));
        downloadedOfLbl.setText(downloadOf);
        progressLbl.setText("Progress: %.2f%%".formatted(dm.getProgress()));
        downloadProgress.setProgress(dm.getProgress() / 100);
        chunksLbl.setText("Chunks: " + dm.getChunks());
        var resumable = dm.isResumable();
        if (resumable) {
            resumableLbl.getStyleClass().add("yes");
            resumableLbl.getStyleClass().remove("no");
            resumableLbl.setText("Resumable");
        } else {
            resumableLbl.getStyleClass().add("no");
            resumableLbl.getStyleClass().remove("yes");
            resumableLbl.setText("Not Resumable");
        }
        if (dm.getCredentials() != null && dm.getCredentials().isOk()) {
            authenticatedCheck.setSelected(true);
            usernameField.getParent().setVisible(true);
            passwordField.getParent().setVisible(true);
            usernameField.setText("***");
            passwordField.setText("***");
        }
        controlBtn.setText(isPaused.get() ? (resumable ? "Resume" : "Restart") : "Pause");
        openSwitch.setSelected(dm.isOpenAfterComplete());
        showSwitch.setSelected(dm.isShowCompleteDialog());
        drag.setDisable(true);
        drag.setVisible(false);
        onComplete(dm);
        turnOffCombo.getSelectionModel().select(dm.getTurnOffMode());
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
        return AppConfigs.currentDownloadings.stream()
                .filter(dm -> dm.equals(this.dm))
                .findFirst()
                .map(DownloadModel::getDownloadTask).orElse(null);
    }

    private void bytesDownloadedListener(DownloadTask dt) {
        if (dm.getSize() != -1)
            dt.valueProperty().addListener((ob, o, bytes) -> {
                if (!isPaused.get()) {
                    if (o == null)
                        o = bytes;
                    var speed = (bytes - o);

                    downloadedBytes.setText(String.valueOf(bytes));
                    if (bytes == 0) {
                        downloadedBytes.setText(String.valueOf(dm.getDownloaded()));
                        speed = 0;
                    } else
                        dm.setDownloaded(bytes);

                    dm.setSpeed(speed);
                    dm.setDownloadStatus(DownloadStatus.Downloading);

                    speedLbl.setText(IOUtils.formatBytes(speed));
                    statusLbl.setText("Status: " + DownloadStatus.Downloading);
                    var downloadOf = "%s / %s"
                            .formatted(IOUtils.formatBytes(bytes),
                                    IOUtils.formatBytes(dm.getSize()));
                    downloadedOfLbl.setText(downloadOf);
                    if (speed > 0) {
                        long delta = dm.getSize() - bytes;
                        var remaining = DurationFormatUtils.formatDuration((delta / speed) * 1000, "dd:HH:mm:ss");
                        remainingLbl.setText("Remaining: " + remaining);
                    }
                }
            });
        else
            dt.valueProperty().addListener((ob, o, bytes) -> {
                if (!isPaused.get()) {
                    dm.setDownloadStatus(DownloadStatus.Downloading);
                    dm.setDownloaded(bytes);
                    statusLbl.setText("Status: " + DownloadStatus.Downloading);
                    var downloadOf = "%s / %s".formatted(IOUtils.formatBytes(bytes), IOUtils.formatBytes(0));
                    downloadedOfLbl.setText(downloadOf);
                    remainingLbl.setText("Not Clear");
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
    public void initialize(URL location, ResourceBundle resources) {
        remainingLbl.setText("Remaining: Paused");
        isPaused.addListener((o, ol, newValue) -> {
            if (!Platform.isFxApplicationThread())
                Platform.runLater(() -> updatePause(newValue));
            else updatePause(newValue);
        });
        openSwitch.selectedProperty().addListener((o, old, newVal) -> {
            dm.setOpenAfterComplete(newVal);
            DownloadsRepo.updateDownloadOpenAfterComplete(dm);
        });
        showSwitch.selectedProperty().addListener((o, old, newVal) -> {
            dm.setShowCompleteDialog(newVal);
            DownloadsRepo.updateDownloadShowCompleteDialog(dm);
        });
        linkPopover.setAnimated(true);
        linkPopover.setContentNode(new Label("Copied"));
        link.setOnMouseExited(event -> linkPopover.hide());
        turnOffCombo.setItems(FXCollections.observableArrayList(TurnOffMode.NOTHING, TurnOffMode.SLEEP, TurnOffMode.TURN_OFF));
        speedApplyBtn.setGraphic(new FontIcon());
        speedApplyBtn.setVisible(false);
        speedApplyBtn.setDisable(true);
        allBytesBtn.setVisible(false);
        allBytesBtn.setDisable(true);
        bytesField.textProperty().addListener(o -> onBytesChanged());
        speedField.textProperty().addListener(o -> onSpeedChanged());
        usernameField.getParent().setVisible(false);
        passwordField.getParent().setVisible(false);
    }

    private void updatePause(Boolean paused) {
        if (dm.getDownloadStatus() == DownloadStatus.Completed)
            return;
        controlBtn.setDisable(false);

        controlBtn.setText(paused ? (dm.isResumable() ? "Resume" : "Restart") : "Pause");
        if (paused)
            remainingLbl.setText("Remaining: Paused");
        if (dm.getDownloadStatus() == DownloadStatus.Merging) {
            setPauseButtonDisable(true);
            updateLabels("Status: " + DownloadStatus.Merging.name(), "Remaining: Merging");
            downloadProgress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        } else
            statusLbl.setText("Status: " + (paused ? DownloadStatus.Paused : DownloadStatus.Downloading));
        var downloadOf = "%s / %s"
                .formatted(IOUtils.formatBytes(dm.getDownloaded()),
                        IOUtils.formatBytes(dm.getSize()));
        downloadedOfLbl.setText(downloadOf);

    }

    @FXML
    private void onClose() {
        closeStage();
    }

    @FXML
    private void onControl() {
        if (isComplete) {
            DownloadOpUtils.openFile(dm);
            stage.close();
            return;
        }
        Runnable graphicalPause = () -> {
            mainTableUtils.refreshTable();
            isPaused.set(true);
        };
        if (isPaused.get()) {
            statusLbl.setText("Status: " + DownloadStatus.Trying);
            controlBtn.setDisable(true);
            if (!usernameField.getText().equals("***") && !passwordField.getText().equals("***")) {
                dm.setCredentials(new Credentials(usernameField.getText(), passwordField.getText()));
                DownloadsRepo.updateDownloadCredential(dm);
            }
            DownloadOpUtils.resumeDownloads(List.of(dm),
                    IOUtils.getBytesFromString(speedField.getText()), Long.parseLong(bytesField.getText()), graphicalPause);
            controlBtn.setDisable(false);
            isPaused.set(false);
        } else {
            var dt = getDownloadTask();
            controlBtn.setDisable(true);
            if (dt != null)
                dt.pause(graphicalPause);
        }

    }

    @FXML
    public void copyLink() {
        FxUtils.setClipboard(link.getText());
        linkPopover.show(link);
    }

    public void onPause() {
        Platform.runLater(() -> isPaused.set(true));
    }

    public void onComplete(DownloadModel download) {
        if (download.getDownloadStatus() == DownloadStatus.Completed)
            Platform.runLater(() -> {
                stage.show();
                stage.toFront();
                stage.setIconified(false);
                stage.setAlwaysOnTop(true);
                stage.setAlwaysOnTop(false);
                isComplete = true;
                remainingLbl.setText("Remaining: Done");
                controlBtn.setText("Open");
                openFolderBtn.setText("Open folder");
                downloadProgress.setProgress(100);
                statusLbl.setText("Status: Complete");
                progressLbl.setText("Progress: 100%");
                var downloadOf = "%s / %s"
                        .formatted(IOUtils.formatBytes(dm.getSize()),
                                IOUtils.formatBytes(dm.getSize()));
                downloadedOfLbl.setText(downloadOf);
                bytesField.setDisable(true);
                speedField.setDisable(true);
                openFolderBtn.setVisible(true);
                turnOffCombo.setDisable(true);
                openSwitch.setDisable(true);
                showSwitch.setDisable(true);
                speedApplyBtn.setDisable(true);
                speedApplyBtn.setVisible(false);
                drag.setDisable(false);
                drag.setVisible(true);
                initDragAndDrop(download);
                setPauseButtonDisable(false);
            });
    }

    private void initDragAndDrop(DownloadModel download) {
        drag.setOnDragDetected(me -> {
            Dragboard db = drag.startDragAndDrop(TransferMode.ANY);

            File f = new File(download.getFilePath());
            dragFiles.add(f);
            filesToCopyClipboard.putFiles(dragFiles);
            db.setContent(filesToCopyClipboard);
        });
        drag.setOnDragDone(me -> {
            filesToCopyClipboard.clear();
            dragFiles.clear();
            me.consume();
            stage.close();
        });
    }

    public void closeStage() {
        AppConfigs.openDownloadings.remove(this);
        linkPopover.hide();
        stage.close();
    }

    public DownloadModel getDownloadModel() {
        return dm;
    }


    @FXML
    private void onFolderOpen() {
        DownloadOpUtils.openContainingFolder(dm.getFilePath());
        stage.close();
    }

    @FXML
    private void onTurnOffChanged() {
        var value = turnOffCombo.getValue();
        dm.setTurnOffMode(value);
        var downloadTask = dm.getDownloadTask();
        if (downloadTask != null)
            downloadTask.setDownloadModel(dm);
        DatabaseHelper.updateCol(DownloadsRepo.COL_TURNOFF_MODE,
                value.toString(), DatabaseHelper.DOWNLOADS_TABLE_NAME, dm.getId());
        if (value != TurnOffMode.NOTHING) {
            Notifications.create()
                    .title("Turn off mode changed")
                    .text("Your computer will %s after download has done".formatted(value.toString().toLowerCase()))
                    .showInformation();
        }
    }

    @FXML
    private void onSpeedApplied() {
        var dmTask = dm.getDownloadTask();
        if (dmTask instanceof ChunksDownloadTask cdt) {
            var speed = IOUtils.getBytesFromString(speedField.getText());
            dm.setSpeed(speed);
            dm.setSpeedLimit(speed);
            cdt.setSpeedLimit(speed);
            speedApplyBtn.setDisable(true);
            speedApplyBtn.setVisible(false);
        }
    }

    @FXML
    private void onAllBytes() {
        bytesField.setText(String.valueOf(dm.getSize()));
        allBytesBtn.setVisible(false);
        allBytesBtn.setDisable(true);
    }

    @FXML
    private void onAuthenticatedCheck() {
        usernameField.getParent().setVisible(authenticatedCheck.isSelected());
        passwordField.getParent().setVisible(authenticatedCheck.isSelected());
        usernameField.setText("");
        passwordField.setText("");
    }

    private void onBytesChanged() {
        var text = bytesField.getText();
        long value = 0;
        if (!text.matches("\\d*"))
            text = text.replaceAll("\\D", "");

        if (!text.isBlank())
            value = Long.parseLong(text);
        if (value < dm.getSize()) {
            allBytesBtn.setVisible(true);
            allBytesBtn.setDisable(false);
        }

    }

    private void onSpeedChanged() {
        var isDownloading = dm.getDownloadStatus() == DownloadStatus.Downloading;
        var isZero = speedField.getText().equals("0");
        speedApplyBtn.setVisible(isDownloading && !isZero);
        speedApplyBtn.setDisable(isDownloading && isZero);
    }


    public ProgressBar getProgressBar() {
        return downloadProgress;
    }

    public void updateLabels(String status, String remaining) {
        statusLbl.setText(status);
        remainingLbl.setText(remaining);
    }

    public void setPauseButtonDisable(boolean disable) {
        controlBtn.setDisable(disable);
    }

    public Label getSpeedLbl() {
        return speedLbl;
    }

    public Label getDownloadedOfLbl() {
        return downloadedOfLbl;
    }
}
