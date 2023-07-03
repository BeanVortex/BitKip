package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.observers.QueueObserver;
import ir.darkdeveloper.bitkip.config.observers.QueueSubject;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.models.TurnOffMode;
import ir.darkdeveloper.bitkip.repo.DatabaseHelper;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.repo.ScheduleRepo;
import ir.darkdeveloper.bitkip.task.ScheduleTask;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.Validations;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

import static com.sun.jna.Platform.*;
import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.config.observers.QueueSubject.getQueueSubject;
import static ir.darkdeveloper.bitkip.repo.DatabaseHelper.QUEUES_TABLE_NAME;
import static ir.darkdeveloper.bitkip.repo.QueuesRepo.*;
import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.Defaults.staticQueueNames;
import static java.time.DayOfWeek.*;

public class QueueSetting implements FXMLController, QueueObserver {

    @FXML
    private Label selectedQueueLbl, windowsPowerLbl, savedLabel;
    @FXML
    private TextField speedField;
    @FXML
    private ComboBox<TurnOffMode> powerCombo;
    @FXML
    private Spinner<Integer> simulDownloadSpinner, stopHourSpinner, stopMinuteSpinner, stopSecondSpinner,
            startHourSpinner, startMinuteSpinner, startSecondSpinner;
    @FXML
    private CheckBox hasFolderCheck, downloadOrderCheck, whenDoneCheck, stopAtCheck,
            saturdayCheck, sundayCheck, mondayCheck, tuesdayCheck, wednesdayCheck, thursdayCheck, fridayCheck,
            enableCheck;
    @FXML
    private RadioButton onceRadio, dailyRadio;
    @FXML
    private GridPane weeksContainer;
    @FXML
    private DatePicker datePicker;
    @FXML
    private HBox stopContainer, horLine1, horLine2;
    @FXML
    private VBox mainBox, rightContainer;
    @FXML
    private ListView<QueueModel> queueList;


