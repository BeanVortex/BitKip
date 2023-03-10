package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.interfaces.NewDownloadFxmlController;
import ir.darkdeveloper.bitkip.utils.ResizeUtil;
import ir.darkdeveloper.bitkip.utils.MainTableUtils;
import ir.darkdeveloper.bitkip.utils.WindowUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

import static ir.darkdeveloper.bitkip.BitKip.getResource;

public class NewDownload implements NewDownloadFxmlController {
    @FXML
    private ImageView logoImg;
    @FXML
    private Button singleButton;
    @FXML
    private Button batchButton;
    @FXML
    private HBox toolbar;
    @FXML
    private Button hideBtn;
    @FXML
    private Button fullWindowBtn;
    @FXML
    private Button closeBtn;
    private Stage stage;

    private MainTableUtils mainTableUtils;
    private Rectangle2D bounds;
    private final int minWidth = 600, minHeight = 400;

    private boolean isSingle = true;

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    @Override
    public void setMainTableUtils(MainTableUtils mainTableUtils) {
        this.mainTableUtils = mainTableUtils;
        switchToSingleDownload();
    }

    @Override
    public void initAfterStage() {
        stage.widthProperty().addListener((ob, o, n) -> {
            var width = n.longValue();
            toolbar.setPrefWidth(width);
            var buttonWidth = width / 2;
            batchButton.setPrefWidth(buttonWidth);
            singleButton.setPrefWidth(buttonWidth);
        });

        var logoPath = getResource("icons/logo.png");
        if (logoPath != null){
            var img = new Image(logoPath.toExternalForm());
            logoImg.setImage(img);
            stage.getIcons().add(img);
        }

        var buttonWidth = stage.getWidth() / 2;
        singleButton.setPrefWidth(buttonWidth);
        batchButton.setPrefWidth(buttonWidth);

        stage.xProperty().addListener((observable, oldValue, newValue) -> {
            if (WindowUtils.isOnPrimaryScreen(newValue.doubleValue()))
                bounds = Screen.getPrimary().getVisualBounds();
        });

        WindowUtils.toolbarInits(toolbar, stage, bounds, minWidth, minHeight);
        WindowUtils.onToolbarDoubleClicked(toolbar, stage, null, bounds, null, minWidth, minHeight);
        ResizeUtil.addResizeListener(stage);

    }

    @Override
    public void initialize() {
        closeBtn.setGraphic(new FontIcon());
        fullWindowBtn.setGraphic(new FontIcon());
        hideBtn.setGraphic(new FontIcon());
        bounds = Screen.getPrimary().getVisualBounds();

    }

    @FXML
    private void hideWindowApp() {
        stage.setIconified(true);
    }

    @FXML
    private void toggleFullWindowApp() {
        bounds = WindowUtils.toggleWindowSize(stage, bounds, minWidth, minHeight);
    }

    @FXML
    private void closeApp() {
        stage.close();
    }

    @FXML
    private void showSingle() {
        if (!isSingle)
            switchToSingleDownload();
    }

    @FXML
    private void showBatch() {
        if (isSingle)
            switchToMultipleDownload();
    }

    private void switchToSingleDownload() {
        isSingle = true;
        singleButton.getStyleClass().add("tab_btn_selected");
        batchButton.getStyleClass().remove("tab_btn_selected");
        switchDownloadDetails("singleDownload");
    }

    private void switchToMultipleDownload() {
        isSingle = false;
        batchButton.getStyleClass().add("tab_btn_selected");
        singleButton.getStyleClass().remove("tab_btn_selected");
        switchDownloadDetails("batchDownload");
    }

    private void switchDownloadDetails(String fxmlName) {
        try {
            var loader = new FXMLLoader(getResource("fxml/" + fxmlName + ".fxml"));
            Parent details = loader.load();
            var root = (VBox) stage.getScene().getRoot();
            var rootChildren = root.getChildren();
            rootChildren.removeIf(node -> {
                if (node.getId() == null)
                    return false;
                return node.getId().equals("download_details");
            });
            NewDownloadFxmlController controller = loader.getController();
            var box = new VBox();
            box.getChildren().add(details);
            box.setId("download_details");
            controller.setStage(stage);
            controller.setMainTableUtils(mainTableUtils);
            AppConfigs.getQueueSubject().addObserver(controller);
            rootChildren.add(box);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void updateQueue() {

    }
}
