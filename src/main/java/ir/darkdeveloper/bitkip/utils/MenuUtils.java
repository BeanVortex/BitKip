package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;

import java.util.LinkedHashMap;
import java.util.List;

public class MenuUtils {


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
        var newQueue = new Label("new queue");
        var addQueue = new Label("add to queue");
        var startQueue = new Label("start queue");
        var stopQueue = new Label("stop queue");

        var lbls = List.of(resume, stop, restart);
        var split = new SeparatorMenuItem();
        var menuItems = createMenuItems(lbls);
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
            var selectedItems = table.getSelectionModel().getSelectedItems();
            menuItems.get(resume).setDisable(selectedItems.size() == 0);
            menuItems.get(stop).setDisable(selectedItems.size() == 0);
            menuItems.get(restart).setDisable(selectedItems.size() == 0);
            menuItems.get(addQueue).setDisable(selectedItems.size() == 0);
            readQueueList(addQueueMenu, startQueueMenu, stopQueueMenu);

            c.show(operationMenu, Side.BOTTOM, 0, 0);
        });
    }

    private static void readQueueList(Menu addQueueMenu, Menu startQueueMenu, Menu stopQueueMenu) {
        // read queue list from the file
    }

    public static void initAboutMenu(Button aboutMenu, TableView<DownloadModel> contentTable) {
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

}
