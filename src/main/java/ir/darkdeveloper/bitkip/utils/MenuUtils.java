package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MenuUtils {


    public static void initFileMenu(Button menuFile, TableUtils tableUtils) {
        var c = new ContextMenu();
        var addLink = new Label("New download");
        var batchDownload = new Label("New batch download");
        var deleteDownloads = new Label("Delete selected");
        var deleteDownloadsWithFile = new Label("Delete selected with file");
        var settings = new Label("Settings");
        var exit = new Label("exit");
        var lbls = List.of(addLink, batchDownload, deleteDownloads, deleteDownloadsWithFile, settings, exit);
        var menuItems = createMapMenuItems(lbls);
        c.getItems().addAll(menuItems.values());
        menuFile.setContextMenu(c);
        menuFile.setOnMouseClicked(event -> {
            var selectedItems = tableUtils.getSelected();
            menuItems.get(deleteDownloads).setDisable(selectedItems.size() == 0);
            deleteDownloads.setText("Delete selected (" + selectedItems.size() + ")");
            c.show(menuFile, Side.BOTTOM, 0, 0);
        });
        menuItems.get(addLink).setOnAction(event -> {
            System.out.println("new Download");
        });
        menuItems.get(batchDownload).setOnAction(event -> {
            System.out.println("new batch download");
        });
        menuItems.get(deleteDownloads).setOnAction(event -> {
            var selectedItems = tableUtils.getSelected();
            selectedItems.forEach(DownloadsRepo::deleteDownload);
            tableUtils.remove(selectedItems);
        });
        menuItems.get(deleteDownloadsWithFile).setOnAction(event -> {
            var selectedItems = tableUtils.getSelected();
            selectedItems.forEach(DownloadsRepo::deleteDownload);
            tableUtils.remove(selectedItems);
            // todo: delete files
        });
        menuItems.get(settings).setOnAction(event -> {
            System.out.println("settings");
        });
        menuItems.get(exit).setOnAction(event -> Platform.exit());
    }


    public static void initOperationMenu(Button operationMenu, TableUtils tableUtils) {
        var c = new ContextMenu();
        var resume = new Label("resume");
        var pause = new Label("pause");
        var restart = new Label("restart");
        var newQueue = new Label("new queue");
        var addQueue = new Label("add to queue");
        var startQueue = new Label("start queue");
        var stopQueue = new Label("stop queue");

        var lbls = List.of(resume, pause, restart);
        var split = new SeparatorMenuItem();
        var menuItems = createMapMenuItems(lbls);
        menuItems.put(new Label("s"), split);

        var newQueueMenu = new MenuItem();
        newQueueMenu.setGraphic(newQueue);

        var addQueueMenu = new Menu();
        addQueueMenu.setGraphic(addQueue);

        var startQueueMenu = new Menu();
        startQueueMenu.setGraphic(startQueue);

        var stopQueueMenu = new Menu();
        stopQueueMenu.setGraphic(stopQueue);

        menuItems.put(newQueue, newQueueMenu);
        menuItems.put(addQueue, addQueueMenu);
        menuItems.put(startQueue, startQueueMenu);
        menuItems.put(stopQueue, stopQueueMenu);

        c.getItems().addAll(menuItems.values());

        operationMenu.setContextMenu(c);
        operationMenu.setOnMouseClicked(event -> {
            var selectedItems = tableUtils.getSelected();
            menuItems.get(resume).setDisable(selectedItems.size() == 0);
            menuItems.get(pause).setDisable(selectedItems.size() == 0);
            menuItems.get(restart).setDisable(selectedItems.size() == 0);
            menuItems.get(addQueue).setDisable(selectedItems.size() == 0);
            readQueueList(addQueueMenu, startQueueMenu, stopQueueMenu);

            c.show(operationMenu, Side.BOTTOM, 0, 0);
        });

        menuItems.get(resume).setOnAction(event -> {
            var selectedItems = tableUtils.getSelected();
            selectedItems
                    .filtered(dm -> !AppConfigs.currentDownloading.contains(dm))
                    .forEach(dm -> {
                        dm.setLastTryDate(LocalDateTime.now());
                        dm.setDownloadStatus(DownloadStatus.Trying);
                        NewDownloadUtils.startDownload(dm, tableUtils, null, null, true);
                    });
        });
        menuItems.get(pause).setOnAction(event -> {
            var selectedItems = tableUtils.getSelected();
            selectedItems.forEach(dm -> {
                if (dm.getDownloadStatus().equals(DownloadStatus.Downloading)) {
                    var downTask = dm.getDownloadTask();
                    if (downTask != null && downTask.isRunning())
                        downTask.pause();
                    else {
                        downTask = AppConfigs.currentDownloading
                                .get(AppConfigs.currentDownloading.indexOf(dm))
                                .getDownloadTask();
                        if (downTask.isRunning())
                            downTask.pause();
                    }
                    tableUtils.refreshTable();
                }
            });
        });
    }

    public static void initAboutMenu(Button aboutMenu, TableView<DownloadModel> table) {
        var c = new ContextMenu();
        var checkForUpdates = new Label("Check updates");
        var about = new Label("About");

        var lbls = List.of(checkForUpdates, about);
        var menuItems = createMapMenuItems(lbls);
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


    public static LinkedHashMap<Label, MenuItem> createMapMenuItems(List<Label> lbls) {
        var menuItems = new LinkedHashMap<Label, MenuItem>();
        lbls.forEach(label -> {
            label.setPrefWidth(150);
            var menuItem = new MenuItem();
            menuItem.setGraphic(label);
            menuItems.put(label, menuItem);
        });
        return menuItems;
    }

    public static List<MenuItem> createMenuItems(List<Label> lbls) {
        var menuItems = new ArrayList<MenuItem>();
        lbls.forEach(label -> {
            label.setPrefWidth(150);
            var menuItem = new MenuItem();
            menuItem.setGraphic(label);
            menuItems.add(menuItem);
        });
        return menuItems;
    }

}
