package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.controllers.interfaces.NewDownloadFxmlController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import ir.darkdeveloper.bitkip.utils.TableUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class BatchDownload implements NewDownloadFxmlController, QueueObserver {
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

    private Stage stage;
    private TableUtils tableUtils;


    @Override
    public void setTableUtils(TableUtils tableUtils) {
        this.tableUtils = tableUtils;
    }


    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    @Override
    public void initAfterStage() {
    }

    @Override
    public void initialize() {
        addBtn.setGraphic(new FontIcon());
        cancelBtn.setGraphic(new FontIcon());
        questionBtnChunks.setGraphic(new FontIcon());
        openLocation.setGraphic(new FontIcon());
        questionBtnUrl.setGraphic(new FontIcon());
        newQueue.setGraphic(new FontIcon());
        var queues = QueuesRepo.getQueues().stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
        errorLabel.setVisible(false);
        NewDownloadUtils.prepareLinkFromClipboard(urlField);
        NewDownloadUtils.validChunksInputChecks(chunksField);
        var questionBtns = new Button[]{questionBtnUrl, questionBtnChunks};
        var contents = new String[]{
                "You want to download several files, clarify where urls are different by $ sign." +
                        " (for example 'https://www.url.com/file00.zip', change 00 to $$)",
                "Every single file is seperated into parts and will be downloaded concurrently"
        };
        NewDownloadUtils.initPopOvers(questionBtns, contents);

        startField.textProperty().addListener(o -> autoFillLocation());
        endField.textProperty().addListener(o -> autoFillLocation());
        urlField.textProperty().addListener(o -> autoFillLocation());
    }

    @FXML
    private void onAdd() {
        try {
            var url = urlField.getText();
            var start = Integer.parseInt(startField.getText());
            var end = Integer.parseInt(endField.getText());
            var links = generateLinks(url, start, end, false);
            System.out.println(links.size());
            // todo: new link check stage :: check their size and existence in system
        } catch (IllegalArgumentException e) {
            errorLabel.setVisible(true);
            addBtn.setDisable(true);
            var errorStr = e.getCause().getLocalizedMessage();
            errorLabel.setText(errorStr);
        }
    }


    private void autoFillLocation() {
        var executor = Executors.newCachedThreadPool();
        var dm = new DownloadModel();
        var url = urlField.getText();
        var start = Integer.parseInt(startField.getText());
        var end = Integer.parseInt(endField.getText());
        String link;
        try {
            var links = generateLinks(url, start, end, true);
            link = links.get(0);
        } catch (Exception e) {
            executor.shutdown();
            errorLabel.setVisible(true);
            addBtn.setDisable(true);
            var errorMsg = e.getLocalizedMessage();
            if (e instanceof IndexOutOfBoundsException)
                errorMsg = "No links found";
            errorLabel.setText(errorMsg);
            return;
        }
        var fileNameLocationFuture = NewDownloadUtils.prepareFileName(link, null, executor)
                .thenAccept(fileName -> NewDownloadUtils.determineLocationAndQueue(locationField, fileName, dm));
        fileNameLocationFuture.whenComplete((unused, throwable) -> {
            errorLabel.setVisible(false);
            addBtn.setDisable(false);
            executor.shutdown();
        }).exceptionally(throwable -> {
            if (!executor.isShutdown())
                executor.shutdown();
            errorLabel.setVisible(true);
            addBtn.setDisable(true);
            var errorMsg = throwable.getCause().getLocalizedMessage();
            Platform.runLater(() -> errorLabel.setText(errorMsg));
            return null;
        });
    }

    public List<String> generateLinks(String url, int start, int end, boolean oneLink) {
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


        var links = new ArrayList<String>();
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
                links.add(link);
                if (oneLink)
                    break;
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
                links.add(String.valueOf(link));
                if (oneLink)
                    break;
            }
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
    }

    @FXML
    private void onNewQueue() {
        FxUtils.newQueueStage();
    }

    @FXML
    private void onCancel() {
        stage.close();
    }

    @Override
    public void updateQueue() {
        var queues = AppConfigs.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getQueues();
        queues = queues.stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().clear();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
    }
}
