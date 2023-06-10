package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.observers.QueueSubject;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.FileType;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.paint.Paint;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.Defaults.staticQueueNames;
import static ir.darkdeveloper.bitkip.utils.ShortcutUtils.*;

public class MenuUtils {

    public static Menu startQueueMenu;
    public static Menu stopQueueMenu;


    public static void initFileMenu(Button menuFile) {
        var c = new ContextMenu();
        var addLink = new Label("New download");
        var batchDownload = new Label("New batch download");
        var settings = new Label("Settings");
        var queueSettings = new Label("Queue settings");
        var importLinks = new Label("Import links");
        var exportLinks = new Label("Export links");
        var exit = new Label("exit");
        var lbls = List.of(addLink, batchDownload, settings, queueSettings, importLinks, exportLinks, exit);
        var keyCodes = Arrays.asList(NEW_DOWNLOAD_KEY, NEW_BATCH_KEY, SETTINGS_KEY, null, null, null, QUIT_KEY);
        var menuItems = createMapMenuItems(lbls, keyCodes);
        c.getItems().addAll(menuItems.values());
        menuFile.setContextMenu(c);
        menuFile.setOnMouseClicked(event -> c.show(menuFile, Side.BOTTOM, 0, 0));
        menuItems.get(addLink).setOnAction(e -> DownloadOpUtils.newDownload(true));
        menuItems.get(batchDownload).setOnAction(e -> DownloadOpUtils.newDownload(false));
        menuItems.get(settings).setOnAction(e -> FxUtils.newSettingsStage());
        menuItems.get(queueSettings).setOnAction(e -> FxUtils.newQueueSettingStage(null));
        menuItems.get(importLinks).setOnAction(DownloadOpUtils::importLinks);
        menuItems.get(exportLinks).setOnAction(e -> DownloadOpUtils.exportLinks(ALL_DOWNLOADS_QUEUE));
        menuItems.get(exit).setOnAction(e -> Platform.exit());
    }


