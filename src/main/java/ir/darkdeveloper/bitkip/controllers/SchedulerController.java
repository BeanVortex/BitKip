package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.TurnOffModel;
import ir.darkdeveloper.bitkip.utils.InputValidations;
import ir.darkdeveloper.bitkip.utils.ResizeUtil;
import ir.darkdeveloper.bitkip.utils.WindowUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
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
    private ComboBox<TurnOffModel> powerCombo;
    @FXML
    private CheckBox whenDoneCheck;
    @FXML
    private Spinner<Integer> stopHourSpinner;
    @FXML
    private Spinner<Integer> stopMinuteSpinner;
    @FXML
    private Spinner<Integer> stopSecondSpinner;
    @FXML
    private HBox stopContainer;
    @FXML
    private CheckBox stopAtCheck;
    @FXML
    private CheckBox saturdayCheck;
    @FXML
    private CheckBox sundayCheck;
    @FXML
    private CheckBox mondayCheck;
    @FXML
    private CheckBox tuesdayCheck;
    @FXML
    private CheckBox wednesdayCheck;
    @FXML
    private CheckBox thursdayCheck;
    @FXML
    private CheckBox fridayCheck;
    @FXML
    private RadioButton onceRadio;
    @FXML
    private RadioButton dailyRadio;
    @FXML
    private GridPane weeksContainer;
    @FXML
    private DatePicker datePicker;
    @FXML
    private HBox horLine1;
    @FXML
    private HBox horLine2;
    @FXML
    private CheckBox enableToggle;
    @FXML
    private Spinner<Integer> startHourSpinner;
    @FXML
    private Spinner<Integer> startMinuteSpinner;
    @FXML
    private Spinner<Integer> startSecondSpinner;
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
        initRadios();
        initPowerCombo();
    }

    private void initPowerCombo() {
        var items = FXCollections.observableArrayList(TurnOffModel.TURN_OFF, TurnOffModel.SLEEP, TurnOffModel.HIBERNATE);
        powerCombo.setItems(items);
        powerCombo.getSelectionModel().select(0);
        powerCombo.setDisable(true);
    }

    private void initRadios() {
        var tg = new ToggleGroup();
        onceRadio.setToggleGroup(tg);
        dailyRadio.setToggleGroup(tg);
        dailyRadio.selectedProperty().addListener(o -> {
            datePicker.setDisable(true);
            weeksContainer.setDisable(false);
        });
        onceRadio.selectedProperty().addListener(o -> {
            datePicker.setDisable(false);
            weeksContainer.setDisable(true);
        });
        dailyRadio.setSelected(true);
    }

    private void initSpinners() {
        var hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 15);
        var minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 30);
        var secondFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        startHourSpinner.setValueFactory(hourFactory);
        startMinuteSpinner.setValueFactory(minuteFactory);
        startSecondSpinner.setValueFactory(secondFactory);
        stopHourSpinner.setValueFactory(hourFactory);
        stopMinuteSpinner.setValueFactory(minuteFactory);
        stopSecondSpinner.setValueFactory(secondFactory);

        startHourSpinner.setEditable(true);
        startMinuteSpinner.setEditable(true);
        startSecondSpinner.setEditable(true);
        stopHourSpinner.setEditable(true);
        stopMinuteSpinner.setEditable(true);
        stopSecondSpinner.setEditable(true);

        InputValidations.validTimePickerInputs(startHourSpinner, startMinuteSpinner, startSecondSpinner);
        InputValidations.validTimePickerInputs(stopHourSpinner, stopMinuteSpinner, stopSecondSpinner);

    }

    @Override
    public void initAfterStage() {

        stage.widthProperty().addListener((o, o2, n) -> {
            var width = n.longValue();
            toolbar.setPrefWidth(width);
            horLine1.setPrefWidth(width);
            horLine2.setPrefWidth(width);
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
        stopContainer.setDisable(true);
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

    @FXML
    private void onStopAtChecked() {
        stopContainer.setDisable(!stopContainer.isDisabled());
    }

    @FXML
    private void onWhenDoneChecked() {
        powerCombo.setDisable(!powerCombo.isDisabled());
    }

    @FXML
    private void onPowerCombo() {

    }

    @FXML
    private void onReset() {
    }

    @FXML
    private void onCancel() {
    }

    @FXML
    private void onSave() {
    }
}
