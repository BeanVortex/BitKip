package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static ir.darkdeveloper.bitkip.utils.FileExtensions.staticQueueNames;
import static ir.darkdeveloper.bitkip.utils.ShortcutUtils.*;

public class MenuUtils {


    public static void initFileMenu(Button menuFile, MainTableUtils mainTableUtils) {
        var c = new ContextMenu();
        var addLink = new Label("New download");
        var batchDownload = new Label("New batch download");
        var settings = new Label("Settings");
        var exit = new Label("exit");
        var lbls = List.of(addLink, batchDownload, settings, exit);
        var keyCodes = List.of(NEW_DOWNLOAD_KEY, NEW_BATCH_KEY, SETTINGS_KEY, QUIT_KEY);
        var menuItems = createMapMenuItems(lbls, keyCodes);
        c.getItems().addAll(menuItems.values());
        menuFile.setContextMenu(c);
        menuFile.setOnMouseClicked(event -> c.show(menuFile, Side.BOTTOM, 0, 0));
        menuItems.get(addLink).setOnAction(e -> DownloadOpUtils.newDownload(mainTableUtils, true));
        menuItems.get(batchDownload).setOnAction(e -> DownloadOpUtils.newDownload(mainTableUtils, false));
        menuItems.get(settings).setOnAction(e -> System.out.println("settings"));
        menuItems.get(exit).setOnAction(e -> Platform.exit());
    }


    public static void initOperationMenu(Button operationMenu, MainTableUtils mainTableUtils) {
        var c = new ContextMenu();
        var openLbl = new Label("Open");
        var resumeLbl = new Label("Resume");
        var pauseLbl = new Label("Pause");
        var restartLbl = new Label("Restart");
        var deleteLbl = new Label("Delete selected");
        var deleteWithFileLbl = new Label("Delete selected with file");
        var newQueueLbl = new Label("New queue");
        var deleteFromQueueLbl = new Label("Delete from this queue");
        var addToQueueLbl = new Label("Add to queue");
        var startQueueLbl = new Label("Start queue");
        var stopQueueLbl = new Label("Stop queue");

        var lbls = List.of(openLbl, resumeLbl, pauseLbl, restartLbl, deleteLbl, deleteWithFileLbl);
        var keyCodes = List.of(OPEN_KEY, RESUME_KEY, PAUSE_KEY, RESTART_KEY, DELETE_KEY, DELETE_FILE_KEY);
        var menuItems = createMapMenuItems(lbls, keyCodes);

        var split = new SeparatorMenuItem();
        menuItems.put(new Label("s"), split);

        var lbls2 = List.of(newQueueLbl, deleteFromQueueLbl);
        var keyCodes2 = Arrays.asList(NEW_QUEUE_KEY, null);
        var menuItems2 = createMapMenuItems(lbls2, keyCodes2);
        menuItems.putAll(menuItems2);

        var addToQueueMenu = new Menu();
        addToQueueMenu.setGraphic(addToQueueLbl);

        var startQueueMenu = new Menu();
        startQueueMenu.setGraphic(startQueueLbl);

        var stopQueueMenu = new Menu();
        stopQueueMenu.setGraphic(stopQueueLbl);

        initQueueMenuList(addToQueueMenu, startQueueMenu, stopQueueMenu, mainTableUtils);

        menuItems.put(addToQueueLbl, addToQueueMenu);
        menuItems.put(startQueueLbl, startQueueMenu);
        menuItems.put(stopQueueLbl, stopQueueMenu);
        c.getItems().addAll(menuItems.values());
        operationMenu.setContextMenu(c);

        operationMenu.setOnMouseClicked(event -> {
            var selectedItems = mainTableUtils.getSelected();
            disableMenuItems(resumeLbl, pauseLbl, openLbl, deleteFromQueueLbl, restartLbl,
                    addToQueueLbl, deleteLbl, deleteWithFileLbl, menuItems, selectedItems);
            deleteLbl.setText("Delete selected (" + selectedItems.size() + ")");
            c.show(operationMenu, Side.BOTTOM, 0, 0);
        });

        menuItems.get(openLbl).setOnAction(e -> DownloadOpUtils.openFiles(mainTableUtils.getSelected()));
        menuItems.get(resumeLbl).setOnAction(e -> DownloadOpUtils.resumeDownloads(mainTableUtils,
                mainTableUtils.getSelected(), null, null));
        menuItems.get(pauseLbl).setOnAction(e -> DownloadOpUtils.pauseDownloads(mainTableUtils));
        menuItems.get(restartLbl).setOnAction(e -> System.out.println("restartLbl"));
        menuItems.get(deleteLbl).setOnAction(e -> DownloadOpUtils.deleteDownloads(mainTableUtils, false));
        menuItems.get(deleteWithFileLbl).setOnAction(e -> DownloadOpUtils.deleteDownloads(mainTableUtils, true));
        menuItems.get(newQueueLbl).setOnAction(e -> FxUtils.newQueueStage());
    }

