package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.*;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.ALL_DOWNLOADS_QUEUE;


public class MainController implements FXMLController, QueueObserver {

    @FXML
    private TreeView<String> sideTree;
    @FXML
    private ImageView logoImg;
    @FXML
    private Button operationMenu;
    @FXML
    private Button moreBtn;
    @FXML
    private Button menuFile;
    @FXML
    private Button newDownloadBtn;
    @FXML
    private TableView<DownloadModel> contentTable;
    @FXML
    private HBox mainBox;
    @FXML
    private HBox toolbar;
    @FXML
    private Button closeBtn;
    @FXML
    private Button fullWindowBtn;
    @FXML
    private Button hideBtn;

    private Stage stage;
    private MainTableUtils mainTableUtils;
    private Rectangle2D bounds;

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

        stage.xProperty().addListener((observable, oldValue, newValue) -> {
            if (WindowUtils.isOnPrimaryScreen(newValue.doubleValue()))
                bounds = Screen.getPrimary().getVisualBounds();
        });
        newDownloadBtnInits();
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null) {
            var img = new Image(logoPath.toExternalForm());
            logoImg.setImage(img);
            stage.getIcons().add(img);
        }
        WindowUtils.toolbarInits(toolbar, stage, bounds, newDownloadBtn, mainPrefWidth, mainPrefHeight);
        WindowUtils.onToolbarDoubleClicked(toolbar, stage, contentTable, bounds, newDownloadBtn, mainPrefWidth, mainPrefHeight);
        MenuUtils.initFileMenu(menuFile, mainTableUtils);
        MenuUtils.initOperationMenu(operationMenu, mainTableUtils);
        MenuUtils.initMoreMenu(moreBtn, contentTable);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        closeBtn.setGraphic(new FontIcon());
        fullWindowBtn.setGraphic(new FontIcon());
        hideBtn.setGraphic(new FontIcon());
        newDownloadBtn.setGraphic(new FontIcon());
        StackPane.setAlignment(newDownloadBtn, Pos.BOTTOM_RIGHT);
        bounds = Screen.getPrimary().getVisualBounds();
        mainBox.setPrefHeight(bounds.getHeight());
    }

    private void initSides() {
        var allDownloadsQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, true);
        var downloadList = allDownloadsQueue.getDownloads().stream()
                .peek(dm -> {
                    dm.setDownloadStatus(DownloadStatus.Paused);
                    if (dm.getProgress() == 100)
                        dm.setDownloadStatus(DownloadStatus.Completed);
                })
                .toList();
        selectedQueue = allDownloadsQueue;
        mainTableUtils.setDownloads(downloadList, true);
        var queues = AppConfigs.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, true);
        SideUtils.prepareSideTree(sideTree, queues, mainTableUtils);

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
    private void closeApp() {
        Platform.exit();
    }

    @FXML
    private void hideWindowApp() {
        stage.setIconified(true);
    }

    @FXML
    private void toggleFullWindowApp() {
        var screenY = stage.getY();
        if (screenY - bounds.getMinY() >= 0 && bounds.getHeight() > stage.getHeight())
            bounds = WindowUtils.maximizeWindow(stage, bounds, newDownloadBtn);
        else if (screenY - bounds.getMinY() <= 0 && bounds.getHeight() <= stage.getHeight())
            bounds = WindowUtils.minimizeWindow(stage, bounds, mainPrefWidth, mainPrefHeight);

    }

    @FXML
    private void onNewDownload() {
        DownloadOpUtils.newDownload(mainTableUtils, true);
    }

    @Override
    public void updateQueue() {
        var queues = AppConfigs.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, true);
        if (!queues.contains(selectedQueue))
            // when delete happens
            initSides();
        else
            // when add happens
            SideUtils.prepareSideTree(sideTree, queues, mainTableUtils);
        MenuUtils.initOperationMenu(operationMenu, mainTableUtils);
    }
}
