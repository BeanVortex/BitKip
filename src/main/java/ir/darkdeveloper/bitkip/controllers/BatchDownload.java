package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.controllers.interfaces.NewDownloadFxmlController;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import ir.darkdeveloper.bitkip.utils.TableUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class BatchDownload implements NewDownloadFxmlController {
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
    private TextField speedField;
    @FXML
    private Button questionBtnSpeed;
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
    private FXMLController parentController;


    @Override
    public void setTableUtils(TableUtils tableUtils) {
        this.tableUtils = tableUtils;
    }

    @Override
    public void setParentController(FXMLController parentController) {
        this.parentController = parentController;
    }

    @Override
    public void updateQueueList() {
        var queues = QueuesRepo.getQueues().stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().clear();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
        parentController.updateQueueList();
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
        questionBtnSpeed.setGraphic(new FontIcon());
        questionBtnUrl.setGraphic(new FontIcon());
        newQueue.setGraphic(new FontIcon());
        var queues = QueuesRepo.getQueues().stream().filter(QueueModel::isCanAddDownload).toList();
        queueCombo.getItems().addAll(queues);
        queueCombo.setValue(queues.get(0));
        NewDownloadUtils.validInputChecks(chunksField, null, speedField);
        var questionBtns = new Button[]{questionBtnSpeed, questionBtnUrl, questionBtnChunks};
        var contents = new String[]{
                "You can limit downloading speed. calculated in MB. (0.8 means 800KB)",
                "You want to download several files, clarify where urls are different by $ sign." +
                        " (for example 'https://www.url.com/file00.zip', change 00 to $$)",
                "Every single file is seperated into parts and will be downloaded concurrently"
        };
        NewDownloadUtils.initPopOvers(questionBtns, contents);
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @FXML
    private void onQuestionUrl(ActionEvent actionEvent) {
    }


    @FXML
    private void onSelectLocation(ActionEvent actionEvent) {
    }

    @FXML

    private void onQueueChanged(ActionEvent actionEvent) {
    }

    @FXML

    private void onQuestionSpeed(ActionEvent actionEvent) {
    }

    @FXML
    private void onQuestionChunks(ActionEvent actionEvent) {
    }

    @FXML
    private void onNewQueue(ActionEvent actionEvent) {
    }


    public List<String> generateLinks(String url, int start, int end) {
        if (start > end)
            throw new IllegalArgumentException("Start value cannot be greater than end value");

        var signsIndex = new ArrayList<Integer>();
        for (int i = 0; i < url.length(); i++)
            if (url.charAt(i) == '$')
                signsIndex.add(i);


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
}
