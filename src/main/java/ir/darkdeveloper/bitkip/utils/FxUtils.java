package ir.darkdeveloper.bitkip.utils;


import ir.darkdeveloper.bitkip.controllers.FXMLController;
import ir.darkdeveloper.bitkip.controllers.MainController;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ir.darkdeveloper.bitkip.BitKip.getResource;

public class FxUtils {


    public static void switchSceneToMain(Stage stage, String fxmlFilename) {
        try {
            var loader = new FXMLLoader(getResource("fxml/" + fxmlFilename));
            Parent root = loader.load();
            var scene = new Scene(root, stage.getWidth(), stage.getHeight());
            MainController controller = loader.getController();
            controller.setStage(stage);
            stage.setScene(scene);
            stage.setTitle("BitKip");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public static Stage getStageFromEvent(ActionEvent e) {
        if (e.getSource() instanceof Node n
                && n.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    public static <T extends FXMLController> T switchSceneAndGetController(
            ActionEvent e, String fxmlFilename, String stageTitle, Class<T> tClass) {
        return switchSceneAndGetController(getStageFromEvent(e), fxmlFilename, stageTitle, tClass);
    }

    public static <T extends FXMLController> T switchSceneAndGetController(Stage stage, String fxmlFilename, String stageTitle, Class<T> tClass) {
        try {
            var loader = new FXMLLoader(getResource("fxml/" + fxmlFilename));
            Parent root = loader.load();
            var width = stage.getWidth();
            var height = stage.getHeight();
            var prevScene = stage.getScene();
            if (prevScene != null) {
                width = prevScene.getWidth();
                height = prevScene.getHeight();
            }

            var scene = new Scene(root, width, height);
            stage.setScene(scene);
            stage.setTitle("JBookFinder - " + stageTitle);
            var controller = (FXMLController) loader.getController();
            controller.setStage(stage);
            return tClass.cast(controller);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static FXMLController newStageAndReturnController(String fxmlFilename, String stageTitle, double minWidth, double minHeight) {
        try {
            var stage = new Stage();
            var loader = new FXMLLoader(getResource("fxml/" + fxmlFilename));
            Parent root = loader.load();
            FXMLController controller = loader.getController();
            controller.setStage(stage);
            var scene = new Scene(root);
            stage.setScene(scene);
            var logoPath = getResource("images/logo.png");
            if (logoPath != null)
                stage.getIcons().add(new Image(logoPath.toExternalForm()));
            stage.setTitle("JBookFinder - " + stageTitle);
            stage.setMinWidth(minWidth);
            stage.setMinHeight(minHeight);
            stage.show();
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> List<T> getAllNodes(Parent root, Class<T> tClass) {
        var nodes = new ArrayList<T>();
        addAllDescendents(root, nodes, tClass);
        return nodes;
    }

    private static <T> void addAllDescendents(Parent root, ArrayList<T> nodes, Class<T> tClass) {
        for (var node : root.getChildrenUnmodifiable()) {
            if (tClass.isInstance(node))
                nodes.add(tClass.cast(node));
            if (node instanceof Parent p)
                addAllDescendents(p, nodes, tClass);
        }
    }

//    public static void updateButtonTheme(List<Button> buttons) {
//        buttons.forEach(btn -> {
//            if (Configs.getTheme().equals("dark")) {
//                btn.getStyleClass().remove("button-dark");
//                if (!btn.getStyleClass().contains("button-light"))
//                    btn.getStyleClass().add("button-light");
//            } else {
//                if (!btn.getStyleClass().contains("button-dark"))
//                    btn.getStyleClass().add("button-dark");
//                btn.getStyleClass().remove("button-light");
//            }
//        });
//    }

//    public static void updateThemeForBooks(String theme, FlowPane booksContainer, VBox contentVbox, List<HBox> itemParents) {
//        var labels = FxUtils.getAllNodes(booksContainer, Label.class);
//        if (theme.equals("light")) {
//            booksContainer.setBackground(Background.fill(Paint.valueOf("#fff")));
//            contentVbox.setBackground(Background.fill(Paint.valueOf("#fff")));
//            itemParents.forEach(parent -> parent.setBackground(Background.fill(Paint.valueOf("#fff"))));
//            labels.forEach(label -> label.setTextFill(Paint.valueOf("#333")));
//        } else {
//            booksContainer.setBackground(Background.fill(Paint.valueOf("#333")));
//            contentVbox.setBackground(Background.fill(Paint.valueOf("#333")));
//            itemParents.forEach(parent -> parent.setBackground(Background.fill(Paint.valueOf("#333"))));
//            labels.forEach(label -> label.setTextFill(Paint.valueOf("#fff")));
//        }
//    }
}

