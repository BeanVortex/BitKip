package ir.darkdeveloper.bitkip.controllers.interfaces;

import javafx.fxml.Initializable;
import javafx.stage.Stage;

public interface FXMLController extends Initializable {

    void initAfterStage();

    void setStage(Stage stage);

    Stage getStage();
}