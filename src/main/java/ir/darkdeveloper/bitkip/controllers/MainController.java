package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.instancio.Instancio;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class MainController implements FXMLController {

    // Todo: download dto
    public static final String DATE_FORMAT = "EE MMM dd yyyy HH:mm:ss";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

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

    @Override
    public void initialize() {
        closeBtn.setGraphic(new FontIcon());
        fullWindowBtn.setGraphic(new FontIcon());
        hideBtn.setGraphic(new FontIcon());
        var screen = Screen.getPrimary();
        bounds = screen.getVisualBounds();
        mainBox.setPrefHeight(bounds.getHeight());
        toolbarInits();
        tableInits();
    }

    private void tableInits() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progress"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        remainingColumn.setCellValueFactory(new PropertyValueFactory<>("remainingTime"));
        chunksColumn.setCellValueFactory(new PropertyValueFactory<>("chunks"));
        addDateColumn.setCellValueFactory(new PropertyValueFactory<>("addDate"));
        lastTryColumn.setCellValueFactory(new PropertyValueFactory<>("lastTryDate"));
        completeColumn.setCellValueFactory(new PropertyValueFactory<>("completeDate"));
        contentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        var dow = DownloadModel.builder()
                .id(UUID.randomUUID().toString())
                .name("sdf")
                .progress(156)
                .size("16")
                .url("adsf")
                .filePath("sdf")
                .remainingTime(56)
                .addDate(LocalDateTime.now())
                .lastTryDate(LocalDateTime.now().plusHours(1))
                .completeDate(LocalDateTime.now().plusHours(2))
                .chunks(3)
                .build();
        System.out.println(dow);
        contentTable.getItems().add(dow);
        contentTable.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.SECONDARY)) {
                contentTable.getSelectionModel().getSelectedItems()
                                .forEach(downloadModel -> System.out.println(downloadModel.getName()));
            }
        });
    }

    public void initAfterStage() {
        stage.widthProperty().addListener((ob, o, n) -> contentTable.setPrefWidth(n.doubleValue() - 100));
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
            else if (screenY == 0 && doubleClickCondition)
                minimizeWindow();
        });
    }

    private void maximizeWindow() {
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    private void minimizeWindow() {
        var width = 850;
        var height = 512;
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
        else if (screenY == 0)
            minimizeWindow();

    }
}