    public static void disableMenuItems(Label resumeLbl, Label pauseLbl, Label openLbl, Label deleteFromQueueLbl,
                                        Label restartLbl, Label addToQueueLbl, Label deleteLbl,
                                        Label deleteWithFileLbl,
                                        LinkedHashMap<Label, MenuItem> menuItems,
                                        ObservableList<DownloadModel> selectedItems) {
        menuItems.get(openLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(resumeLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(pauseLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(restartLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(addToQueueLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(deleteLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(deleteWithFileLbl).setDisable(selectedItems.isEmpty());

        selectedItems.stream().filter(dm -> dm.getDownloadStatus() == DownloadStatus.Paused)
                .findAny().ifPresent(dm -> {
                    menuItems.get(resumeLbl).setDisable(false);
                    menuItems.get(pauseLbl).setDisable(true);
                    menuItems.get(openLbl).setDisable(true);
                });
        selectedItems.stream().filter(dm -> dm.getDownloadStatus() == DownloadStatus.Downloading)
                .findAny().ifPresent(dm -> {
                    menuItems.get(resumeLbl).setDisable(true);
                    menuItems.get(pauseLbl).setDisable(false);
                    menuItems.get(openLbl).setDisable(true);
                });
        selectedItems.stream().filter(dm -> dm.getDownloadStatus() == DownloadStatus.Completed)
                .findAny().ifPresent(dm -> {
                    menuItems.get(resumeLbl).setDisable(true);
                    menuItems.get(pauseLbl).setDisable(true);
                    menuItems.get(openLbl).setDisable(false);
                });
        selectedItems
                .filtered(dm -> staticQueueNames.stream()
                        .anyMatch(s -> dm.getQueue().get(0).getName().equals(s)))
                .stream()
                .findAny()
                .ifPresentOrElse(dm -> menuItems.get(deleteFromQueueLbl).setDisable(true),
                        () -> menuItems.get(deleteFromQueueLbl).setDisable(selectedItems.isEmpty()));
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

    private static void initQueueMenuList(Menu addToQueueMenu, Menu startQueueMenu,
                                          Menu stopQueueMenu, MainTableUtils mainTableUtils) {
        var addToQueueItems = new LinkedHashMap<MenuItem, QueueModel>();
        var startQueueItems = new LinkedHashMap<MenuItem, QueueModel>();
        var stopQueueItems = new LinkedHashMap<MenuItem, QueueModel>();
        QueuesRepo.getQueues().forEach(qm -> {
            var defaultColor = ((Label) addToQueueMenu.getGraphic()).getTextFill();
            if (staticQueueNames.stream().noneMatch(s -> qm.getName().equals(s))) {
                var addToQueueMenuItem = createMenuItem(qm, defaultColor);
                addToQueueItems.put(addToQueueMenuItem, qm);
            }
            var startQueueMenuItem = createMenuItem(qm, defaultColor);
            var stopQueueMenuItem = createMenuItem(qm, defaultColor);
            startQueueItems.put(startQueueMenuItem, qm);
            stopQueueItems.put(stopQueueMenuItem, qm);
        });
        addToQueueMenu.getItems().addAll(addToQueueItems.keySet());
        startQueueMenu.getItems().addAll(startQueueItems.keySet());
        stopQueueMenu.getItems().addAll(stopQueueItems.keySet());

        addToQueueMenu.getItems().forEach(menuItem ->
                menuItem.setOnAction(e -> {
                    var qm = addToQueueItems.get(menuItem);
                    var notObserved = new ArrayList<>(mainTableUtils.getSelected());
                    notObserved.forEach(dm -> {
                        if (dm.getQueue().contains(qm))
                            return;
                        if (staticQueueNames.stream().noneMatch(s -> dm.getQueue().get(0).getName().equals(s)))
                            mainTableUtils.remove(dm);
                        DownloadsRepo.updateDownloadQueue(dm.getId(), qm.getId());
                    });
                }));

        startQueueMenu.getItems().forEach(menuItem -> menuItem.setOnAction(e ->
                DownloadOpUtils.startDownloadsInQueue(startQueueItems, menuItem, mainTableUtils)));
    }

    public static MenuItem createMenuItem(QueueModel qm, Paint defaultColor) {
        var queueMenuItem = new MenuItem();
        var lbl = new Label(qm.getName());
        lbl.setTextFill(defaultColor);
        lbl.setOnMouseEntered(e -> lbl.setTextFill(Paint.valueOf("#fff")));
        lbl.setOnMouseExited(e -> lbl.setTextFill(defaultColor));
        lbl.setPrefWidth(150);
        lbl.setStyle("-fx-padding: 2 0 0 2");
        queueMenuItem.setGraphic(lbl);
        queueMenuItem.setStyle("-fx-padding: 0");
        return queueMenuItem;
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


}
