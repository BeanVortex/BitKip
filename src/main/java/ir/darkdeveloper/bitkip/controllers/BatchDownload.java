package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.InputValidations;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static ir.darkdeveloper.bitkip.config.AppConfigs.getQueueSubject;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.extensions;

public class BatchDownload implements QueueObserver {
    @FXML
    private Label errorLabel;
    @FXML
    private Button newQueue;
    @FXML
    private TextField startField;
    @FXML
    private TextField endField;
    @FXML
    private TextField locationField;
    @FXML
    private Button openLocation;
    @FXML
    private ComboBox<QueueModel> queueCombo;
    @FXML
    private TextField chunksField;
    @FXML
    private Button questionBtnChunks;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button addBtn;
    @FXML
    private Button questionBtnUrl;
    @FXML
    private TextField urlField;

    private LinkModel tempLink;

    private Stage stage;


    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    @Override
    public void initAfterStage() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addBtn.setGraphic(new FontIcon());
        cancelBtn.setGraphic(new FontIcon());
        questionBtnChunks.setGraphic(new FontIcon());
        openLocation.setGraphic(new FontIcon());
        questionBtnUrl.setGraphic(new FontIcon());
        newQueue.setGraphic(new FontIcon());
        var queues = AppConfigs.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, false);
        queues = queues.stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
        errorLabel.setVisible(false);
        addBtn.setDisable(true);
        InputValidations.prepareLinkFromClipboard(urlField);
        InputValidations.validChunksInputChecks(chunksField);
        InputValidations.validIntInputCheck(startField, 0L);
        InputValidations.validIntInputCheck(endField, 0L);
        var questionBtns = new Button[]{questionBtnUrl, questionBtnChunks};
        var contents = new String[]{
                "You want to download several files, clarify where urls are different by $ sign." +
                        " (for example 'https://www.url.com/file00.zip', change 00 to $$)",
                "Every single file is seperated into parts and will be downloaded concurrently"
        };
        NewDownloadUtils.initPopOvers(questionBtns, contents);
        autoFillLocation();
        queueCombo.getSelectionModel().selectedIndexProperty().addListener(observable -> onQueueChanged());
        startField.textProperty().addListener(o -> autoFillLocation());
        endField.textProperty().addListener(o -> autoFillLocation());
        urlField.textProperty().addListener(o -> autoFillLocation());
        locationField.textProperty().addListener((o, ol, n) -> onOfflineFieldsChanged());
    }

    private void onOfflineFieldsChanged() {
        NewDownloadUtils.onOfflineFieldsChanged(locationField, tempLink.getName(), null, queueCombo,
                errorLabel, null, addBtn, openLocation);
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
            var connection = NewDownloadUtils.connect(link.getLink(), 3000, 3000);
            var fileNameLocationFuture = CompletableFuture.supplyAsync(() -> NewDownloadUtils.extractFileName(link.getLink(), connection))
                    .thenAccept(this::setLocation);
            fileNameLocationFuture.whenComplete((unused, throwable) -> {
                NewDownloadUtils.checkIfFileExists(locationField.getText(), tempLink.getName(), errorLabel, null, addBtn);
            }).exceptionally(throwable -> {
                errorLabel.setVisible(true);
                addBtn.setDisable(true);
                var errorMsg = throwable.getCause().getLocalizedMessage();
                Platform.runLater(() -> errorLabel.setText(errorMsg));
                return null;
            });
        } catch (NumberFormatException ignore) {
        } catch (Exception e) {
            errorLabel.setVisible(true);
            addBtn.setDisable(true);
            var errorMsg = e.getLocalizedMessage();
            if (e instanceof IndexOutOfBoundsException)
                errorMsg = "No links found";
            errorLabel.setText(errorMsg);
        }
    }

    private void setLocation(String fileName) {
        NewDownloadUtils.determineLocationAndQueue(locationField, fileName, null);
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
        NewDownloadUtils.selectLocation(e, locationField);
        if (tempLink != null)
            NewDownloadUtils.checkIfFileExists(locationField.getText(), tempLink.getName(), errorLabel, null, addBtn);
    }

    @FXML
    private void onCheck() {
        try {
            var url = urlField.getText();
            var start = Integer.parseInt(startField.getText());
            var end = Integer.parseInt(endField.getText());
            var links = generateLinks(url, start, end, Integer.parseInt(chunksField.getText()), false);
            var selectedQueue = queueCombo.getSelectionModel().getSelectedItem();
            var allDownloadsQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, false);
            var secondaryQueue = getSecondaryQueueByFileName(tempLink.getName());
            links.forEach(lm -> {
                lm.getQueues().add(allDownloadsQueue);
                lm.getQueues().add(secondaryQueue);
                if (selectedQueue.getId() != allDownloadsQueue.getId())
                    lm.getQueues().add(selectedQueue);
                lm.setPath(locationField.getText());
            });
            FxUtils.newBatchListStage(links);
            getQueueSubject().removeObserver(this);
            stage.close();
        } catch (IllegalArgumentException e) {
            if (e instanceof NumberFormatException)
                return;
            errorLabel.setVisible(true);
            addBtn.setDisable(true);
            var errorStr = e.getLocalizedMessage();
            errorLabel.setText(errorStr);
        }
    }

    private QueueModel getSecondaryQueueByFileName(String fileName) {
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
        var queues = AppConfigs.getQueues();
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


