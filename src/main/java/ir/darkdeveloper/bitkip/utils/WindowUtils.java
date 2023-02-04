package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WindowUtils {
    private static double xOffset = 0;
    private static double yOffset = 0;

    public static void toolbarInits(HBox toolbar, Stage stage, Rectangle2D bounds,
                                    Button actionBtn, TableView<DownloadModel> table) {
        toolbar.setOnMousePressed(event -> {
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });
        var boundsCopy = new AtomicReference<>(bounds);
        toolbar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
            Screen.getScreens().forEach(screen -> {
                var currentX = stage.getX();
                if (!isOnPrimaryScreen(currentX))
                    boundsCopy.set(screen.getVisualBounds());
                else
                    boundsCopy.set(bounds);
                if (stage.getWidth() == boundsCopy.get().getWidth())
                    if (stage.getHeight() == boundsCopy.get().getHeight() || stage.getHeight() == boundsCopy.get().getHeight() + boundsCopy.get().getMinY())
                        minimizeWindow(stage, boundsCopy.get());
            });
        });
        toolbar.setOnMouseReleased(event -> {
            var screenY = event.getScreenY();
            if (screenY - boundsCopy.get().getMinY() <= 0)
                maximizeWindow(stage, boundsCopy.get(), actionBtn);
        });
        toolbar.setOnMouseClicked(event -> {
            var doubleClickCondition = event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2;
            var screenY = stage.getY();
            table.getSelectionModel().clearSelection();
            if (doubleClickCondition) {
                if (screenY - boundsCopy.get().getMinY() >= 0 && boundsCopy.get().getHeight() > stage.getHeight())
                    maximizeWindow(stage, boundsCopy.get(), actionBtn);
                else if (screenY - boundsCopy.get().getMinY() <= 0 && boundsCopy.get().getHeight() <= stage.getHeight())
                    minimizeWindow(stage, boundsCopy.get());
            }
        });
    }

    public static void initFileMenu(Button menuFile, TableView<DownloadModel> table) {
        var c = new ContextMenu();
        var addLink = new Label("Add download");
        var batchDownload = new Label("Add batch download");
        var addLinkFromClipboard = new Label("Add download(clipboard)");
        var deleteDownloads = new Label("Delete selected");
        var exit = new Label("exit");
        var lbls = List.of(addLink, batchDownload, addLinkFromClipboard, deleteDownloads, exit);
        var menuItems = createMenuItems(lbls);
        c.getItems().addAll(menuItems.values());
        menuFile.setContextMenu(c);
        menuFile.setOnMouseClicked(event -> {
            var selectedItems = table.getSelectionModel().getSelectedItems();
            menuItems.get(deleteDownloads).setDisable(selectedItems.size() == 0);
            c.show(menuFile, Side.BOTTOM, 0, 0);
        });
        menuItems.get(addLink).setOnAction(event -> {
            System.out.println("new link");
        });
        menuItems.get(batchDownload).setOnAction(event -> {
            System.out.println("new batch download");
        });
        menuItems.get(addLinkFromClipboard).setOnAction(event -> {
            var clipboard = Clipboard.getSystemClipboard();
            System.out.println("new Clipboard download " + clipboard.getString());
        });
        menuItems.get(deleteDownloads).setOnAction(event -> {
            var selectedItems = table.getSelectionModel().getSelectedItems();
            table.getItems().removeAll(selectedItems);
            System.out.println("deleting");
        });
        menuItems.get(exit).setOnAction(event -> Platform.exit());
    }


    public static void initOperationMenu(Button operationMenu, TableView<DownloadModel> table) {
        var c = new ContextMenu();
        var resume = new Label("resume");
        var stop = new Label("stop");
        var restart = new Label("restart");
        var addQueue = new Label("add to queue");
        var startQueue = new Label("start queue");
        var stopQueue = new Label("stop queue");

        var lbls = List.of(resume, stop, restart);
        var split = new SeparatorMenuItem();
        var menuItems = createMenuItems(lbls);
        menuItems.put(new Label("s"), split);
        var queueMenu = new Menu();
        queueMenu.setGraphic(addQueue);
        var menuBar = new MenuBar();
        menuBar.getMenus().add(queueMenu);
        c.getItems().addAll(menuItems.values());
        operationMenu.setContextMenu(c);
        operationMenu.setOnMouseClicked(event -> {
            var selectedItems = table.getSelectionModel().getSelectedItems();
//            menuItems.get(deleteDownloads).setDisable(selectedItems.size() == 0);
            c.show(operationMenu, Side.BOTTOM, 0, 0);
        });
    }

    private static LinkedHashMap<Label, MenuItem> createMenuItems(List<Label> lbls) {
        var menuItems = new LinkedHashMap<Label, MenuItem>();
        lbls.forEach(label -> {
            label.setPrefWidth(150);
            var menuItem = new MenuItem();
            menuItem.setGraphic(label);
            menuItems.put(label, menuItem);
        });
        return menuItems;
    }

    public static void initAboutMenu(Button aboutMenu, TableView<DownloadModel> contentTable) {
    }


    public static Rectangle2D maximizeWindow(Stage stage, Rectangle2D bounds, Button actionBtn) {
        var currentX = stage.getX();
        var boundsCopy = new AtomicReference<>(bounds);
        Screen.getScreens().forEach(screen -> {
            if (!isOnPrimaryScreen(currentX))
                boundsCopy.set(screen.getVisualBounds());
            maximizeStage(stage, boundsCopy.get(), actionBtn);
        });
        return boundsCopy.get();
    }

    public static Rectangle2D minimizeWindow(Stage stage, Rectangle2D bounds) {
        var currentX = stage.getX();
        var boundsCopy = new AtomicReference<>(bounds);
        Screen.getScreens().forEach(screen -> {
            if (!isOnPrimaryScreen(currentX))
                boundsCopy.set(screen.getVisualBounds());
            minimizeStage(stage, boundsCopy.get());
        });
        return boundsCopy.get();
    }

    private static void maximizeStage(Stage stage, Rectangle2D bounds, Button actionBtn) {
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        if (actionBtn.getTranslateY() == 100)
            actionBtn.setTranslateY(0);
    }

    private static void minimizeStage(Stage stage, Rectangle2D bounds) {
        var width = 853;
        var height = 515;
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setY((bounds.getMaxY() - height) / 2);
        stage.setX((bounds.getMaxX() - width) / 2);
    }

    public static boolean isOnPrimaryScreen(double x) {
        var bounds = Screen.getPrimary().getVisualBounds();
        return x < bounds.getMaxX();
    }


}
