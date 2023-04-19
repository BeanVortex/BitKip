package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.*;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.OTHERS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.ShortcutUtils.DELETE_KEY;
import static ir.darkdeveloper.bitkip.utils.ShortcutUtils.NEW_QUEUE_KEY;

public class SideUtils {


    public static void prepareSideTree(TreeView<String> sideTree, List<QueueModel> queues) {

        var finishedIcon = createIcon("fas-check-square", "#81C784");
        var unFinishedIcon = createIcon("fas-pause-circle", "#FB8C00");

        var allItem = new TreeItem<>("All");
        allItem.setExpanded(true);
        allItem.setGraphic(createFolderIcon());
        var categoryItem = new TreeItem<>("Categories");
        categoryItem.setGraphic(createFolderIcon());
        categoryItem.getChildren().addAll(createCategoryItemsForTree());
        categoryItem.setExpanded(true);
        var finishedItem = new TreeItem<>("Finished");
        finishedItem.setGraphic(finishedIcon);
        finishedItem.getChildren().addAll(createCategoryItemsForTree());
        var unfinishedItem = new TreeItem<>("Unfinished");
        unfinishedItem.setGraphic(unFinishedIcon);
        unfinishedItem.getChildren().addAll(createCategoryItemsForTree());

        var queuesItem = new TreeItem<>("Queues");
        queuesItem.setExpanded(true);
        queuesItem.setGraphic(createFolderIcon());
        var treeQueueItems = queues.stream()
                .filter(q -> !staticQueueNames.contains(q.getName()))
                .map(SideUtils::createQueueItem).toList();
        queuesItem.getChildren().addAll(treeQueueItems);

        allItem.getChildren().addAll(List.of(categoryItem, finishedItem, unfinishedItem, queuesItem));
        sideTree.setRoot(allItem);
        sideTree.setShowRoot(true);
        sideTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        sideTree.setOnMouseClicked(SideUtils.onSideClicked(sideTree));
        sideTree.setOnKeyPressed(e -> {
            var selectedItem = sideTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                var itemName = selectedItem.getValue();
                if (selectedItem.getParent().getValue().equals("Queues") && e.getCode().equals(KeyCode.DELETE))
                    QueueUtils.deleteQueue(itemName);
            }
        });

    }


    private static EventHandler<? super MouseEvent> onSideClicked(TreeView<String> sideTree) {
        return event -> {
            TreeItem<String> selectedItem = sideTree.getSelectionModel().getSelectedItem();
            if (selectedItem == null)
                return;
            var itemName = selectedItem.getValue();
            if (itemName.equals("All")) {
                sideTree.setContextMenu(null);
                return;
            }
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                // updates status of current downloading before changing queue
                if (itemName.equals("Queues")) {
                    sideTree.setContextMenu(null);
                    return;
                }
                currentDownloadings.forEach(DownloadsRepo::updateTableStatus);
                Predicate<DownloadModel> condition = null;
                String queueToFetch = itemName;
                switch (itemName) {
                    case "Categories" -> queueToFetch = ALL_DOWNLOADS_QUEUE;
                    case "Finished" -> {
                        condition = dm -> dm.getProgress() == 100;
                        queueToFetch = ALL_DOWNLOADS_QUEUE;
                    }
                    case "Unfinished" -> {
                        condition = dm -> dm.getProgress() != 100;
                        queueToFetch = ALL_DOWNLOADS_QUEUE;
                    }
                    default -> {
                        switch (selectedItem.getParent().getValue()) {
                            case "Finished" -> condition = dm -> dm.getProgress() == 100;
                            case "Unfinished" -> condition = dm -> dm.getProgress() != 100;
                            default -> {
                            }
                        }
                    }
                }
                var downloadsData = fetchDownloadsOfQueue(queueToFetch, condition);
                var addDateSort = staticQueueNames.contains(itemName) || itemName.equals("Categories")
                        || itemName.equals("Finished") || itemName.equals("Unfinished");
                mainTableUtils.setDownloads(downloadsData, addDateSort);
            } else if (event.getButton().equals(MouseButton.SECONDARY)) {
                if (itemName.equals("Finished") || itemName.equals("Unfinished") || itemName.equals("Categories")) {
                    sideTree.setContextMenu(null);
                    return;
                }
                ContextMenu cMenu;
                if (itemName.equals("Queues"))
                    cMenu = createNewQueueMenu();
                else
                    cMenu = createTreeMenu(itemName);
                sideTree.setContextMenu(cMenu);
                cMenu.show(selectedItem.getGraphic(), Side.BOTTOM, 0, 0);

            }
        };
    }

    private static ContextMenu createNewQueueMenu() {
        var cMenu = new ContextMenu();
        var newQueueLbl = new Label("New queue");
        newQueueLbl.setPrefWidth(150);
        var menuItem = new MenuItem();
        menuItem.setGraphic(newQueueLbl);
        menuItem.setAccelerator(NEW_QUEUE_KEY);
        cMenu.getItems().add(menuItem);
        menuItem.setOnAction(e -> FxUtils.newQueueStage());
        return cMenu;
    }

    private static ContextMenu createTreeMenu(String itemName) {
        var cMenu = new ContextMenu();
        var startQueueLbl = new Label("Start queue");
        var stopQueueLbl = new Label("Stop  queue");
        var queueSettingLbl = new Label("Settings");
        var deleteLbl = new Label("Delete");

        List<Label> lbls;
        List<KeyCodeCombination> keys = null;
        if (FileExtensions.staticQueueNames.stream().anyMatch(itemName::equals))
            lbls = List.of(startQueueLbl, stopQueueLbl, queueSettingLbl);
        else {
            lbls = List.of(startQueueLbl, stopQueueLbl, queueSettingLbl, deleteLbl);
            keys = Arrays.asList(null, null, null, DELETE_KEY);
        }
        var menuItems = MenuUtils.createMapMenuItems(lbls, keys);
        cMenu.getItems().addAll(menuItems.values());
        var qm = QueuesRepo.findByName(itemName, false);
        menuItems.get(startQueueLbl).setDisable(startedQueues.contains(qm));
        menuItems.get(stopQueueLbl).setDisable(!startedQueues.contains(qm));

        menuItems.get(startQueueLbl).setOnAction(e ->
                QueueUtils.startQueue(qm, menuItems.get(startQueueLbl), menuItems.get(stopQueueLbl)));
        menuItems.get(stopQueueLbl).setOnAction(e ->
                QueueUtils.stopQueue(qm, menuItems.get(startQueueLbl), menuItems.get(stopQueueLbl)));
        menuItems.get(queueSettingLbl).setOnAction(e -> FxUtils.newQueueSettingStage(qm));
        if (menuItems.containsKey(deleteLbl))
            menuItems.get(deleteLbl).setOnAction(e -> QueueUtils.deleteQueue(itemName));
        return cMenu;
    }

    private static List<DownloadModel> fetchDownloadsOfQueue(String queueName, Predicate<? super DownloadModel> condition) {
        if (condition == null)
            condition = dm -> true;
        var queue = QueuesRepo.findByName(queueName, true);
        selectedQueue = queue;
        return queue.getDownloads()
                .stream()
                .filter(condition)
                .map(dm -> {
                    dm.setDownloadStatus(DownloadStatus.Paused);
                    if (dm.getProgress() == 100)
                        dm.setDownloadStatus(DownloadStatus.Completed);
                    // this will make downloads in currentDownloadings, observed by table
                    if (currentDownloadings.contains(dm))
                        return currentDownloadings.get(currentDownloadings.indexOf(dm));
                    return dm;
                }).toList();
    }

    private static List<TreeItem<String>> createCategoryItemsForTree() {
        var allDownloadsIcon = createIcon("fas-file-download", "#00BFA5");
        var compressedIcon = createIcon("fas-file-archive", "#00BFA5");
        var musicIcon = createIcon("fas-file-audio", "#00BFA5");
        var videoIcon = createIcon("fas-file-video", "#00BFA5");
        var programIcon = createIcon("fas-file-code", "#00BFA5");
        var docsIcon = createIcon("fas-file-invoice", "#00BFA5");
        var othersIcon = createIcon("fas-file", "#00BFA5");


        var allDownloads = new TreeItem<>(ALL_DOWNLOADS_QUEUE);
        allDownloads.setGraphic(allDownloadsIcon);
        var compressed = new TreeItem<>(COMPRESSED_QUEUE);
        compressed.setGraphic(compressedIcon);
        var music = new TreeItem<>(MUSIC_QUEUE);
        music.setGraphic(musicIcon);
        var video = new TreeItem<>(VIDEOS_QUEUE);
        video.setGraphic(videoIcon);
        var program = new TreeItem<>(PROGRAMS_QUEUE);
        program.setGraphic(programIcon);
        var docs = new TreeItem<>(DOCS_QUEUE);
        docs.setGraphic(docsIcon);
        var others = new TreeItem<>(OTHERS_QUEUE);
        others.setGraphic(othersIcon);
        return List.of(allDownloads, compressed, music, video, program, docs, others);
    }

    private static FontIcon createIcon(String iconCode, String value) {
        var icon = new FontIcon(iconCode);
        icon.setIconSize(15);
        icon.setFill(Paint.valueOf(value));
        return icon;
    }

    private static FontIcon createFolderIcon() {
        return createIcon("fas-folder", "#FFC107");
    }

    private static TreeItem<String> createQueueItem(QueueModel q) {
        var tree = new TreeItem<>(q.getName());
        if (q.getSchedule().isEnabled())
            tree.setGraphic(createIcon("fas-clock", "#FF6D00"));
        else
            tree.setGraphic(createIcon("fas-th-list", "#0091EA"));
        return tree;
    }

}
