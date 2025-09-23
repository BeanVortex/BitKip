package io.beanvortex.bitkip.controllers;

import io.beanvortex.bitkip.repo.DownloadsRepo;
import io.beanvortex.bitkip.utils.DownloadOpUtils;
import io.beanvortex.bitkip.utils.MainTableUtils;
import io.beanvortex.bitkip.utils.MenuUtils;
import io.beanvortex.bitkip.utils.SideUtils;
import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.config.observers.QueueObserver;
import io.beanvortex.bitkip.config.observers.QueueSubject;
import io.beanvortex.bitkip.controllers.interfaces.FXMLController;
import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.models.DownloadStatus;
import io.beanvortex.bitkip.repo.QueuesRepo;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import static io.beanvortex.bitkip.BitKip.getResource;
import static io.beanvortex.bitkip.config.AppConfigs.selectedQueue;
import static io.beanvortex.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;


public class MainController implements FXMLController, QueueObserver {

    @FXML
    private TextField searchField;
    @FXML
    private TreeView<String> sideTree;
    @FXML
    private Button newDownloadBtn, menuFile, operationMenu, moreBtn, clearSearchBtn;
    @FXML
    private TableView<DownloadModel> contentTable;
    @FXML
    private HBox toolbar;

    private Stage stage;
    private MainTableUtils mainTableUtils;

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }


    @Override
    public Stage getStage() {
        return stage;
    }


    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
        mainTableUtils = new MainTableUtils(contentTable);
        AppConfigs.mainTableUtils = mainTableUtils;
        mainTableUtils.tableInits();
        initSides();
        stage.widthProperty().addListener((ob, o, n) -> {
            contentTable.setPrefWidth(n.doubleValue() + 90);
            toolbar.setPrefWidth(n.longValue());
        });
        stage.heightProperty().addListener((ob, o, n) ->
                sideTree.setPrefHeight(n.doubleValue() - toolbar.getPrefHeight()));
        hideClearSearchBtn(true);


        newDownloadBtnInits();
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null) {
            var img = new Image(logoPath.toExternalForm());
            stage.getIcons().add(img);
        }
        MenuUtils.initFileMenu(menuFile);
        MenuUtils.initOperationMenu(operationMenu);
        MenuUtils.initMoreMenu(moreBtn, contentTable);
        searchField.textProperty().addListener((o, ol, n) -> {
            hideClearSearchBtn(n.isEmpty());
            if (n.length() < 3) {
                mainTableUtils.setDownloads(normalizeDownloadsList(selectedQueue.getDownloads()), true);
                return;
            }
            var foundItems = DownloadsRepo.searchLike(n, selectedQueue);
            mainTableUtils.setDownloads(normalizeDownloadsList(foundItems), true);
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        newDownloadBtn.setGraphic(new FontIcon());
        clearSearchBtn.setGraphic(new FontIcon());
        StackPane.setAlignment(newDownloadBtn, Pos.BOTTOM_RIGHT);
    }

    private void initSides() {
        selectedQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, true);
        mainTableUtils.setDownloads(normalizeDownloadsList(selectedQueue.getDownloads()), true);
        var queues = QueueSubject.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, true);

        sideTree.focusedProperty().addListener(o -> mainTableUtils.clearSelection());
        SideUtils.prepareSideTree(sideTree, queues, searchField);

    }

    private static List<DownloadModel> normalizeDownloadsList(List<DownloadModel> dms) {
        return dms.stream()
                .peek(dm -> {
                    dm.setDownloadStatus(DownloadStatus.Paused);
                    if (dm.getProgress() == 100)
                        dm.setDownloadStatus(DownloadStatus.Completed);
                })
                .toList();
    }

    private void newDownloadBtnInits() {
        var transition = new TranslateTransition(Duration.millis(300), newDownloadBtn);
        var scrollBar = new AtomicReference<>((ScrollBar) contentTable.lookup(".scroll-bar:vertical"));
        contentTable.addEventFilter(ScrollEvent.ANY, event -> {
            if (scrollBar.get() == null)
                scrollBar.set((ScrollBar) contentTable.lookup(".scroll-bar:vertical"));
            if (!scrollBar.get().isVisible()) {
                if (newDownloadBtn.getTranslateY() == 100)
                    newDownloadBtn.setTranslateY(0);
                return;
            }

            if (newDownloadBtn.getTranslateY() == 100 && event.getDeltaY() > 0) {
                transition.setFromY(100);
                transition.setToY(0);
                transition.play();
            }
            if (newDownloadBtn.getTranslateY() == 0 && event.getDeltaY() < 0) {
                transition.setFromY(0);
                transition.setToY(100);
                transition.play();
            }
        });
        stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            var sb = (ScrollBar) contentTable.lookup(".scroll-bar:vertical");
            if (sb != null && !sb.isVisible()) {
                if (newDownloadBtn.getTranslateY() == 100)
                    newDownloadBtn.setTranslateY(0);
            }
        });
    }

    @FXML
    private void onClearSearchBtn() {
        searchField.setText("");
        hideClearSearchBtn(true);
    }

    private void hideClearSearchBtn(boolean hide) {
        clearSearchBtn.setVisible(!hide);
        clearSearchBtn.setManaged(!hide);
        clearSearchBtn.setDisable(hide);
    }


    @FXML
    private void onNewDownload() {
        DownloadOpUtils.newDownload(true);
    }

    @Override
    public void updateQueue() {
        var queues = QueueSubject.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, true);
        if (!queues.contains(selectedQueue))
            // when delete happens
            initSides();
        else
            // when add happens
            SideUtils.prepareSideTree(sideTree, queues, searchField);
        MenuUtils.initOperationMenu(operationMenu);
    }

}
