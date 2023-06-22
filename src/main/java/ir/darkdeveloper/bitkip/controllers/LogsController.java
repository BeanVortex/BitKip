package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import static ir.darkdeveloper.bitkip.config.AppConfigs.dataPath;
import static ir.darkdeveloper.bitkip.config.AppConfigs.log;

public class LogsController implements FXMLController {

    @FXML
    private ComboBox<FileWrapper> comboSelectFile;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Text text;
    private Stage stage;

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        initAfterStage();
    }

    @Override
    public void initAfterStage() {
        updateTheme(stage.getScene());
//        text.wrappingWidthProperty().bind(stage.widthProperty());
        stage.widthProperty().addListener((o, ol, n) -> scrollPane.setPrefWidth(n.doubleValue() - 50));
        stage.heightProperty().addListener((o, ol, n) -> scrollPane.setPrefHeight(n.doubleValue() - 50));
    }

    record FileWrapper(File file) {
        @Override
        public String toString() {
            return file.getName().substring(0, file.getName().lastIndexOf('.'));
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        var logsDir = Path.of(dataPath + "logs").toFile();
        if (logsDir.exists() && logsDir.isDirectory()) {
            var files = logsDir.listFiles();
            if (files != null) {
                ObservableList<FileWrapper> listFiles = FXCollections.observableArrayList();
                for (var f : files)
                    if (!f.getName().contains("lck"))
                        listFiles.add(new FileWrapper(f));
                comboSelectFile.setItems(listFiles);
                comboSelectFile.getSelectionModel().select(listFiles.size() - 1);
                fileSelected();
            }
        }

    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @FXML
    private void fileSelected() {
        try {
            var logs = Files.readAllLines(Path.of(comboSelectFile.getSelectionModel().getSelectedItem().file().getPath()))
                    .stream().reduce((s1, s2) -> String.join("\n", s1, s2));
            text.setText(logs.orElse("No Logs"));
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
            Notifications.create()
                    .title("Failed to read log file")
                    .text(e.getLocalizedMessage())
                    .showError();
        }
    }
}
