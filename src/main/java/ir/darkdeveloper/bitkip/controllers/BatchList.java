package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.observers.QueueObserver;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.task.LinkDataTask;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.LinkTableUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.mainTableUtils;
import static ir.darkdeveloper.bitkip.config.observers.QueueSubject.getQueues;
import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.Defaults.staticQueueNames;

public class BatchList implements QueueObserver {
    @FXML
    private ComboBox<QueueModel> comboQueue;
    @FXML
    private Button addBtn,newQueue;
    @FXML
    private TableView<LinkModel> linkTable;

    private Stage stage;
    private LinkTableUtils linkTableUtils;
    private List<LinkModel> links;
    private static final QueueModel customQueue = new QueueModel("CUSTOM", false);


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addBtn.requestFocus();
        addBtn.setDisable(true);
        comboQueue.setDisable(true);
        newQueue.setGraphic(new FontIcon());
    }


    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    public void setData(List<LinkModel> links) {
        this.links = links;
        linkTableUtils = new LinkTableUtils(linkTable, links, comboQueue, stage);
        linkTableUtils.tableInits();
        fetchLinksData(links);
        initQueueCombo();
    }


    private void fetchLinksData(List<LinkModel> links) {
        var executor = Executors.newCachedThreadPool();
        var linkTask = new LinkDataTask(links);
        linkTask.valueProperty().addListener((o, ol, linkFlux) ->
                executor.submit(() -> {
                    linkFlux.subscribe(
                            lm -> {
                                linkTableUtils.updateLink(lm);
                                if (!stage.isShowing())
                                    linkTask.setCancel(true);

                            },
                            this::errorLog,
                            () -> {
                                addBtn.setDisable(false);
                                comboQueue.setDisable(false);
                                executor.shutdown();
                            }
                    );
                }));
        executor.submit(linkTask);
    }

    private void initQueueCombo() {
        var queues = links.get(0).getQueues();
        // get selected queue if not selected any queue in batch download, get All downloads queue
        var selectedQueue = queues.size() == 3 ? queues.get(2) : queues.get(0);
        var queuesToShow = new ArrayList<>(getQueues().stream()
                .filter(qm -> qm.getName().equals(ALL_DOWNLOADS_QUEUE) || !staticQueueNames.contains(qm.getName()))
                .toList());
        queuesToShow.add(customQueue);
        comboQueue.setOnAction(null);
        comboQueue.setItems(FXCollections.observableArrayList(queuesToShow));
        comboQueue.getSelectionModel().select(selectedQueue);
        comboQueue.setOnAction(e -> {
            var selectedItem = comboQueue.getSelectionModel().getSelectedItem();
            if (selectedItem.getName().equals(customQueue.getName()))
                return;
            linkTableUtils.changeQueues(selectedItem);
        });
    }


    private void errorLog(Throwable err) {
        log.error(err.getMessage());
    }

    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
        stage.widthProperty().addListener((ob, o, n) -> linkTable.setPrefWidth(n.doubleValue() + 90));
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null) {
            var img = new Image(logoPath.toExternalForm());
            stage.getIcons().add(img);
        }
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @FXML
    private void onCancel() {
        stage.close();
    }

    @FXML
    private void onAdd() {
        var links = linkTableUtils.getLinks();
        var downloads = new ArrayList<>(createDownloads(links));
        Collections.reverse(downloads);
        DownloadsRepo.insertDownloads(downloads);
        mainTableUtils.addRows(downloads);
        stage.close();
    }


    private List<DownloadModel> createDownloads(List<LinkModel> links) {
        var list = new ArrayList<DownloadModel>();
        for (int i = 0; i < links.size(); i++) {
            var lm = links.get(i);
            var dm = new DownloadModel();
            dm.setUrl(lm.getUrl());
            var fileName = lm.getName();
            var path = lm.getPath();
            if (path.endsWith(File.separator))
                dm.setFilePath(path + fileName);
            else
                dm.setFilePath(path + File.separator + fileName);
            dm.setProgress(0);
            dm.setName(fileName);
            dm.setSize(lm.getSize());
            dm.setChunks(lm.getChunks());
            dm.setAddDate(LocalDateTime.now());
            if (i == 0)
                dm.setAddToQueueDate(LocalDateTime.now());
            else
                dm.setAddToQueueDate(list.get(i - 1).getAddToQueueDate().plusSeconds(1));
            dm.setResumable(lm.getResumable());
            dm.setQueues(new CopyOnWriteArrayList<>(lm.getQueues()));
            dm.setDownloadStatus(DownloadStatus.Paused);
            list.add(dm);
        }
        return list;
    }

    @Override
    public void updateQueue() {
        linkTableUtils.updateQueues();
        initQueueCombo();
    }

    public void onNewQueue() {
        FxUtils.newQueueStage();

    }
}
