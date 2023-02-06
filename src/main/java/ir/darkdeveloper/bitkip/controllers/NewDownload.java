package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.utils.ResizeUtil;
import ir.darkdeveloper.bitkip.utils.TableUtils;
import ir.darkdeveloper.bitkip.utils.WindowUtils;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class NewDownload implements FXMLController {
    @FXML
    private Tab singleTab;
    @FXML
    private Tab multipleTab;
    @FXML
    private HBox toolbar;
    @FXML
    private Button hideBtn;
    @FXML
    private Button fullWindowBtn;
    @FXML
    private Button closeBtn;
    private Stage stage;

    private TableUtils tableUtils;
    private Rectangle2D bounds;
    private final int minWidth = 600, minHeight = 400;


    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    public void setTableUtils(TableUtils tableUtils) {
        this.tableUtils = tableUtils;
    }

    @Override
    public void initAfterStage() {
        stage.widthProperty().addListener((ob, o, n) -> toolbar.setPrefWidth(n.longValue()));
        stage.xProperty().addListener((observable, oldValue, newValue) -> {
            if (WindowUtils.isOnPrimaryScreen(newValue.doubleValue()))
                bounds = Screen.getPrimary().getVisualBounds();
        });
        WindowUtils.toolbarInits(toolbar, stage, bounds, minWidth, minHeight);
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
        var screenY = stage.getY();
        if (screenY - bounds.getMinY() >= 0 && bounds.getHeight() > stage.getHeight())
            bounds = WindowUtils.maximizeWindow(stage, bounds, null);
        else if (screenY - bounds.getMinY() <= 0 && bounds.getHeight() <= stage.getHeight())
            bounds = WindowUtils.minimizeWindow(stage, bounds, minWidth, minHeight);
    }

    @FXML
    private void closeApp() {
        stage.close();
    }
}
