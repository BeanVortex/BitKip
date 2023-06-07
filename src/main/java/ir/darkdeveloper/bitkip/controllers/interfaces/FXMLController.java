package ir.darkdeveloper.bitkip.controllers.interfaces;

import ir.darkdeveloper.bitkip.config.observers.ThemeObserver;
import javafx.fxml.Initializable;
import javafx.stage.Stage;

public interface FXMLController extends Initializable, ThemeObserver {

    void initAfterStage();

    void setStage(Stage stage);

    Stage getStage();
}