    public static void initOperationMenu(Button operationMenu) {
        var c = new ContextMenu();
        var openLbl = new Label("Open");
        var openFolderLbl = new Label("Open folder");
        var resumeLbl = new Label("Resume");
        var pauseLbl = new Label("Pause");
        var refreshLbl = new Label("Refresh URL");
        var copyLbl = new Label("Copy URL");
        var restartLbl = new Label("Restart");
        var exportLinkLbl = new Label("Export selected");
        var deleteLbl = new Label("Delete selected");
        var deleteWithFileLbl = new Label("Delete selected with file");
        var newQueueLbl = new Label("New queue");
        var deleteFromQueueLbl = new Label("Delete from this queue");
        var addToQueueLbl = new Label("Add to queue");
        var startQueueLbl = new Label("Start queue");
        var stopQueueLbl = new Label("Stop queue");
        var queueSettingLbl = new Label("Queue settings");

        var lbls = List.of(openLbl, openFolderLbl, resumeLbl, pauseLbl, refreshLbl, copyLbl,
                restartLbl, exportLinkLbl, deleteLbl, deleteWithFileLbl);
        var keyCodes = Arrays.asList(OPEN_KEY, OPEN_FOLDER_KEY, RESUME_KEY, PAUSE_KEY,
                REFRESH_KEY, COPY_KEY, RESTART_KEY, null, DELETE_KEY, DELETE_FILE_KEY);
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
        MenuUtils.startQueueMenu = startQueueMenu;

        var stopQueueMenu = new Menu();
        stopQueueMenu.setGraphic(stopQueueLbl);
        MenuUtils.stopQueueMenu = stopQueueMenu;

        var queueSettingItem = new MenuItem();
        queueSettingItem.setGraphic(queueSettingLbl);

        initQueueMenuList(addToQueueMenu, startQueueMenu, stopQueueMenu);

        menuItems.put(addToQueueLbl, addToQueueMenu);
        menuItems.put(startQueueLbl, startQueueMenu);
        menuItems.put(stopQueueLbl, stopQueueMenu);
        menuItems.put(queueSettingLbl, queueSettingItem);
        c.getItems().addAll(menuItems.values());
        operationMenu.setContextMenu(c);

        operationMenu.setOnMouseClicked(event -> {
            var selectedItems = mainTableUtils.getSelected();
            disableMenuItems(resumeLbl, pauseLbl, openLbl, openFolderLbl, deleteFromQueueLbl, refreshLbl, copyLbl, restartLbl,
                    addToQueueLbl, deleteLbl, deleteWithFileLbl, menuItems, selectedItems);
            disableEnableStartStopQueue(startQueueMenu, stopQueueMenu);
            deleteLbl.setText("Delete selected (" + selectedItems.size() + ")");
            c.show(operationMenu, Side.BOTTOM, 0, 0);
        });

        menuItems.get(openLbl).setOnAction(e -> DownloadOpUtils.openFiles(mainTableUtils.getSelected()));
        menuItems.get(openFolderLbl).setOnAction(e -> DownloadOpUtils.openContainingFolder(mainTableUtils.getSelected().get(0)));
        menuItems.get(resumeLbl).setOnAction(e -> DownloadOpUtils.resumeDownloads(
                mainTableUtils.getSelected(), null, null));
        menuItems.get(pauseLbl).setOnAction(e -> DownloadOpUtils.pauseDownloads(mainTableUtils.getSelected()));
        menuItems.get(restartLbl).setOnAction(e -> DownloadOpUtils.restartDownloads(mainTableUtils.getSelected()));
        menuItems.get(exportLinkLbl).setOnAction(e -> DownloadOpUtils.exportLinks(mainTableUtils.getSelectedUrls()));
        menuItems.get(refreshLbl).setOnAction(e -> DownloadOpUtils.refreshDownload(mainTableUtils.getSelected()));
        menuItems.get(copyLbl).setOnAction(e -> FxUtils.setClipboard(mainTableUtils.getSelected().get(0).getUrl()));
        menuItems.get(deleteLbl).setOnAction(e -> DownloadOpUtils.deleteDownloads(mainTableUtils.getSelected(), false));
        menuItems.get(deleteWithFileLbl).setOnAction(e -> DownloadOpUtils.deleteDownloads(mainTableUtils.getSelected(), true));
        menuItems.get(newQueueLbl).setOnAction(e -> FxUtils.newQueueStage());
        menuItems.get(deleteFromQueueLbl).setOnAction(e -> deleteFromQueue());
        menuItems.get(queueSettingLbl).setOnAction(e -> FxUtils.newQueueSettingStage(null));
    }

    private static void disableEnableStartStopQueue(Menu startQueueMenu, Menu stopQueueMenu) {
        var startedQueueNames = startedQueues.stream().map(QueueModel::getName)
                .toList();
        startQueueMenu.getItems().stream()
                .filter(i -> startedQueueNames.contains(((Label) i.getGraphic()).getText()))
                .forEach(item -> item.setDisable(true));
        stopQueueMenu.getItems().stream()
                .filter(i -> startedQueueNames.contains(((Label) i.getGraphic()).getText()))
                .forEach(item -> item.setDisable(false));
    }

