package io.beanvortex.bitkip.controllers.interfaces;

import io.beanvortex.bitkip.config.observers.ThemeObserver;
import javafx.fxml.Initializable;
import javafx.stage.Stage;

public interface FXMLController extends Initializable, ThemeObserver {

    void initAfterStage();

    void setStage(Stage stage);

    Stage getStage();
}