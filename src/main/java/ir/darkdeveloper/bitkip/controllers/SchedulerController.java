package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.utils.InputValidations;
import ir.darkdeveloper.bitkip.utils.ResizeUtil;
import ir.darkdeveloper.bitkip.utils.WindowUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.FxUtils.SCHEDULER_STAGE;
import static ir.darkdeveloper.bitkip.utils.FxUtils.openStages;

public class SchedulerController implements FXMLController, QueueObserver {

    @FXML
    private HBox horLine1;
    @FXML
    private CheckBox enableToggle;
    @FXML
    private Spinner<Integer> hourSpinner;
    @FXML
    private Spinner<Integer> minuteSpinner;
    @FXML
    private Spinner<Integer> secondSpinner;
    @FXML
    private VBox mainBox;
    @FXML
    private HBox toolbar;
    @FXML
    private ImageView logoImg;
    @FXML
    private Label toolbarTitle;
    @FXML
    private Button hideBtn;
    @FXML
    private Button fullWindowBtn;
    @FXML
    private Button closeBtn;
    @FXML
    private ListView<QueueModel> queueList;
    private Stage stage;
    private final ObjectProperty<QueueModel> selectedQueue = new SimpleObjectProperty<>();
    private Rectangle2D bounds;


    @Override
    public void initialize() {
        closeBtn.setGraphic(new FontIcon());
        fullWindowBtn.setGraphic(new FontIcon());
        hideBtn.setGraphic(new FontIcon());
        bounds = Screen.getPrimary().getVisualBounds();
        initQueuesList();
        selectedQueue.addListener((ob, old, newVal) -> initSelectedQueueData());
        initSpinners();
    }

    private void initSpinners() {
        var hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 15);
        var minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 30);
        var secondFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        hourSpinner.setValueFactory(hourFactory);
        minuteSpinner.setValueFactory(minuteFactory);
        secondSpinner.setValueFactory(secondFactory);

        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);
        secondSpinner.setEditable(true);

        InputValidations.validIntInputCheck(hourSpinner.getEditor(), 15);
        InputValidations.validIntInputCheck(minuteSpinner.getEditor(), 30);
        InputValidations.validIntInputCheck(secondSpinner.getEditor(), 0);

        hourSpinner.getEditor().textProperty().addListener((o, o2, n) -> {
            if (n == null)
                return;
            if (n.isBlank())
                hourSpinner.getEditor().setText("0");
            if (!n.isBlank() && Integer.parseInt(n) > 23)
                hourSpinner.getEditor().setText("23");
        });
        minuteSpinner.getEditor().textProperty().addListener((o, o2, n) -> {
            if (n == null)
                return;
            if (n.isBlank())
                minuteSpinner.getEditor().setText("0");
            if (!n.isBlank() && Integer.parseInt(n) > 59)
                minuteSpinner.getEditor().setText("59");
        });
        secondSpinner.getEditor().textProperty().addListener((o, o2, n) -> {
            if (n == null)
                return;
            if (n.isBlank())
                secondSpinner.getEditor().setText("0");
            if (!n.isBlank() && Integer.parseInt(n) > 59)
                secondSpinner.getEditor().setText("59");
        });
    }

    @Override
    public void initAfterStage() {

        stage.widthProperty().addListener((o, o2, n) -> {
            var width = n.longValue();
            toolbar.setPrefWidth(width);
            horLine1.setPrefWidth(width);
        });

        stage.xProperty().addListener((observable, oldValue, newValue) -> {
            if (WindowUtils.isOnPrimaryScreen(newValue.doubleValue()))
                bounds = Screen.getPrimary().getVisualBounds();
        });
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null) {
            var img = new Image(logoPath.toExternalForm());
            logoImg.setImage(img);
            stage.getIcons().add(img);
        }

        WindowUtils.toolbarInits(toolbar, stage, bounds, newDownloadMinWidth, newDownloadMinHeight);
        WindowUtils.onToolbarDoubleClicked(toolbar, stage, null, bounds, null, newDownloadMinWidth, newDownloadMinHeight);
        ResizeUtil.addResizeListener(stage);
    }

    public void setSelectedQueue(QueueModel selectedQueue) {
        this.selectedQueue.set(selectedQueue);
    }

    private void initSelectedQueueData() {
        toolbarTitle.setText("Scheduler: %s".formatted(selectedQueue.get().getName()));
        stage.setTitle("Scheduler: %s".formatted(selectedQueue.get().getName()));
        queueList.getSelectionModel().select(selectedQueue.get());
//        mainBox.setDisable(selectedQueue.get().getIsScheduled());
//        enableToggle.setSelected(selectedQueue.get().getIsScheduled());
        mainBox.setDisable(true);
        enableToggle.setSelected(false);
    }


    private void initQueuesList() {
        queueList.getItems().clear();
        queueList.getItems().addAll(getQueues());
        queueList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        queueList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(QueueModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.getName() == null ? null : item.getName());
            }
        });
        queueList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY)
                selectedQueue.set(queueList.getSelectionModel().getSelectedItem());
        });
    }


    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @FXML
    private void closeStage() {
        openStages.remove(SCHEDULER_STAGE);
        stage.close();
    }

    @FXML
    private void hideStage() {
        stage.setIconified(true);
    }

    @FXML
    private void toggleStageSize() {
        bounds = WindowUtils.toggleWindowSize(stage, bounds, newDownloadMinWidth, newDownloadMinHeight);
    }


    @Override
    public void updateQueue() {
        initQueuesList();
    }

    @FXML
    private void onEnableToggle() {
        mainBox.setDisable(!mainBox.isDisabled());
    }

}
