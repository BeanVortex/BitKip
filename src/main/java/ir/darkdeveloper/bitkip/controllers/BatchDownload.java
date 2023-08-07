package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.observers.QueueObserver;
import ir.darkdeveloper.bitkip.config.observers.QueueSubject;
import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.Validations;
import ir.darkdeveloper.bitkip.utils.DownloadUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static ir.darkdeveloper.bitkip.config.AppConfigs.addSameDownload;
import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.observers.QueueSubject.getQueueSubject;
import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.Defaults.extensions;
import static ir.darkdeveloper.bitkip.utils.DownloadUtils.handleError;

public class BatchDownload implements QueueObserver {

    @FXML
    private Label errorLabel;
    @FXML
    private Button questionBtnUrl, checkBtn, cancelBtn, questionBtnChunks, openLocation, newQueue;
    @FXML
    private TextField startField, locationField, endField;
    @FXML
    private ComboBox<QueueModel> queueCombo;
    @FXML
    private TextField chunksField, urlField;


    private LinkModel tempLink;
    private Stage stage;


    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        checkBtn.setGraphic(new FontIcon());
        cancelBtn.setGraphic(new FontIcon());
        questionBtnChunks.setGraphic(new FontIcon());
        openLocation.setGraphic(new FontIcon());
        questionBtnUrl.setGraphic(new FontIcon());
        newQueue.setGraphic(new FontIcon());
        var queues = QueueSubject.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, false);
        queues = queues.stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
        errorLabel.setVisible(false);
        checkBtn.setDisable(true);
        Validations.prepareLinkFromClipboard(urlField);
        Validations.validateChunksInputChecks(chunksField);
        Validations.validateIntInputCheck(startField, 0L);
        Validations.validateIntInputCheck(endField, 0L);
        var questionBtns = new Button[]{questionBtnUrl, questionBtnChunks};
        var contents = new String[]{
                "You want to download several files, clarify where urls are different by $ sign." +
                        " (for example 'https://www.url.com/file00.zip', change 00 to $$)",
                "Every single file is seperated into parts and will be downloaded concurrently"
        };
        DownloadUtils.initPopOvers(questionBtns, contents);
        autoFillLocation();
        queueCombo.getSelectionModel().selectedIndexProperty().addListener(observable -> onQueueChanged());
        startField.textProperty().addListener(o -> autoFillLocation());
        endField.textProperty().addListener(o -> autoFillLocation());
        urlField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                DownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, checkBtn, null, null);
            else autoFillLocation();
        });
        locationField.textProperty().addListener((o, ol, n) -> {
            if (n.isBlank())
                DownloadUtils.disableControlsAndShowError("Location is blank", errorLabel, checkBtn, null, null);
            else onOfflineFieldsChanged();
        });
    }

    private void onOfflineFieldsChanged() {
        if (tempLink != null)
            handleError(() -> DownloadUtils.onOfflineFieldsChanged(locationField, tempLink.getName(),
                    null, queueCombo, null, checkBtn, openLocation, null), errorLabel);

    }

    private void autoFillLocation() {
        try {
            queueCombo.getSelectionModel().select(queueCombo.getSelectionModel().getSelectedItem());
            var url = urlField.getText();
            var start = Integer.parseInt(startField.getText());
            var end = Integer.parseInt(endField.getText());
            var links = generateLinks(url, start, end, Integer.parseInt(chunksField.getText()), true);
            var link = links.get(0);
            tempLink = link;
            var connection = DownloadUtils.connect(link.getUrl());
            var fileNameLocationFuture = CompletableFuture.supplyAsync(() -> DownloadUtils.extractFileName(link.getUrl(), connection))
                    .thenAccept(this::setLocation);
            fileNameLocationFuture
                    .whenComplete((unused, throwable) ->
                            handleError(() -> DownloadUtils.checkIfFileIsOKToSave(locationField.getText(),
                                    tempLink.getName(), null, checkBtn, null), errorLabel))
                    .exceptionally(throwable -> {
                        var errorMsg = throwable.getCause().getLocalizedMessage();
                        Platform.runLater(() ->
                                DownloadUtils.disableControlsAndShowError(errorMsg, errorLabel,
                                        checkBtn, null, null));
                        return null;
                    });
        } catch (NumberFormatException ignore) {
        } catch (Exception e) {
            var errorMsg = e.getLocalizedMessage();
            if (e instanceof IndexOutOfBoundsException)
                errorMsg = "No URLs found";
            DownloadUtils.disableControlsAndShowError(errorMsg, errorLabel, checkBtn, null, null);
        }
    }

    private void setLocation(String fileName) {
        DownloadUtils.setLocationAndQueue(locationField, fileName, null);
        tempLink.setName(fileName);
    }

    public List<LinkModel> generateLinks(String url, int start, int end, int chunks, boolean oneLink) {
        if (start > end)
            throw new IllegalArgumentException("Start value cannot be greater than end value");

        var signsIndex = new ArrayList<Integer>();
        for (int i = 0; i < url.length(); i++)
            if (url.charAt(i) == '$')
                signsIndex.add(i);

        if (signsIndex.isEmpty())
            throw new IllegalArgumentException("No pattern found in url");

        if (signsIndex.size() < digits(start) || signsIndex.size() < digits(end))
            throw new IllegalArgumentException("Not enough pattern to cover the range");


        var links = new ArrayList<LinkModel>();
        for (int i = start; i < end + 1; i++) {
            var x = i / 10;
            if (x == 0) {
                String link = url;
                for (int j = 0; j < signsIndex.size(); j++) {
                    if (j == signsIndex.size() - 1)
                        link = replaceDollarOnce(link, (char) (i + 48));
                    else
                        link = replaceDollarOnce(link, '0');
                }
                links.add(new LinkModel(link, chunks));
            } else {
                StringBuilder link = new StringBuilder(url);
                var digitsToFill = signsIndex.size();
                var iDigits = digits(i);
                var diff = digitsToFill - iDigits;
                for (int j = 0; j < diff; j++)
                    link.setCharAt(signsIndex.get(j), '0');

                var cpI = i;
                for (int j = 0; j < signsIndex.size() - diff; j++) {
                    link.setCharAt(link.lastIndexOf("$"), (char) (cpI % 10 + 48));
                    cpI /= 10;
                }
                links.add(new LinkModel(link.toString(), chunks));
            }
            if (oneLink)
                break;
        }

        return links;
    }

    private String replaceDollarOnce(String str, char replace) {
        var chars = str.toCharArray();
        for (int i = 0; i < str.length(); i++)
            if ('$' == chars[i]) {
                chars[i] = replace;
                break;
            }
        return String.copyValueOf(chars);
    }

    private int digits(int num) {
        var count = 0;
        while (num != 0) {
            count++;
            num /= 10;
        }
        return count;
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @FXML
    private void onSelectLocation(ActionEvent e) {
        DownloadUtils.selectLocation(e, locationField);
        if (tempLink != null)
            handleError(() -> DownloadUtils.checkIfFileIsOKToSave(locationField.getText(),
                    tempLink.getName(), null, checkBtn, null), errorLabel);
    }

    @FXML
    private void onCheck() {
        try {
            var url = urlField.getText();
            var path = locationField.getText();
            if (url.isBlank()) {
                log.warn("URL is blank");
                DownloadUtils.disableControlsAndShowError("URL is blank", errorLabel, checkBtn, null, null);
                return;
            }
            if (path.isBlank()) {
                log.warn("Location is blank");
                DownloadUtils.disableControlsAndShowError("Location is blank", errorLabel, checkBtn, null, null);
                return;
            }
            var start = Integer.parseInt(startField.getText());
            var end = Integer.parseInt(endField.getText());
            var links = generateLinks(url, start, end, Integer.parseInt(chunksField.getText()), false);

            if (!addSameDownload)
                for (var link : links) {
                    var byURL = DownloadsRepo.findByURL(link.getUrl());
                    if (!byURL.isEmpty()) {
                        var found = byURL.stream()
                                .filter(dm -> {
                                    var s = Paths.get(dm.getFilePath()).getParent().toString() + File.separator;
                                    return s.equals(path);
                                })
                                .findFirst();
                        if (found.isPresent()) {
                            var msg = "At least one URL exists for this location. Change location or change start, end.\n"
                                    + found.get().getUrl();
                            log.warn(msg);
                            DownloadUtils.disableControlsAndShowError(msg, errorLabel, checkBtn, null, null);
                            return;
                        }
                    }
                }
            var selectedQueue = queueCombo.getSelectionModel().getSelectedItem();
            var allDownloadsQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, false);
            var secondaryQueue = getSecondaryQueueByFileName(tempLink.getName());
            links.forEach(lm -> {
                lm.getQueues().add(allDownloadsQueue);
                lm.getQueues().add(secondaryQueue);
                if (selectedQueue.getId() != allDownloadsQueue.getId())
                    lm.getQueues().add(selectedQueue);
                lm.setPath(path);
                lm.setSelectedPath(path);
            });
            FxUtils.newBatchListStage(links);
            getQueueSubject().removeObserver(this);
            stage.close();
        } catch (IllegalArgumentException e) {
            if (e instanceof NumberFormatException)
                return;
            log.error(e.getLocalizedMessage());
            DownloadUtils.disableControlsAndShowError(e.getLocalizedMessage(), errorLabel, checkBtn, null, null);
        }
    }

    public static QueueModel getSecondaryQueueByFileName(String fileName) {
        if (fileName == null || fileName.isBlank())
            return null;
        for (var entry : extensions.entrySet()) {
            // empty is set for others
            if (entry.getValue().isEmpty())
                return QueuesRepo.findByName(entry.getKey(), false);

            var matched = entry.getValue().stream().anyMatch(fileName::endsWith);
            if (matched)
                return QueuesRepo.findByName(entry.getKey(), false);
        }
        return null;
    }


    @FXML
    private void onNewQueue() {
        FxUtils.newQueueStage();
    }

    @FXML
    private void onCancel() {
        getQueueSubject().removeObserver(this);
        stage.close();
    }

    @Override
    public void updateQueue() {
        updateQueueData(queueCombo);
    }

    public static void updateQueueData(ComboBox<QueueModel> queueCombo) {
        var queues = QueueSubject.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, false);
        queues = queues.stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().clear();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
    }

    @FXML
    private void onQueueChanged() {
        onOfflineFieldsChanged();
    }

}


