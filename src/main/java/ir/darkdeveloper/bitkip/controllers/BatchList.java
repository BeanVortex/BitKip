package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.task.LinkDataTask;
import ir.darkdeveloper.bitkip.utils.LinkTableUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.stage.Stage;

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
import static ir.darkdeveloper.bitkip.config.AppConfigs.mainTableUtils;

public class BatchList implements FXMLController {
    @FXML
    private Button addBtn;
    @FXML
    private TableView<LinkModel> linkTable;

    private Stage stage;
    private LinkTableUtils linkTableUtils;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addBtn.requestFocus();
        addBtn.setDisable(true);
    }


    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    public void setData(List<LinkModel> links) {
        linkTableUtils = new LinkTableUtils(linkTable, links, stage);
        linkTableUtils.tableInits();
        fetchLinksData(links);
    }

    private void fetchLinksData(List<LinkModel> links) {
        var executor = Executors.newCachedThreadPool();
        var linkTask = new LinkDataTask(links);
        linkTask.valueProperty().addListener((o, ol, linkFlux) ->
                executor.submit(() -> {
                    linkFlux.subscribe(
                            lm -> linkTableUtils.updateLink(lm),
                            Throwable::printStackTrace,
                            () -> {
                                addBtn.setDisable(false);
                                executor.shutdown();
                            }
                    );
                }));
        new Thread(linkTask).start();
    }

    @Override
    public void initAfterStage() {
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
                dm.setAddToQueueDate(list.get(i-1).getAddToQueueDate().plusSeconds(1));
            dm.setResumable(lm.getResumeable());
            dm.setQueues(new CopyOnWriteArrayList<>(lm.getQueues()));
            dm.setDownloadStatus(DownloadStatus.Paused);
            dm.setAgent(lm.getAgent());
            list.add(dm);
        }
        return list;
    }

}