    private Stage stage;
    private final ObjectProperty<QueueModel> selectedQueue = new SimpleObjectProperty<>();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        selectedQueue.addListener((ob, old, newVal) -> initSelectedQueueData());
        initQueuesList();
        initInputs();
        initRadios();
        initPowerCombo();
    }

    private void initPowerCombo() {
        var items = FXCollections.observableArrayList(TurnOffMode.TURN_OFF, TurnOffMode.SLEEP);
        powerCombo.setItems(items);
        powerCombo.getSelectionModel().select(0);
        powerCombo.setDisable(true);
        if (isWindows()) {
            windowsPowerLbl.setMaxWidth(rightContainer.getPrefWidth());
            windowsPowerLbl.setWrapText(true);
            windowsPowerLbl.setText("Sleep option may hibernate windows." +
                    " To avoid, please run cmd as administrator and type: powercfg -hibernate off");
            windowsPowerLbl.setVisible(true);
        }
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

        Validations.validateTimePickerInputs(startHourSpinner, startMinuteSpinner, startSecondSpinner);
        Validations.validateTimePickerInputs(stopHourSpinner, stopMinuteSpinner, stopSecondSpinner);
        Validations.validateSpeedInputChecks(speedField);
        Validations.validateIntInputCheck(simulDownloadSpinner.getEditor(), 1, 1, 5);


        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: darkgray;");
                }
            }
        });
    }

    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
        stage.widthProperty().addListener((o, o2, n) -> {
            var width = n.longValue();
            horLine1.setPrefWidth(width);
            horLine2.setPrefWidth(width);
            rightContainer.setPrefWidth(width - queueList.getPrefWidth());
        });
        selectedQueue.set(QueueSubject.getQueues().get(0));
        stage.heightProperty().addListener((o, o2, n) -> {
            var height = n.longValue();
            queueList.setPrefHeight(height);
            rightContainer.setPrefHeight(height - 32 - 10);
        });

        var logoPath = getResource("icons/logo.png");
        if (logoPath != null) {
            var img = new Image(logoPath.toExternalForm());
            stage.getIcons().add(img);
        }

    }

    public void setSelectedQueue(QueueModel selectedQueue) {
        if (selectedQueue != null)
            this.selectedQueue.set(selectedQueue);
    }

    private void initSelectedQueueData() {

        speedField.setText(selectedQueue.get().getSpeed());
        simulDownloadSpinner.getValueFactory().setValue(selectedQueue.get().getSimultaneouslyDownload());
        hasFolderCheck.setSelected(selectedQueue.get().hasFolder());
        hasFolderCheck.setDisable(false);
        if (staticQueueNames.stream().anyMatch(s -> s.equals(selectedQueue.get().getName()))) {
            hasFolderCheck.setSelected(true);
            hasFolderCheck.setDisable(true);
            if (selectedQueue.get().getName().equals(ALL_DOWNLOADS_QUEUE))
                hasFolderCheck.setSelected(false);
        }
        downloadOrderCheck.setSelected(selectedQueue.get().isDownloadFromTop());
        stage.setTitle("Queue Setting: %s".formatted(selectedQueue.get().getName()));
        selectedQueueLbl.setText("Queue: " + selectedQueue.get().getName());
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
        queueList.getItems().addAll(QueueSubject.getQueues());
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


    @Override
    public void updateQueue() {
        initQueuesList();
        initSelectedQueueData();
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
        getQueueSubject().removeObserver(this);
        stage.close();
    }

    @FXML
    private void onSave() {
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
            if (schedule.isEnabled() && schedule.isTurnOffEnabled() && (isLinux() || isMac()) && userPassword == null) {
                var header = "By enabling to turn off or suspend automatically, provide system password.";
                var content = "To be able to turn off or suspend your pc, application needs your system password";
                if (!FxUtils.askForPassword(header, content))
                    throw new IllegalArgumentException("Can't schedule because application won't be able to" +
                            " turn off or suspend\nprovide password in the prompt");
            }

            var startDate = schedule.getStartDate();
            if (schedule.isEnabled() && schedule.isOnceDownload() && startDate != null) {
                var d = startDate.atTime(schedule.getStartTime());
                boolean before = d.isBefore(LocalDateTime.now());
                if (before)
                    throw new IllegalArgumentException("Can't schedule in past");
            }

            queue.setSpeed(speedField.getText());
            queue.setSimultaneouslyDownload(simulDownloadSpinner.getValue());
            if (queue.hasFolder() != hasFolderCheck.isSelected() && startedQueues.contains(queue)) {
                onReset();
                throw new IllegalArgumentException("This Queue is currently running, stop it and try again");
            }
            queue.setHasFolder(hasFolderCheck.isSelected());
            queue.setDownloadFromTop(downloadOrderCheck.isSelected());
            queue.setSchedule(schedule);
            String[] qCols = {COL_SPEED_LIMIT, COL_SIMUL_DOWNLOAD, COL_HAS_FOLDER, COL_DOWN_TOP};
            String[] qValues = {queue.getSpeed(), String.valueOf(queue.getSimultaneouslyDownload()),
                    String.valueOf(queue.hasFolder() ? 1 : 0), String.valueOf(queue.isDownloadFromTop() ? 1 : 0)};
            DatabaseHelper.updateRow(qCols, qValues, QUEUES_TABLE_NAME, queue.getId());
            IOUtils.createOrDeleteFolderForQueue(queue);
            ScheduleRepo.updateSchedule(schedule);
            var updatedQueues = QueuesRepo.getAllQueues(false, true);
            QueueSubject.addAllQueues(updatedQueues);
            queueList.getItems().clear();
            queueList.getItems().addAll(updatedQueues);
            queueList.getSelectionModel().select(queue);

            ScheduleTask.schedule(queue);
            showResultMessage("Successfully Saved", SaveStatus.SUCCESS);
            log.info("Updated queue : " + queue.toStringModel());
        } catch (IllegalArgumentException e) {
            showResultMessage(e.getMessage(), SaveStatus.ERROR);
            log.error(e.getMessage());
        }

    }

    private void showResultMessage(String message, SaveStatus saveStatus) {
        var executor = Executors.newCachedThreadPool();
        executor.submit(() -> {
            try {
                Platform.runLater(() -> savedLabel.setText(message));
                savedLabel.setTextFill(Paint.valueOf("#009688"));
                if (saveStatus == SaveStatus.ERROR)
                    savedLabel.setTextFill(Paint.valueOf("#EF5350"));
                savedLabel.setVisible(true);
                Thread.sleep(2500);
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