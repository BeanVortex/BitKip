package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.KeyCodeCombination;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static ir.darkdeveloper.bitkip.utils.ShortcutUtils.*;

public class MenuUtils {


    public static void initFileMenu(Button menuFile, MainTableUtils mainTableUtils) {
        var c = new ContextMenu();
        var addLink = new Label("New download");
        var batchDownload = new Label("New batch download");
        var deleteDownloads = new Label("Delete selected");
        var deleteDownloadsWithFile = new Label("Delete selected with file");
        var settings = new Label("Settings");
        var exit = new Label("exit");
        var lbls = List.of(addLink, batchDownload, deleteDownloads, deleteDownloadsWithFile, settings, exit);
        var keyCodes = List.of(NEW_DOWNLOAD_KEY, NEW_BATCH_KEY, DELETE_KEY, DELETE_FILE_KEY, SETTINGS_KEY, QUIT_KEY);
        var menuItems = createMapMenuItems(lbls, keyCodes);
        c.getItems().addAll(menuItems.values());
        menuFile.setContextMenu(c);
        menuFile.setOnMouseClicked(event -> {
            var selectedItems = mainTableUtils.getSelected();
            menuItems.get(deleteDownloads).setDisable(selectedItems.size() == 0);
            menuItems.get(deleteDownloadsWithFile).setDisable(selectedItems.size() == 0);
            deleteDownloads.setText("Delete selected (" + selectedItems.size() + ")");
            c.show(menuFile, Side.BOTTOM, 0, 0);
        });
        menuItems.get(addLink).setOnAction(e -> DownloadOpUtils.newDownload(mainTableUtils, false));
        menuItems.get(batchDownload).setOnAction(e -> DownloadOpUtils.newDownload(mainTableUtils, true));
        menuItems.get(deleteDownloads).setOnAction(e -> DownloadOpUtils.deleteDownloads(mainTableUtils, false));
        menuItems.get(deleteDownloadsWithFile).setOnAction(e -> DownloadOpUtils.deleteDownloads(mainTableUtils, true));
        menuItems.get(settings).setOnAction(e -> System.out.println("settings"));
        menuItems.get(exit).setOnAction(e -> Platform.exit());
    }


    public static void initOperationMenu(Button operationMenu, MainTableUtils mainTableUtils) {
        var c = new ContextMenu();
        var resume = new Label("Resume");
        var pause = new Label("Pause");
        var restart = new Label("Restart");
        var newQueue = new Label("New queue");
        var addQueue = new Label("Add to queue");
        var startQueue = new Label("Start queue");
        var stopQueue = new Label("Stop queue");

        var lbls = List.of(resume, pause, restart);
        var keyCodes = List.of(RESUME_KEY, PAUSE_KEY, RESTART_KEY);
        var menuItems = createMapMenuItems(lbls, keyCodes);

        var split = new SeparatorMenuItem();
        menuItems.put(new Label("s"), split);

        var newQueueMenu = new MenuItem();
        newQueueMenu.setGraphic(newQueue);
        newQueueMenu.setAccelerator(NEW_QUEUE_KEY);
        menuItems.put(newQueue, newQueueMenu);

        var addQueueMenu = new Menu();
        addQueueMenu.setGraphic(addQueue);
        menuItems.put(addQueue, addQueueMenu);

        var startQueueMenu = new Menu();
        startQueueMenu.setGraphic(startQueue);
        menuItems.put(startQueue, startQueueMenu);

        var stopQueueMenu = new Menu();
        stopQueueMenu.setGraphic(stopQueue);
        menuItems.put(stopQueue, stopQueueMenu);

        c.getItems().addAll(menuItems.values());

        operationMenu.setContextMenu(c);
        operationMenu.setOnMouseClicked(event -> {
            var selectedItems = mainTableUtils.getSelected();
            menuItems.get(resume).setDisable(selectedItems.size() == 0);
            menuItems.get(pause).setDisable(selectedItems.size() == 0);
            menuItems.get(restart).setDisable(selectedItems.size() == 0);
            menuItems.get(addQueue).setDisable(selectedItems.size() == 0);
            readQueueList(addQueueMenu, startQueueMenu, stopQueueMenu);

            c.show(operationMenu, Side.BOTTOM, 0, 0);
        });

        menuItems.get(resume).setOnAction(e -> DownloadOpUtils.resumeDownloads(mainTableUtils));
        menuItems.get(pause).setOnAction(e -> DownloadOpUtils.pauseDownloads(mainTableUtils));
        menuItems.get(newQueue).setOnAction(e -> FxUtils.newQueueStage());
    }

    public static void initAboutMenu(Button aboutMenu, TableView<DownloadModel> table) {
        var c = new ContextMenu();
        var checkForUpdates = new Label("Check updates");
        var about = new Label("About");

        var lbls = List.of(checkForUpdates, about);
        var menuItems = createMapMenuItems(lbls, null);
        c.getItems().addAll(menuItems.values());
        aboutMenu.setContextMenu(c);
        aboutMenu.setOnAction(event -> {
            table.getSelectionModel().clearSelection();
            c.show(aboutMenu, Side.BOTTOM, 0, 0);
        });
    }

    private static void readQueueList(Menu addQueueMenu, Menu startQueueMenu, Menu stopQueueMenu) {
        // read queue list from the file
    }


    public static LinkedHashMap<Label, MenuItem> createMapMenuItems(List<Label> lbls, List<KeyCodeCombination> keyCodes) {
        var menuItems = new LinkedHashMap<Label, MenuItem>();
        for (int i = 0; i < lbls.size(); i++) {
            lbls.get(i).setPrefWidth(150);
            var menuItem = new MenuItem();
            menuItem.setGraphic(lbls.get(i));
            if (keyCodes != null && keyCodes.get(i) != null)
                menuItem.setAccelerator(keyCodes.get(i));
            menuItems.put(lbls.get(i), menuItem);
        }
        return menuItems;
    }

    /**
     * labels value should correspond keycodes value
     * otherwise leave keycodes null
     */
    public static List<MenuItem> createMenuItems(List<Label> lbls, List<KeyCodeCombination> keyCodes) {
        var menuItems = new ArrayList<MenuItem>();
        for (int i = 0; i < lbls.size(); i++) {
            lbls.get(i).setPrefWidth(150);
            var menuItem = new MenuItem();
            menuItem.setGraphic(lbls.get(i));
            if (keyCodes != null && keyCodes.get(i) != null)
                menuItem.setAccelerator(keyCodes.get(i));
            menuItems.add(menuItem);
        }
        return menuItems;
    }

}
