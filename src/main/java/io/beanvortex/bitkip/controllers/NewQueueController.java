package io.beanvortex.bitkip.controllers;

import io.beanvortex.bitkip.BitKip;
import io.beanvortex.bitkip.controllers.interfaces.FXMLController;
import io.beanvortex.bitkip.models.QueueModel;
import io.beanvortex.bitkip.repo.QueuesRepo;
import io.beanvortex.bitkip.repo.ScheduleRepo;
import io.beanvortex.bitkip.utils.IOUtils;
import io.beanvortex.bitkip.config.observers.QueueSubject;
import io.beanvortex.bitkip.models.ScheduleModel;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import static io.beanvortex.bitkip.config.AppConfigs.log;

public class NewQueueController implements FXMLController {

    @FXML
    private CheckBox hasFolderCheck;
    @FXML
    private TextField queueField;

    private Stage stage;
    private final QueueModel queueModel = new QueueModel();

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
    public void initAfterStage() {
        updateTheme(stage.getScene());
        var logoPath = BitKip.getResource("icons/logo.png");
        if (logoPath != null) {
            var img = new Image(logoPath.toExternalForm());
            stage.getIcons().add(img);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }


    @FXML
    private void onSaveQueue() {
        var queueName = queueField.getText();
        try {
            QueuesRepo.findByName(queueName, false);
            Notifications.create()
                    .title("Queue exists")
                    .text("Queue with this name already exists")
                    .showWarning();
        } catch (IllegalArgumentException e) {
            queueModel.setName(queueName);
            queueModel.setEditable(true);
            queueModel.setCanAddDownload(true);
            queueModel.setHasFolder(hasFolderCheck.isSelected());
            if (hasFolderCheck.isSelected())
                IOUtils.createFolderInSaveLocation("Queues" + File.separator + queueName);
            var schedule = new ScheduleModel();
            ScheduleRepo.insertSchedule(schedule, -1);
            queueModel.setSchedule(schedule);
            QueuesRepo.insertQueue(queueModel);
            schedule.setQueueId(queueModel.getId());
            ScheduleRepo.updateScheduleQueueId(schedule.getId(), schedule.getQueueId());
            QueueSubject.addQueue(queueModel);
            log.info("Created queue : " + queueModel.toStringModel());
            stage.close();
        }
    }
}
