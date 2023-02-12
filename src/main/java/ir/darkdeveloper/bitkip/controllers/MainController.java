package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.task.DownloadTask;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.MenuUtils;
import ir.darkdeveloper.bitkip.utils.TableUtils;
import ir.darkdeveloper.bitkip.utils.WindowUtils;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MainController implements FXMLController {


    @FXML
    private ScrollPane sideScrollPane;
    @FXML
    private VBox side;
    @FXML
    private Button operationMenu;
    @FXML
    private Button aboutMenu;
    @FXML
    private Button menuFile;
    @FXML
    private Button actionBtn;
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
    private TableUtils tableUtils;
    private Rectangle2D bounds;
    private final int minWidth = 853, minHeight = 515;
    private final List<DownloadTask> downloadTaskList = new ArrayList<>();

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
        stage.widthProperty().addListener((ob, o, n) -> {
            contentTable.setPrefWidth(n.doubleValue() + 90);
            toolbar.setPrefWidth(n.longValue());
        });

        stage.xProperty().addListener((observable, oldValue, newValue) -> {
            if (WindowUtils.isOnPrimaryScreen(newValue.doubleValue()))
                bounds = Screen.getPrimary().getVisualBounds();
        });
        actionBtnInits();
        WindowUtils.toolbarInits(toolbar, stage, bounds, actionBtn, contentTable, minWidth, minHeight);
        MenuUtils.initFileMenu(menuFile, contentTable);
        MenuUtils.initOperationMenu(operationMenu, contentTable);
        MenuUtils.initAboutMenu(aboutMenu, contentTable);
    }


    @Override
    public void initialize() {
        closeBtn.setGraphic(new FontIcon());
        fullWindowBtn.setGraphic(new FontIcon());
        hideBtn.setGraphic(new FontIcon());
        actionBtn.setGraphic(new FontIcon());
        StackPane.setAlignment(actionBtn, Pos.BOTTOM_RIGHT);
        bounds = Screen.getPrimary().getVisualBounds();
        mainBox.setPrefHeight(bounds.getHeight());
        tableUtils = new TableUtils(contentTable);
        tableUtils.tableInits();
        initSides();
    }

    private void initSides() {
        var vScrollBar = new ScrollBar();
        vScrollBar.setOrientation(Orientation.VERTICAL);
        vScrollBar.minProperty().bind(sideScrollPane.vminProperty());
        vScrollBar.maxProperty().bind(sideScrollPane.vmaxProperty());
        vScrollBar.visibleAmountProperty().bind(sideScrollPane.heightProperty().divide(side.heightProperty()));
        sideScrollPane.vvalueProperty().bindBidirectional(vScrollBar.valueProperty());

        var hScrollBar = new ScrollBar();
        hScrollBar.setOrientation(Orientation.HORIZONTAL);
        hScrollBar.minProperty().bind(sideScrollPane.hminProperty());
        hScrollBar.maxProperty().bind(sideScrollPane.hmaxProperty());
        hScrollBar.visibleAmountProperty().bind(sideScrollPane.widthProperty().divide(side.heightProperty()));
        sideScrollPane.hvalueProperty().bindBidirectional(hScrollBar.valueProperty());

        sideScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sideScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sideScrollPane.setPadding(Insets.EMPTY);
        var queueButtons = new ArrayList<Button>();
        QueuesRepo.getQueues().forEach(queueModel -> {
            var btn = new Button(queueModel.getName());
            btn.getStyleClass().add("side_queue");
            if (queueModel.getName().equals("All Downloads"))
                btn.getStyleClass().add("selected_queue");
            btn.setPrefWidth(side.getPrefWidth());
            btn.setPrefHeight(60);
            btn.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2)
                    return;
                var downloadsData = DownloadsRepo.getDownloadsByQueue(queueModel.getId());
                tableUtils.setDownloads(downloadsData);
                if (!queueButtons.isEmpty()) {
                    btn.getStyleClass().add("selected_queue");
                    queueButtons.forEach(otherBtn -> {
                        if (!btn.equals(otherBtn))
                            otherBtn.getStyleClass().remove("selected_queue");
                    });
                }
            });
            queueButtons.add(btn);
            side.getChildren().add(btn);
        });


    }


    private void actionBtnInits() {
        var transition = new TranslateTransition(Duration.millis(300), actionBtn);
        var scrollBar = new AtomicReference<>((ScrollBar) contentTable.lookup(".scroll-bar:vertical"));
        contentTable.addEventFilter(ScrollEvent.ANY, event -> {
            if (scrollBar.get() == null)
                scrollBar.set((ScrollBar) contentTable.lookup(".scroll-bar:vertical"));
            if (!scrollBar.get().isVisible()) {
                if (actionBtn.getTranslateY() == 100)
                    actionBtn.setTranslateY(0);
                return;
            }

            if (actionBtn.getTranslateY() == 100 && event.getDeltaY() > 0) {
                transition.setFromY(100);
                transition.setToY(0);
                transition.play();
            }
            if (actionBtn.getTranslateY() == 0 && event.getDeltaY() < 0) {
                transition.setFromY(0);
                transition.setToY(100);
                transition.play();
            }
        });
        stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            var sb = (ScrollBar) contentTable.lookup(".scroll-bar:vertical");
            if (sb != null && !sb.isVisible()) {
                if (actionBtn.getTranslateY() == 100)
                    actionBtn.setTranslateY(0);
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
            bounds = WindowUtils.maximizeWindow(stage, bounds, actionBtn);
        else if (screenY - bounds.getMinY() <= 0 && bounds.getHeight() <= stage.getHeight())
            bounds = WindowUtils.minimizeWindow(stage, bounds, minWidth, minHeight);

    }

    @FXML
    private void doAction() {
        contentTable.getSelectionModel().clearSelection();
//        var dow = DownloadModel.builder()
//                .name("avvv")
//                .progress(10)
//                .size(20)
//                .url("sdfads")
//                .filePath("fsdaf")
//                .addDate(LocalDateTime.now().plusSeconds(70))
//                .lastTryDate(LocalDateTime.now().plusHours(1))
//                .completeDate(LocalDateTime.now().plusHours(2))
//                .chunks(10)
//                .build();
//        dow.fillProperties();
//        DownloadsRepo.insertDownload(dow);
        FxUtils.newDownloadStage("newDownload.fxml", 600, 550, tableUtils, downloadTaskList);
//        tableUtils.addRow(dow);
    }
}
