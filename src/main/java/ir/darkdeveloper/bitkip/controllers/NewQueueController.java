package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.repo.ScheduleRepo;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.WindowUtils;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import static ir.darkdeveloper.bitkip.BitKip.getResource;

public class NewQueueController implements FXMLController {

    @FXML
    private CheckBox hasFolderCheck;
    @FXML
    private ImageView logoImg;
    @FXML
    private HBox toolbar;
    @FXML
    private Button closeBtn;
    @FXML
    private TextField queueField;

    private Stage stage;
    private Rectangle2D bounds;

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
        stage.widthProperty().addListener((ob, o, n) -> toolbar.setPrefWidth(n.longValue()));
        stage.xProperty().addListener((o) -> bounds = Screen.getPrimary().getVisualBounds());
        var logoPath = getResource("icons/logo.png");
        if (logoPath != null) {
            var img = new Image(logoPath.toExternalForm());
            logoImg.setImage(img);
            stage.getIcons().add(img);
        }
        int minHeight = 100;
        int minWidth = 250;
        WindowUtils.toolbarInits(toolbar, stage, bounds, minWidth, minHeight);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        closeBtn.setGraphic(new FontIcon());
        bounds = Screen.getPrimary().getVisualBounds();
    }

    @FXML
    private void closeApp() {
        stage.close();
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
        AppConfigs.addQueue(queueModel);
        stage.close();
    }

}
