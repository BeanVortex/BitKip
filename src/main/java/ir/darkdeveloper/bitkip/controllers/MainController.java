package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.utils.WindowUtils;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Translate;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class MainController implements FXMLController {


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

        stage.xProperty().addListener((observable, oldValue, newValue) -> {
            if (WindowUtils.isOnPrimaryScreen(newValue.doubleValue()))
                bounds = Screen.getPrimary().getVisualBounds();
        });
        actionBtnInits();
        WindowUtils.toolbarInits(toolbar, stage, bounds, actionBtn, contentTable);
        WindowUtils.initFileMenu(menuFile, contentTable);
        WindowUtils.initOperationMenu(operationMenu, contentTable);
        WindowUtils.initAboutMenu(aboutMenu, contentTable);
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
        tableInits();

    }


    private void tableInits() {
        nameColumn.setCellValueFactory(p -> p.getValue().getNameProperty());
        sizeColumn.setCellValueFactory(p -> p.getValue().getSizeProperty());
        progressColumn.setCellValueFactory(p -> p.getValue().getProgressProperty().asObject());
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
        contentTable.setRowFactory(param -> {
            var row = new TableRow<DownloadModel>();
            row.setOnMouseClicked(event -> {
                var selectedItems = contentTable.getSelectionModel().getSelectedItems();
                if (selectedItems.size() == 1 && !row.isEmpty()
                        && event.getButton().equals(MouseButton.SECONDARY))
                    System.out.println(row.getIndex());
            });
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
        stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            var sb = (ScrollBar) contentTable.lookup(".scroll-bar:vertical");
            if (sb != null && !sb.isVisible()) {
                if (actionBtn.getTranslateY() == 100)
                    actionBtn.setTranslateY(0);
            }
        });
    }

    public void closeApp() {
        Platform.exit();
    }

    public void hideWindowApp() {
        stage.setIconified(true);
    }

    public void toggleFullWindowApp() {
        var screenY = stage.getY();
        if (screenY - bounds.getMinY() >= 0 && bounds.getHeight() > stage.getHeight())
            bounds = WindowUtils.maximizeWindow(stage, bounds, actionBtn);
       else if (screenY - bounds.getMinY() <= 0 && bounds.getHeight() <= stage.getHeight())
            bounds = WindowUtils.minimizeWindow(stage, bounds);

    }

    public void doAction() {
        contentTable.getSelectionModel().clearSelection();
    }
}
