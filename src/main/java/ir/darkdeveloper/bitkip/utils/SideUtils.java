package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.function.Predicate;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.*;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.OTHERS_QUEUE;

public class SideUtils {


    public static void prepareSideTree(TreeView<String> sideTree,
                                       List<QueueModel> queues, MainTableUtils mainTableUtils) {

        var allItem = new TreeItem<>("");
        allItem.setExpanded(true);
        var categoryItem = new TreeItem<>("Categories");
        categoryItem.getChildren().addAll(createCategoryItemsForTree());
        categoryItem.setExpanded(true);
        var finishedItem = new TreeItem<>("Finished");
        finishedItem.getChildren().addAll(createCategoryItemsForTree());
        var unfinishedItem = new TreeItem<>("Unfinished");
        unfinishedItem.getChildren().addAll(createCategoryItemsForTree());

        var queuesItem = new TreeItem<>("Queues");
        queuesItem.setExpanded(true);
        var treeQueueItems = queues.stream()
                .filter(q -> !staticQueueNames.contains(q.getName()))
                .map(q -> new TreeItem<>(q.getName())).toList();
        queuesItem.getChildren().addAll(treeQueueItems);

        allItem.getChildren().addAll(List.of(categoryItem, finishedItem, unfinishedItem, queuesItem));
        sideTree.setRoot(allItem);
        sideTree.setShowRoot(true);
        sideTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        sideTree.setOnMouseClicked(SideUtils.onSideClicked(sideTree, mainTableUtils));
    }


    private static EventHandler<? super MouseEvent> onSideClicked(TreeView<String> sideTree, MainTableUtils mainTableUtils) {
        return event -> {
            TreeItem<String> selectedItem = sideTree.getSelectionModel().getSelectedItem();
            if (selectedItem == null)
                return;
            var itemName = selectedItem.getValue();
            System.out.println(itemName);
            if (itemName.equals("") || itemName.equals("Queues")) {
                sideTree.setContextMenu(null);
                return;
            }
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                // updates status of current downloading before changing queue
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
                mainTableUtils.setDownloads(downloadsData, staticQueueNames.contains(itemName));
            } else if (event.getButton().equals(MouseButton.SECONDARY)) {
                if (itemName.equals("Finished") || itemName.equals("Unfinished") || itemName.equals("Categories")) {
                    sideTree.setContextMenu(null);
                    return;
                }
                var cMenu = new ContextMenu();
                cMenu.getItems().clear();
                var startQueueLbl = new Label("Start queue");
                var stopQueueLbl = new Label("Stop queue");
                var scheduleLbl = new Label("Settings");
                var deleteLbl = new Label("Delete");

                List<Label> lbls;
                if (FileExtensions.staticQueueNames.stream().anyMatch(itemName::equals))
                    lbls = List.of(startQueueLbl, stopQueueLbl, scheduleLbl);
                else
                    lbls = List.of(startQueueLbl, stopQueueLbl, scheduleLbl, deleteLbl);
                var menuItems = MenuUtils.createMapMenuItems(lbls, null);
                cMenu.getItems().addAll(menuItems.values());
                sideTree.setContextMenu(cMenu);
                var qm = QueuesRepo.findByName(itemName, false);
                menuItems.get(startQueueLbl).setDisable(startedQueues.contains(qm));
                menuItems.get(stopQueueLbl).setDisable(!startedQueues.contains(qm));

                menuItems.get(startQueueLbl).setOnAction(e ->
                        QueueUtils.startQueue(qm, menuItems.get(startQueueLbl), menuItems.get(stopQueueLbl), mainTableUtils));
                menuItems.get(stopQueueLbl).setOnAction(e ->
                        QueueUtils.stopQueue(qm, menuItems.get(startQueueLbl), menuItems.get(stopQueueLbl), mainTableUtils));
                menuItems.get(scheduleLbl).setOnAction(e -> FxUtils.newQueueSettingStage(qm));
                if (menuItems.containsKey(deleteLbl))
                    menuItems.get(deleteLbl).setOnAction(e -> QueueUtils.deleteQueue(itemName));
                cMenu.show(selectedItem.getGraphic(), Side.BOTTOM, 0, 0);
            }
        };
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
        var allDownloads = new TreeItem<>(ALL_DOWNLOADS_QUEUE);
        allDownloads.setGraphic(new Label());
        var compressed = new TreeItem<>(COMPRESSED_QUEUE);
        compressed.setGraphic(new Label());
        var music = new TreeItem<>(MUSIC_QUEUE);
        music.setGraphic(new Label());
        var video = new TreeItem<>(VIDEOS_QUEUE);
        video.setGraphic(new Label());
        var program = new TreeItem<>(PROGRAMS_QUEUE);
        program.setGraphic(new Label());
        var docs = new TreeItem<>(DOCS_QUEUE);
        docs.setGraphic(new Label());
        var others = new TreeItem<>(OTHERS_QUEUE);
        others.setGraphic(new Label());
        return List.of(allDownloads, compressed, music, video, program, docs, others);
    }


}
