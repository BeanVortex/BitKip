package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.*;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;


public class MainController implements FXMLController, QueueObserver {

    @FXML
    private ImageView logoImg;
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
        mainTableUtils.tableInits();
        stage.widthProperty().addListener((ob, o, n) -> {
            contentTable.setPrefWidth(n.doubleValue() + 90);
            toolbar.setPrefWidth(n.longValue());
        });

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
        MenuUtils.initAboutMenu(aboutMenu, contentTable);
    }


    @Override
    public void initialize() {
        closeBtn.setGraphic(new FontIcon());
        fullWindowBtn.setGraphic(new FontIcon());
        hideBtn.setGraphic(new FontIcon());
        newDownloadBtn.setGraphic(new FontIcon());
        StackPane.setAlignment(newDownloadBtn, Pos.BOTTOM_RIGHT);
        bounds = Screen.getPrimary().getVisualBounds();
        mainBox.setPrefHeight(bounds.getHeight());
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
        side.getChildren().clear();
        var queues = AppConfigs.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getQueues();
        queues.forEach(queueModel -> {
            var btn = new Button(queueModel.getName());
            btn.getStyleClass().add("side_queue");
            if (queueModel.getName().equals("All Downloads"))
                btn.getStyleClass().add("selected_queue");
            btn.setPrefWidth(side.getPrefWidth());
            btn.setPrefHeight(60);
            btn.setOnMouseClicked(onSideQueueClicked(queueButtons, queueModel, btn));
            queueButtons.add(btn);
            side.getChildren().add(btn);
        });

    }

    private EventHandler<MouseEvent> onSideQueueClicked(ArrayList<Button> queueButtons,
                                                        QueueModel queueModel, Button btn) {
        return event -> {
            if (event.getClickCount() == 2)
                return;
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                currentDownloadings.forEach(DownloadsRepo::updateTableStatus);
                var downloadsData = DownloadsRepo.getDownloadsByQueue(queueModel.getId())
                        .stream().peek(downloadModel -> {
                            downloadModel.setDownloadStatus(DownloadStatus.Paused);
                            if (downloadModel.getProgress() == 100)
                                downloadModel.setDownloadStatus(DownloadStatus.Completed);
                        }).map(dm -> {
                            // this will make downloads in currentDownloadings, observed by table
                            if (currentDownloadings.contains(dm))
                                return currentDownloadings.get(currentDownloadings.indexOf(dm));
                            return dm;
                        }).toList();
                mainTableUtils.setDownloads(downloadsData);
                if (!queueButtons.isEmpty() && !btn.getStyleClass().contains("selected_queue")) {
                    btn.getStyleClass().add("selected_queue");
                    queueButtons.forEach(otherBtn -> {
                        if (!btn.equals(otherBtn))
                            otherBtn.getStyleClass().remove("selected_queue");
                    });
                }
            } else if (event.getButton().equals(MouseButton.SECONDARY)) {
                if (FileExtensions.staticQueueNames.stream().anyMatch(s -> btn.getText().equals(s)))
                    return;
                var cMenu = btn.getContextMenu();
                if (cMenu == null)
                    cMenu = new ContextMenu();
                cMenu.getItems().clear();
                var scheduleLbl = new Label("Change Schedule");
                var deleteLbl = new Label("Delete");

                var lbls = List.of(scheduleLbl, deleteLbl);
                var menuItems = MenuUtils.createMapMenuItems(lbls, null);
                cMenu.getItems().addAll(menuItems.values());
                btn.setContextMenu(cMenu);
                menuItems.get(scheduleLbl).setOnAction(e -> System.out.println("change schedule"));
                menuItems.get(deleteLbl).setOnAction(e -> {
                    QueuesRepo.deleteQueue(btn.getText());
                    AppConfigs.deleteQueue(btn.getText());
                    initialize();
                });
                cMenu.show(btn, Side.BOTTOM, 0, 0);
            }
        };
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
        initSides();
    }
}