    public static void disableMenuItems(Label resumeLbl, Label pauseLbl, Label openLbl, Label openFolderLbl, Label deleteFromQueueLbl,
                                        Label refreshLbl, Label copyLbl, Label restartLbl, Label addToQueueLbl,
                                        Label deleteLbl, Label deleteWithFileLbl,
                                        LinkedHashMap<Label, MenuItem> menuItems,
                                        ObservableList<DownloadModel> selectedItems) {
        menuItems.get(openLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(openFolderLbl).setDisable(selectedItems.size() != 1);
        menuItems.get(resumeLbl).setDisable(selectedItems.isEmpty() || selectedItems.stream().anyMatch(dm -> !dm.isResumable()));
        menuItems.get(pauseLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(restartLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(refreshLbl).setDisable(selectedItems.size() != 1);
        menuItems.get(copyLbl).setDisable(selectedItems.size() != 1);
        menuItems.get(addToQueueLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(deleteLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(deleteWithFileLbl).setDisable(selectedItems.isEmpty());
        menuItems.get(deleteFromQueueLbl).setDisable(selectedItems.isEmpty());

        selectedItems.filtered(dm -> staticQueueNames.stream()
                        .anyMatch(s -> dm.getQueues().get(0).getName().equals(s)))
                .stream()
                .findFirst()
                .ifPresentOrElse(dm -> menuItems.get(deleteFromQueueLbl).setDisable(true),
                        () -> menuItems.get(deleteFromQueueLbl).setDisable(selectedItems.isEmpty()));

        selectedItems.stream().filter(dm -> dm.getDownloadStatus() == DownloadStatus.Paused)
                .findFirst().ifPresent(dm -> {
                    menuItems.get(pauseLbl).setDisable(true);
                    menuItems.get(openLbl).setDisable(true);
                    if (selectedItems.size() == 1)
                        menuItems.get(openFolderLbl).setDisable(true);
                    menuItems.get(restartLbl).setDisable(false);
                    menuItems.get(refreshLbl).setDisable(false);
                    menuItems.get(addToQueueLbl).setDisable(false);
                });
        selectedItems.stream().filter(dm -> dm.getDownloadStatus() == DownloadStatus.Downloading
                        || dm.getDownloadStatus() == DownloadStatus.Trying)
                .findFirst().ifPresent(dm -> {
                    menuItems.get(resumeLbl).setDisable(true);
                    menuItems.get(pauseLbl).setDisable(false);
                    menuItems.get(openLbl).setDisable(true);
                    if (selectedItems.size() == 1)
                        menuItems.get(openFolderLbl).setDisable(true);
                    menuItems.get(restartLbl).setDisable(true);
                    menuItems.get(refreshLbl).setDisable(true);
                    menuItems.get(addToQueueLbl).setDisable(true);
                });
        selectedItems.stream().filter(dm -> dm.getDownloadStatus() == DownloadStatus.Completed)
                .findFirst().ifPresent(dm -> {
                    menuItems.get(resumeLbl).setDisable(true);
                    menuItems.get(pauseLbl).setDisable(true);
                    menuItems.get(openLbl).setDisable(false);
                    if (selectedItems.size() == 1)
                        menuItems.get(openFolderLbl).setDisable(false);
                    menuItems.get(restartLbl).setDisable(false);
                    menuItems.get(refreshLbl).setDisable(false);
                    menuItems.get(addToQueueLbl).setDisable(false);
                });
    }

    public static void initMoreMenu(Button moreBtn, TableView<DownloadModel> table) {
        var c = new ContextMenu();
        var checkForUpdatesLbl = new Label("Check updates");
        var logsLbl = new Label("Logs");
        var aboutLbl = new Label("About");

        var lbls = List.of(checkForUpdatesLbl, logsLbl, aboutLbl);
        var menuItems = createMapMenuItems(lbls, null);
        c.getItems().addAll(menuItems.values());
        moreBtn.setContextMenu(c);
        moreBtn.setOnAction(event -> {
            table.getSelectionModel().clearSelection();
            c.show(moreBtn, Side.BOTTOM, 0, 0);
        });

        menuItems.get(checkForUpdatesLbl).setOnAction(e -> MoreUtils.checkUpdates(true));
        menuItems.get(logsLbl).setOnAction(e -> FxUtils.newLogsStage());
        menuItems.get(aboutLbl).setOnAction(e -> FxUtils.newAboutStage());
    }

    private static void initQueueMenuList(Menu addToQueueMenu, Menu startQueueMenu, Menu stopQueueMenu) {
        var addToQueueItems = new LinkedHashMap<MenuItem, QueueModel>();
        var startQueueItems = new LinkedHashMap<MenuItem, QueueModel>();
        var stopQueueItems = new LinkedHashMap<MenuItem, QueueModel>();
        var queues = QueueSubject.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, false);
        queues.forEach(qm -> {
            var defaultColor = ((Label) addToQueueMenu.getGraphic()).getTextFill();
            if (staticQueueNames.stream().noneMatch(s -> qm.getName().equals(s))) {
                var addToQueueMenuItem = createMenuItem(qm, defaultColor);
                addToQueueItems.put(addToQueueMenuItem, qm);
            }
            var startQueueMenuItem = createMenuItem(qm, defaultColor);
            var stopQueueMenuItem = createMenuItem(qm, defaultColor);
            stopQueueMenuItem.setDisable(true);
            startQueueItems.put(startQueueMenuItem, qm);
            stopQueueItems.put(stopQueueMenuItem, qm);
        });
        addToQueueMenu.getItems().addAll(addToQueueItems.keySet());
        startQueueMenu.getItems().addAll(startQueueItems.keySet());
        stopQueueMenu.getItems().addAll(stopQueueItems.keySet());

        initAddToQueueMenu(addToQueueMenu, addToQueueItems);

        startQueueMenu.getItems().forEach(startItem -> startItem.setOnAction(e ->
                stopQueueItems.keySet()
                        .stream()
                        .filter(stopItem -> ((Label) stopItem.getGraphic()).getText()
                                .equals(((Label) startItem.getGraphic()).getText()))
                        .findFirst()
                        .ifPresent(stopItem -> QueueUtils.startQueue(startQueueItems.get(startItem), startItem,
                                stopItem))
        ));

        stopQueueMenu.getItems().forEach(stopItem -> stopItem.setOnAction(e ->
                startQueueItems.keySet()
                        .stream()
                        .filter(startItem -> ((Label) startItem.getGraphic()).getText()
                                .equals(((Label) stopItem.getGraphic()).getText()))
                        .findFirst()
                        .ifPresent(startItem -> QueueUtils.stopQueue(stopQueueItems.get(stopItem), startItem, stopItem))
        ));
    }

    public static void deleteFromQueue() {
        var notObserved = new ArrayList<>(mainTableUtils.getSelected());
        var moveFiles = FxUtils.askToMoveFilesForQueues(notObserved, null);
        for (var dm : notObserved) {
            mainTableUtils.remove(dm);
            dm.getQueues()
                    .stream()
                    .filter(qm -> !staticQueueNames.contains(qm.getName()))
                    .findFirst()
                    .ifPresent(qm -> {
                        if (startedQueues.contains(qm))
                            startedQueues.get(startedQueues.indexOf(qm)).getDownloads().remove(dm);
                        DownloadsRepo.deleteDownloadQueue(dm.getId(), qm.getId());
                    });
            if (moveFiles) {
                var newFilePath = FileType.determineFileType(dm.getName()).getPath() + dm.getName();
                IOUtils.moveDownloadFilesFiles(dm, newFilePath);
            }
        }
    }

    public static void initAddToQueueMenu(Menu addToQueueMenu, LinkedHashMap<MenuItem, QueueModel> addToQueueItems) {
        addToQueueMenu.getItems().forEach(menuItem ->
                menuItem.setOnAction(e -> {
                    var qm = addToQueueItems.get(menuItem);
                    var notObserved = new ArrayList<>(mainTableUtils.getSelected());
                    var moveFiles = FxUtils.askToMoveFilesForQueues(notObserved, qm);
                    for (int i = 0; i < notObserved.size(); i++) {
                        var dm = notObserved.get(i);
                        if (dm.getQueues().contains(qm))
                            return;
                        if (staticQueueNames.stream().noneMatch(s -> dm.getQueues().get(0).getName().equals(s)))
                            mainTableUtils.remove(dm);
                        if (startedQueues.contains(qm))
                            startedQueues.get(startedQueues.indexOf(qm)).getDownloads().add(dm);
                        if (moveFiles) {
                            var newFilePath = FileType.determineFileType(dm.getName()).getPath() + dm.getName();
                            if (qm.hasFolder())
                                newFilePath = queuesPath + qm.getName() + File.separator + dm.getName();
                            IOUtils.moveDownloadFilesFiles(dm, newFilePath);
                        }
                        var addToQueueDate = LocalDateTime.now();
                        if (i != 0)
                            addToQueueDate = notObserved.get(i - 1).getAddToQueueDate().plusSeconds(1);
                        DownloadsRepo.updateDownloadQueue(dm.getId(), qm.getId(), addToQueueDate.toString());
                    }
                }));
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
