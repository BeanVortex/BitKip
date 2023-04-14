package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.models.TurnOffMode;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.repo.ScheduleRepo;
import ir.darkdeveloper.bitkip.task.ScheduleTask;
import ir.darkdeveloper.bitkip.utils.*;
import javafx.application.Platform;
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
import javafx.scene.paint.Paint;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.repo.QueuesRepo.*;
import static ir.darkdeveloper.bitkip.utils.FxUtils.SCHEDULER_STAGE;
import static ir.darkdeveloper.bitkip.utils.FxUtils.openStages;
import static java.time.DayOfWeek.*;

public class QueueSetting implements FXMLController, QueueObserver {

    @FXML
    private CheckBox hasFolderCheck;
    @FXML
    private VBox rightContainer;
    @FXML
    private Label savedLabel;
    @FXML
    private Spinner<Integer> simulDownloadSpinner;
    @FXML
    private TextField speedField;
    @FXML
    private ComboBox<TurnOffMode> powerCombo;
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
    private CheckBox enableCheck;
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
        initInputs();
        initRadios();
        initPowerCombo();
    }

    private void initPowerCombo() {
        var items = FXCollections.observableArrayList(TurnOffMode.TURN_OFF, TurnOffMode.SLEEP, TurnOffMode.HIBERNATE);
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

    private void initInputs() {
        var simulDownloadFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1);
        var startHourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 15);
        var startMinuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 30);
        var startSecondFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);

        var stopHourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 15);
        var stopMinuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 30);
        var stopSecondFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);

        startHourSpinner.setValueFactory(startHourFactory);
        startMinuteSpinner.setValueFactory(startMinuteFactory);
        startSecondSpinner.setValueFactory(startSecondFactory);
        stopHourSpinner.setValueFactory(stopHourFactory);
        stopMinuteSpinner.setValueFactory(stopMinuteFactory);
        stopSecondSpinner.setValueFactory(stopSecondFactory);
        simulDownloadSpinner.setValueFactory(simulDownloadFactory);

        startHourSpinner.setEditable(true);
        startMinuteSpinner.setEditable(true);
        startSecondSpinner.setEditable(true);
        stopHourSpinner.setEditable(true);
        stopMinuteSpinner.setEditable(true);
        stopSecondSpinner.setEditable(true);
        simulDownloadSpinner.setEditable(true);

        InputValidations.validTimePickerInputs(startHourSpinner, startMinuteSpinner, startSecondSpinner);
        InputValidations.validTimePickerInputs(stopHourSpinner, stopMinuteSpinner, stopSecondSpinner);
        InputValidations.validSpeedInputChecks(speedField);
        InputValidations.validIntInputCheck(simulDownloadSpinner.getEditor(), 1, 1, 5);


        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                }
            }
        });
    }

    @Override
    public void initAfterStage() {

        stage.widthProperty().addListener((o, o2, n) -> {
            var width = n.longValue();
            toolbar.setPrefWidth(width);
            horLine1.setPrefWidth(width);
            horLine2.setPrefWidth(width);
            rightContainer.setPrefWidth(width - queueList.getPrefWidth());
        });

        stage.heightProperty().addListener((o, o2, n) -> {
            var height = n.longValue();
            queueList.setPrefHeight(height);
            rightContainer.setPrefHeight(height - 32 - 10);
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

        speedField.setText(selectedQueue.get().getSpeed());
        simulDownloadSpinner.getValueFactory().setValue(selectedQueue.get().getSimultaneouslyDownload());
        hasFolderCheck.setSelected(selectedQueue.get().hasFolder());

        toolbarTitle.setText("Queue Setting: %s".formatted(selectedQueue.get().getName()));
        stage.setTitle("Queue Setting: %s".formatted(selectedQueue.get().getName()));
        queueList.getSelectionModel().select(selectedQueue.get());
        datePicker.setValue(LocalDate.now());

        var schedule = selectedQueue.get().getSchedule();
        enableCheck.setSelected(schedule.isEnabled());
        mainBox.setDisable(!schedule.isEnabled());
        var startTime = schedule.getStartTime();
        if (startTime != null) {
            startHourSpinner.getValueFactory().setValue(startTime.getHour());
            startMinuteSpinner.getValueFactory().setValue(startTime.getMinute());
            startSecondSpinner.getValueFactory().setValue(startTime.getSecond());
        }

        onceRadio.setSelected(schedule.isOnceDownload());
        dailyRadio.setSelected(!schedule.isOnceDownload());

        schedule.getDays().forEach(day -> {
            switch (day) {
                case SATURDAY -> saturdayCheck.setSelected(true);
                case SUNDAY -> sundayCheck.setSelected(true);
                case MONDAY -> mondayCheck.setSelected(true);
                case TUESDAY -> tuesdayCheck.setSelected(true);
                case WEDNESDAY -> wednesdayCheck.setSelected(true);
                case THURSDAY -> thursdayCheck.setSelected(true);
                case FRIDAY -> fridayCheck.setSelected(true);
            }
        });

        var startDate = schedule.getStartDate();
        if (startDate != null)
            datePicker.setValue(startDate);

        stopAtCheck.setSelected(schedule.isStopTimeEnabled());
        stopContainer.setDisable(!schedule.isStopTimeEnabled());
        var stopTime = schedule.getStopTime();
        if (stopTime != null) {
            stopHourSpinner.getValueFactory().setValue(stopTime.getHour());
            stopMinuteSpinner.getValueFactory().setValue(stopTime.getMinute());
            stopSecondSpinner.getValueFactory().setValue(stopTime.getSecond());
        }
        whenDoneCheck.setSelected(schedule.isTurnOffEnabled());
        powerCombo.setDisable(!schedule.isTurnOffEnabled());
        if (schedule.getTurnOffMode() != null)
            powerCombo.setValue(schedule.getTurnOffMode());


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
    private void onEnableCheck() {
        mainBox.setDisable(!enableCheck.isSelected());
    }

    @FXML
    private void onStopAtChecked() {
        stopContainer.setDisable(!stopAtCheck.isSelected());
    }

    @FXML
    private void onWhenDoneChecked() {
        powerCombo.setDisable(!whenDoneCheck.isSelected());
    }


    @FXML
    private void onReset() {
        initSelectedQueueData();
    }

    @FXML
    private void onCancel() {
        stage.close();
    }

    @FXML
    private void onSave() {
        var executor = Executors.newCachedThreadPool();
        try {
            var schedule = new ScheduleModel();
            var queue = selectedQueue.get();
            schedule.setId(queue.getSchedule().getId());
            var days = new HashSet<DayOfWeek>();
            if (saturdayCheck.isSelected())
                days.add(SATURDAY);
            if (sundayCheck.isSelected())
                days.add(SUNDAY);
            if (mondayCheck.isSelected())
                days.add(MONDAY);
            if (tuesdayCheck.isSelected())
                days.add(TUESDAY);
            if (wednesdayCheck.isSelected())
                days.add(WEDNESDAY);
            if (thursdayCheck.isSelected())
                days.add(THURSDAY);
            if (fridayCheck.isSelected())
                days.add(FRIDAY);
            schedule.setDays(days);
            schedule.setEnabled(enableCheck.isSelected());
            schedule.setOnceDownload(onceRadio.isSelected());
            schedule.setQueueId(queue.getId());
            schedule.setStartDate(datePicker.getValue());
            var startTime = LocalTime.of(startHourSpinner.getValue(),
                    startMinuteSpinner.getValue(), startSecondSpinner.getValue());
            schedule.setStartTime(startTime);
            schedule.setStopTimeEnabled(stopAtCheck.isSelected());
            var stopTime = LocalTime.of(stopHourSpinner.getValue(),
                    stopMinuteSpinner.getValue(), stopSecondSpinner.getValue());
            schedule.setStopTime(stopTime);
            schedule.setTurnOffEnabled(whenDoneCheck.isSelected());
            schedule.setTurnOffMode(powerCombo.getValue());
            var startDate = schedule.getStartDate();
            if (schedule.isOnceDownload() && startDate != null) {
                var d = startDate.atTime(schedule.getStartTime());
                boolean before = d.isBefore(LocalDateTime.now());
                if (before)
                    throw new IllegalArgumentException("Can't schedule in past");
            }

            queue.setSpeed(speedField.getText());
            queue.setSimultaneouslyDownload(simulDownloadSpinner.getValue());
            queue.setHasFolder(hasFolderCheck.isSelected());
            queue.setSchedule(schedule);
            String[] qCols = {COL_SPEED_LIMIT, COL_SIMUL_DOWNLOAD, COL_HAS_FOLDER};
            String[] qValues = {queue.getSpeed(), queue.getSimultaneouslyDownload() + "", (queue.hasFolder() ? 1 : 0) + ""};
            QueuesRepo.updateQueue(qCols, qValues, queue.getId());
            createOrDeleteFolder(queue.hasFolder(), queue);
            ScheduleRepo.updateSchedule(schedule);
            var updatedQueues = getQueues().stream()
                    .peek(q -> {
                        if (q.equals(queue))
                            q.setSchedule(schedule);
                    }).toList();
            addAllQueues(updatedQueues);
            queueList.getItems().clear();
            queueList.getItems().addAll(updatedQueues);
            queueList.getSelectionModel().select(queue);

            ScheduleTask.schedule(queue);
            showResultMessage("Successfully Saved", SaveStatus.SUCCESS, executor);
        } catch (IllegalArgumentException e) {
            showResultMessage(e.getMessage(), SaveStatus.ERROR, executor);
        }

    }

    private void createOrDeleteFolder(boolean hasFolder, QueueModel queue) {
        if (hasFolder) {
            var res = IOUtils.createFolderInSaveLocation("Queues" + File.separator + queue.getName());
            if (res) {
                var downloadsByQueueName = DownloadsRepo.getDownloadsByQueueName(queue.getName());
                if (FxUtils.askToMoveFiles(downloadsByQueueName, queue)) {
                    downloadsByQueueName.forEach(dm -> {
                        var newFilePath = downloadPath + File.separator + "Queues" +
                                File.separator + queue.getName() + File.separator + dm.getName();
                        DownloadOpUtils.moveFiles(dm, newFilePath);
                    });
                }
            }
        } else
            QueueUtils.moveFilesAndDeleteQueueFolder(queue.getName());

    }

    private void showResultMessage(String message, SaveStatus saveStatus, ExecutorService executor) {
        executor.submit(() -> {
            try {
                Platform.runLater(() -> savedLabel.setText(message));
                savedLabel.setTextFill(Paint.valueOf("#009688"));
                if (saveStatus == SaveStatus.ERROR)
                    savedLabel.setTextFill(Paint.valueOf("#EF5350"));
                savedLabel.setVisible(true);
                Thread.sleep(1500);
                savedLabel.setVisible(false);
            } catch (InterruptedException ignore) {
            }
            executor.shutdown();
        });
    }

    enum SaveStatus {
        ERROR,
        SUCCESS
    }
}