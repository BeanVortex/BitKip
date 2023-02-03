package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Translate;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class MainController implements FXMLController {


    @FXML
    private Button actionBtn;
    @FXML
    private TableView<DownloadModel> contentTable;
    @FXML
    private TableColumn<DownloadModel, String> nameColumn;
    @FXML
    private TableColumn<DownloadModel, Double> progressColumn;
    @FXML
    private TableColumn<DownloadModel, String> sizeColumn;
    @FXML
    private TableColumn<DownloadModel, Integer> remainingColumn;
    @FXML
    private TableColumn<DownloadModel, Integer> chunksColumn;
    @FXML
    private TableColumn<DownloadModel, String> addDateColumn;
    @FXML
    private TableColumn<DownloadModel, String> lastTryColumn;
    @FXML
    private TableColumn<DownloadModel, String> completeColumn;
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
    private static double xOffset = 0;
    private static double yOffset = 0;
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

    public void initAfterStage() {
        stage.widthProperty().addListener((ob, o, n) -> {
            contentTable.setPrefWidth(n.doubleValue() + 90);
            toolbar.setPrefWidth(n.longValue());
        });
        stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            var scrollBar = (ScrollBar) contentTable.lookup(".scroll-bar:vertical");
            if (scrollBar != null && !scrollBar.isVisible()) {
                if (actionBtn.getTranslateY() == 100)
                    actionBtn.setTranslateY(0);
            }
        });
        stage.xProperty().addListener((observable, oldValue, newValue) -> {
            if (isOnPrimaryScreen(newValue.doubleValue()))
                bounds = Screen.getPrimary().getBounds();
        });
    }


    @Override
    public void initialize() {
        closeBtn.setGraphic(new FontIcon());
        fullWindowBtn.setGraphic(new FontIcon());
        hideBtn.setGraphic(new FontIcon());
        actionBtn.setGraphic(new FontIcon());
        StackPane.setAlignment(actionBtn, Pos.BOTTOM_RIGHT);
        var screen = Screen.getPrimary();
        bounds = screen.getVisualBounds();
        mainBox.setPrefHeight(bounds.getHeight());
        toolbarInits();
        tableInits();
        actionBtnInits();
    }

    private void toolbarInits() {
        toolbar.setOnMousePressed(event -> {
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });
        toolbar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
            if (stage.getWidth() == bounds.getWidth() && stage.getHeight() == bounds.getHeight())
                minimizeWindow();
        });
        toolbar.setOnMouseReleased(event -> {
            var screenY = event.getScreenY();
            if (screenY <= 0)
                maximizeWindow();
        });
        toolbar.setOnMouseClicked(event -> {
            var doubleClickCondition = event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2;
            var screenY = stage.getY();
            if (screenY > 0 && doubleClickCondition)
                maximizeWindow();
            else if (screenY <= 0 && doubleClickCondition)
                minimizeWindow();
        });
    }

    private void tableInits() {
        nameColumn.setCellValueFactory(p -> p.getValue().getNameProperty());
        progressColumn.setCellValueFactory(p -> p.getValue().getProgressProperty().asObject());
        sizeColumn.setCellValueFactory(p -> p.getValue().getSizeProperty());
        remainingColumn.setCellValueFactory(p -> p.getValue().getRemainingTimeProperty().asObject());
        chunksColumn.setCellValueFactory(p -> p.getValue().getChunksProperty().asObject());
        addDateColumn.setCellValueFactory(p -> p.getValue().getAddDateProperty());
        lastTryColumn.setCellValueFactory(p -> p.getValue().getLastTryDateProperty());
        completeColumn.setCellValueFactory(p -> p.getValue().getCompleteDateProperty());
        contentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        var data = FXCollections.<DownloadModel>observableArrayList();
        for (int i = 0; i < 30; i++) {
            var dow = DownloadModel.builder()
                    .id(UUID.randomUUID().toString())
                    .name("ffd")
                    .progress(45.6)
                    .size("16")
                    .url("adsf")
                    .filePath("sdf")
                    .remainingTime(56)
                    .addDate(LocalDateTime.now())
                    .lastTryDate(LocalDateTime.now().plusHours(1))
                    .completeDate(LocalDateTime.now().plusHours(2))
                    .chunks(3)
                    .build();
            dow.fillProperties();
            data.add(dow);
        }
        contentTable.getItems().addAll(data);

        contentTable.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.SECONDARY)) {
                var selectedItems = contentTable.getSelectionModel().getSelectedItems();
                if (selectedItems.size() > 1)
                    selectedItems.forEach(downloadModel -> {
                        downloadModel.getNameProperty().setValue("df");
                        downloadModel.getProgressProperty().setValue(160.5);
                        downloadModel.getCompleteDateProperty().setValue(LocalDateTime.now().toString());
                    });
            }
        });
        var translate = new Translate();
        actionBtn.getTransforms().add(translate);
        contentTable.setRowFactory(param -> {
            var row = new TableRow<DownloadModel>();
            var selectedItems = contentTable.getSelectionModel().getSelectedItems();
            if (selectedItems.size() == 1) {
                row.setOnMouseClicked(event -> {
                    if (!row.isEmpty() && event.getButton().equals(MouseButton.SECONDARY)) {
                        System.out.println(row.getIndex());
                        actionBtn.setRotate(15);
                    }
                });
            }
            return row;
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
    }

    private void maximizeWindow() {
        var currentX = stage.getX();
        Screen.getScreens().forEach(screen -> {
            if (!isOnPrimaryScreen(currentX))
                bounds = screen.getBounds();
            maximizeStage();
        });
    }

    private void minimizeWindow() {
        var currentX = stage.getX();
        Screen.getScreens().forEach(screen -> {
            if (!isOnPrimaryScreen(currentX))
                bounds = screen.getBounds();
            minimizeStage();
        });
    }

    private void maximizeStage() {
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        if (actionBtn.getTranslateY() == 100)
            actionBtn.setTranslateY(0);
    }

    private void minimizeStage() {
        var width = 853;
        var height = 515;
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setY((bounds.getMaxY() - height) / 2);
        stage.setX((bounds.getMaxX() - width) / 2);
    }


    public void closeApp() {
        Platform.exit();
    }

    public void hideWindowApp() {
        stage.setIconified(true);
    }

    public void toggleFullWindowApp() {
        var screenY = stage.getY();
        if (screenY > 0)
            maximizeWindow();
        else if (screenY <= 0)
            minimizeWindow();

    }

    private boolean isOnPrimaryScreen(double x) {
        var bounds = Screen.getPrimary().getBounds();
        return x < bounds.getMaxX();
    }
}
