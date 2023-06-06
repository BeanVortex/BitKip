package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.config.observers.QueueSubject;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.repo.ScheduleRepo;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.log;

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
        var logoPath = getResource("icons/logo.png");
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
