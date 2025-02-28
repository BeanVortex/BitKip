package io.beanvortex.bitkip.controllers;

import io.beanvortex.bitkip.BitKip;
import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.models.*;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import io.beanvortex.bitkip.task.LinkDataTask;
import io.beanvortex.bitkip.utils.Defaults;
import io.beanvortex.bitkip.utils.DownloadUtils;
import io.beanvortex.bitkip.utils.FxUtils;
import io.beanvortex.bitkip.utils.LinkTableUtils;
import io.beanvortex.bitkip.config.observers.QueueObserver;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
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

import static io.beanvortex.bitkip.config.AppConfigs.log;
import static io.beanvortex.bitkip.config.AppConfigs.mainTableUtils;
import static io.beanvortex.bitkip.config.observers.QueueSubject.getQueues;

public class BatchList implements QueueObserver {

    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField locationField, usernameField;
    @FXML
    private CheckBox lastLocationCheck, authorizedCheck;
    @FXML
    private ComboBox<QueueModel> comboQueue;
    @FXML
    private Button addBtn, newQueue, openLocation, refreshBtn, stopBtn;
    @FXML
    private TableView<LinkModel> linkTable;

    private volatile boolean fetchingStopped;

    private Stage stage;
    private LinkTableUtils linkTableUtils;
    private List<LinkModel> links;
    private static final QueueModel customQueue = new QueueModel("CUSTOM", false);
    private Credentials credentials;

    @FXML
    private void onAuthorizedCheck() {
        toggleViewOfAuthorizeFields(authorizedCheck.isSelected());
        usernameField.setText(null);
        passwordField.setText(null);
    }

    public void setCredential(Credentials credentials) {
        this.credentials = credentials;
        if (credentials != null){
            authorizedCheck.setSelected(true);
            authorizedCheck.setDisable(true);
            usernameField.setText("***");
            passwordField.setText("***");
            toggleViewOfAuthorizeFields(false);
        }
    }


    @AllArgsConstructor
    @Getter
    @Setter
    public static class LocationData {
        private TextField locationField;
        private String firstPath;
        private boolean change;

        public void revertPath() {
            locationField.setText(firstPath);
        }
    }

    private LocationData location;

    @Override
    public void initialize(URL l, ResourceBundle resources) {
        addBtn.requestFocus();
        addBtn.setDisable(true);
        comboQueue.setDisable(true);
        newQueue.setGraphic(new FontIcon());
        refreshBtn.setGraphic(new FontIcon());
        stopBtn.setGraphic(new FontIcon());
        stopBtn.setVisible(false);
        stopBtn.setDisable(true);
        stopBtn.setOnAction(e -> fetchingStopped = true);
        refreshBtn.setOnAction(e -> fetchLinksData(this.links));
        toggleViewOfAuthorizeFields(false);
        location = new LocationData(locationField, null, true);
    }

    private void toggleViewOfAuthorizeFields(boolean visible) {
        usernameField.getParent().setManaged(visible);
        usernameField.getParent().setVisible(visible);
        passwordField.getParent().setManaged(visible);
        passwordField.getParent().setVisible(visible);
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
        location.setFirstPath(links.get(0).getPath());
        locationField.setText(location.getFirstPath());
        locationField.textProperty().addListener((o, ol, n) -> {
            if (location.isChange()) {
                links.forEach(l -> l.setPath(n));
                linkTableUtils.refreshTable();
            } else location.setChange(true);
        });
        fetchLinksData(links);
        initQueueCombo();
    }


    private void fetchLinksData(List<LinkModel> links) {
        fetchingStopped = false;
        var executor = Executors.newCachedThreadPool();
        if (credentials == null)
            credentials = new Credentials(usernameField.getText(), passwordField.getText());
        var linkTask = new LinkDataTask(links, credentials);
        linkTask.valueProperty().addListener((o, ol, linkFlux) ->
                executor.submit(() -> {
                    Platform.runLater(() -> {
                        refreshBtn.setDisable(true);
                        refreshBtn.setVisible(false);
                        stopBtn.setDisable(false);
                        stopBtn.setVisible(true);
                    });
                    linkFlux.takeUntil(lm -> fetchingStopped)
                            .subscribe(
                                    lm -> {
                                        linkTableUtils.updateLink(lm);
                                        if (!stage.isShowing())
                                            linkTask.setCancel(true);
                                    },
                                    this::errorLog,
                                    () -> {
                                        addBtn.setDisable(false);
                                        comboQueue.setDisable(false);
                                        Platform.runLater(() -> {
                                            refreshBtn.setDisable(false);
                                            refreshBtn.setVisible(true);
                                            stopBtn.setDisable(true);
                                            stopBtn.setVisible(false);
                                        });
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
                .filter(qm -> qm.getName().equals(Defaults.ALL_DOWNLOADS_QUEUE) || !Defaults.staticQueueNames.contains(qm.getName()))
                .toList());
        queuesToShow.add(customQueue);
        comboQueue.setOnAction(null);
        comboQueue.setItems(FXCollections.observableArrayList(queuesToShow));
        comboQueue.getSelectionModel().select(selectedQueue);
        comboQueue.setOnAction(e -> {
            var selectedItem = comboQueue.getSelectionModel().getSelectedItem();
            if (selectedItem.getName().equals(customQueue.getName()))
                return;
            linkTableUtils.changeQueues(selectedItem, location);
        });
    }


    private void errorLog(Throwable err) {
        log.error(err.getMessage());
    }

    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
        openLocation.setGraphic(new FontIcon());
        stage.widthProperty().addListener((ob, o, n) -> linkTable.setPrefWidth(n.doubleValue() + 90));
        var logoPath = BitKip.getResource("icons/logo.png");
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
            dm.setUri(lm.getUri());
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
            dm.setTurnOffMode(TurnOffMode.NOTHING);
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

    @FXML
    private void onSelectLocation(ActionEvent e) {
        var path = DownloadUtils.selectLocation(FxUtils.getStageFromEvent(e));
        if (path != null)
            locationField.setText(path);
//        DownloadUtils.handleError(() -> DownloadUtils.checkIfFileIsOKToSave(locationField.getText(),
//                null, null, addBtn, null, lastLocationCheck), null);
    }

    @FXML
    private void onLastLocationCheck() {
        if (lastLocationCheck.isSelected())
            locationField.setText(AppConfigs.lastSavedDir);
        else
            locationField.setText(location.getFirstPath());
    }

}
