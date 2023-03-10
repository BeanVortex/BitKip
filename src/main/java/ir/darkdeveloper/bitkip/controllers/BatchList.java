package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.task.LinkDataTask;
import ir.darkdeveloper.bitkip.utils.LinkTableUtils;
import ir.darkdeveloper.bitkip.utils.ResizeUtil;
import ir.darkdeveloper.bitkip.utils.WindowUtils;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.BitKip.getResource;

public class BatchList implements FXMLController {
    @FXML
    private Button addBtn;
    @FXML
    private HBox toolbar;
    @FXML
    private ImageView logoImg;
    @FXML
    private Button closeBtn;
    @FXML
    private HBox mainBox;
    @FXML
    private TableView<LinkModel> linkTable;
    private Stage stage;

    private LinkTableUtils linkTableUtils;
    private Rectangle2D bounds;


    @Override
    public void initialize() {
        closeBtn.setGraphic(new FontIcon());
        addBtn.requestFocus();
        bounds = Screen.getPrimary().getVisualBounds();
        mainBox.setPrefHeight(bounds.getHeight());
        linkTable.setPrefWidth(bounds.getWidth());
    }


    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    public void setData(List<LinkModel> links) {
        linkTableUtils = new LinkTableUtils(linkTable, links);
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
                            executor::shutdown
                    );
                }));
        new Thread(linkTask).start();
    }

    @Override
    public void initAfterStage() {
        stage.widthProperty().addListener((ob, o, n) -> {
            linkTable.setPrefWidth(n.doubleValue() + 90);
            toolbar.setPrefWidth(n.longValue());
        });
        stage.xProperty().addListener((o) -> bounds = Screen.getPrimary().getVisualBounds());
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null) {
            var img = new Image(logoPath.toExternalForm());
            logoImg.setImage(img);
            stage.getIcons().add(img);
        }
        int minHeight = 400;
        int minWidth = 700;
        WindowUtils.toolbarInits(toolbar, stage, bounds, minWidth, minHeight);
        ResizeUtil.addResizeListener(stage);
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @FXML
    private void close() {
        stage.close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    @FXML
    private void onAdd() {
        var links = linkTableUtils.getLinks();
    }
}
