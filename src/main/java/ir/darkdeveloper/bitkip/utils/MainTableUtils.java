package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentDownloadings;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.staticQueueNames;
import static ir.darkdeveloper.bitkip.utils.MenuUtils.createMenuItem;
import static ir.darkdeveloper.bitkip.utils.ShortcutUtils.*;


public class MainTableUtils {


    private final TableView<DownloadModel> contentTable;

    public MainTableUtils(TableView<DownloadModel> contentTable) {
        this.contentTable = contentTable;
    }

    public void tableInits() {
        var nameColumn = new TableColumn<DownloadModel, String>("Name");
        var progressColumn = new TableColumn<DownloadModel, String>("Progress");
        var speedColumn = new TableColumn<DownloadModel, String>("Speed");
        var downloadedColumn = new TableColumn<DownloadModel, String>("Downloaded");
        var sizeColumn = new TableColumn<DownloadModel, String>("Size");
        var statusColumn = new TableColumn<DownloadModel, String>("Status");
        var remainingColumn = new TableColumn<DownloadModel, String>("Remaining");
        var chunksColumn = new TableColumn<DownloadModel, Integer>("Chunks");
        var addDateColumn = new TableColumn<DownloadModel, String>("Added on");
        var lastTryColumn = new TableColumn<DownloadModel, String>("Last try");
        var completeColumn = new TableColumn<DownloadModel, String>("Completed On");

        nameColumn.setPrefWidth(200);
        speedColumn.setPrefWidth(100);
        downloadedColumn.setPrefWidth(90);
        sizeColumn.setPrefWidth(90);
        statusColumn.setPrefWidth(120);
        remainingColumn.setPrefWidth(80);
        addDateColumn.setPrefWidth(135);
        lastTryColumn.setPrefWidth(135);
        completeColumn.setPrefWidth(135);
        addDateColumn.setSortType(TableColumn.SortType.DESCENDING);

        List<TableColumn<DownloadModel, ?>> listOfColumns = List.of(nameColumn, progressColumn, speedColumn,
                downloadedColumn, sizeColumn, statusColumn, remainingColumn, chunksColumn, addDateColumn,
                lastTryColumn, completeColumn);
        contentTable.getColumns().addAll(listOfColumns);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progressString"));
        speedColumn.setCellValueFactory(new PropertyValueFactory<>("speedString"));
        downloadedColumn.setCellValueFactory(new PropertyValueFactory<>("downloadedString"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("sizeString"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("downloadStatus"));
        remainingColumn.setCellValueFactory(new PropertyValueFactory<>("remainingTime"));
        chunksColumn.setCellValueFactory(new PropertyValueFactory<>("chunks"));
        addDateColumn.setCellValueFactory(new PropertyValueFactory<>("addDateString"));
        lastTryColumn.setCellValueFactory(new PropertyValueFactory<>("lastTryDateString"));
        completeColumn.setCellValueFactory(new PropertyValueFactory<>("completeDateString"));

        contentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        var allDownloadsQueue = QueuesRepo.findByName("All Downloads");
        var downloadList = DownloadsRepo.getDownloads().stream()
                .peek(dm -> {
                    dm.setDownloadStatus(DownloadStatus.Paused);
                    if (dm.getProgress() == 100)
                        dm.setDownloadStatus(DownloadStatus.Completed);
                })
                .filter(dm -> dm.getQueue().contains(allDownloadsQueue))
                .toList();
        contentTable.getItems().clear();
        contentTable.getItems().addAll(downloadList);
        contentTable.getSortOrder().add(addDateColumn);
        contentTable.setOnMouseClicked(onItemsClicked());
        contentTable.setRowFactory(getTableViewTableRowCallback());
    }


    private Callback<TableView<DownloadModel>, TableRow<DownloadModel>> getTableViewTableRowCallback() {
        return param -> {
            var row = new TableRow<DownloadModel>();
            row.setOnMouseClicked(event -> {
                var selectedItems = getSelected();
                if (!row.isEmpty() && event.getButton().equals(MouseButton.SECONDARY)) {
                    var cMenu = new ContextMenu();
                    var openLbl = new Label("open");
                    var resumeLbl = new Label("resume");
                    var pauseLbl = new Label("pause");
                    var restartLbl = new Label("restart");
                    var downloadingLbl = new Label("details");
                    var deleteFromQueueLbl = new Label("delete from this queue");
                    var deleteLbl = new Label("delete");
                    var deleteWithFileLbl = new Label("delete with file");
                    var lbls = List.of(openLbl, resumeLbl, pauseLbl, restartLbl, downloadingLbl, deleteFromQueueLbl,
                            deleteLbl, deleteWithFileLbl);
                    var keyCodes = Arrays.asList(OPEN_KEY, RESUME_KEY, PAUSE_KEY, RESTART_KEY, DOWNLOADING_STAGE_KEY,
                            null, DELETE_KEY, DELETE_FILE_KEY);
                    var menuItems = MenuUtils.createMapMenuItems(lbls, keyCodes);

                    var addToQueueMenu = new Menu();
                    var addToQueueLbl = new Label("add to queue");
                    addToQueueMenu.setGraphic(addToQueueLbl);
                    initAddToQueueMenu(addToQueueMenu);
                    menuItemOperations(menuItems, lbls);
                    for (var item : menuItems.values()) {
                        if (item.getGraphic().equals(deleteFromQueueLbl))
                            cMenu.getItems().add(addToQueueMenu);
                        cMenu.getItems().add(item);
                    }

                    menuItems.put(addToQueueLbl, addToQueueMenu);
                    MenuUtils.disableMenuItems(resumeLbl, pauseLbl, openLbl, deleteFromQueueLbl, restartLbl,
                            addToQueueLbl, deleteLbl, deleteWithFileLbl, menuItems, selectedItems);

                    row.setContextMenu(cMenu);
                    cMenu.show(row, event.getX(), event.getY());
                }
            });
            return row;
        };
    }

    private void initAddToQueueMenu(Menu addToQueueMenu) {
        var addQueueItems = new LinkedHashMap<MenuItem, QueueModel>();
        QueuesRepo.getQueues().forEach(qm -> {
            if (staticQueueNames.stream().noneMatch(s -> qm.getName().equals(s))) {
                var defaultColor = ((Label) addToQueueMenu.getGraphic()).getTextFill();
                var addToQueueMenuItem = createMenuItem(qm, defaultColor);
                addQueueItems.put(addToQueueMenuItem, qm);
            }
        });
        addToQueueMenu.getItems().addAll(addQueueItems.keySet());

        addToQueueMenu.getItems().forEach(menuItem ->
                menuItem.setOnAction(event -> {
                    var qm = addQueueItems.get(menuItem);
                    var notObserved = new ArrayList<>(getSelected());
                    notObserved.forEach(dm -> {
                        if (dm.getQueue().contains(qm))
                            return;
                        if (staticQueueNames.stream().noneMatch(s -> dm.getQueue().get(0).getName().equals(s)))
                            remove(dm);
                        DownloadsRepo.updateDownloadQueue(dm.getId(), qm.getId());
                    });
                }));
    }

    // sequence is important where labels defined
    private void menuItemOperations(LinkedHashMap<Label, MenuItem> menuItems, List<Label> lbls) {
        // OPEN
        menuItems.get(lbls.get(0)).setOnAction(e -> DownloadOpUtils.openFiles(getSelected()));
        // RESUME
        menuItems.get(lbls.get(1)).setOnAction(e ->
                DownloadOpUtils.resumeDownloads(this, getSelected(), null, null));
        // PAUSE
        menuItems.get(lbls.get(2)).setOnAction(e -> DownloadOpUtils.pauseDownloads(this));
        // RESTART
        menuItems.get(lbls.get(3)).setOnAction(e -> System.out.println("restart"));
        // DETAILS
        menuItems.get(lbls.get(4)).setOnAction(e ->
                getSelected().forEach(dm -> FxUtils.newDownloadingStage(dm, this)));
        // DELETE FROM QUEUE
        menuItems.get(lbls.get(5)).setOnAction(e ->
                getSelected().forEach(dm -> {
                    remove(dm);
                    dm.getQueue()
                            .stream()
                            .filter(qm -> !staticQueueNames.contains(qm.getName()))
                            .findAny()
                            .ifPresent(qm -> DownloadsRepo.deleteDownloadQueue(dm.getId(), qm.getId()));
                }));
        // DELETE
        menuItems.get(lbls.get(6)).setOnAction(e -> DownloadOpUtils.deleteDownloads(this, false));
        // DELETE WITH FILE
        menuItems.get(lbls.get(7)).setOnAction(ev -> DownloadOpUtils.deleteDownloads(this, true));
    }

    private EventHandler<? super MouseEvent> onItemsClicked() {
        return event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                var dm = getSelected().get(0);
                DownloadOpUtils.openDownloadingStage(dm, this);
            }
        };
    }

    public void addRow(DownloadModel download) {
        contentTable.getItems().add(download);
        contentTable.sort();
    }

    public void setDownloads(List<DownloadModel> downloadModels) {
        contentTable.getItems().setAll(downloadModels);
        contentTable.sort();
    }

    public void updateDownloadSpeedAndRemaining(long speed, DownloadModel dm, Long bytesDownloaded) {
        var downTask = dm.getDownloadTask();
        if (!downTask.isPaused() && currentDownloadings.contains(dm)) {
            var i = findDownload(dm.getId());
            if (i != null) {
                i.setSpeed(speed);
                i.setDownloadStatus(DownloadStatus.Downloading);
                i.setSpeedString(IOUtils.formatBytes(speed));
                i.setDownloaded(bytesDownloaded);
                if (speed != 0) {
                    long delta = dm.getSize() - bytesDownloaded;
                    var remaining = DurationFormatUtils.formatDuration((delta / speed) * 1000, "dd:HH:mm:ss");
                    i.setRemainingTime(remaining);
                }
                contentTable.refresh();
            }
        }
    }

    public void updateDownloadProgress(float progress, DownloadModel dm) {
        var downTask = dm.getDownloadTask();
        if (!downTask.isPaused() && currentDownloadings.contains(dm)) {
            var i2 = currentDownloadings.get(currentDownloadings.indexOf(dm));
            i2.setProgress(progress);
            var i = findDownload(dm.getId());
            if (i != null) {
                i.setProgress(progress);
                i.setDownloadStatus(DownloadStatus.Downloading);
                if (progress == 100)
                    i.setDownloadStatus(DownloadStatus.Completed);
                i.setProgressString(new DecimalFormat("##.#").format(progress) + " %");
            }
        }
        refreshTable();

    }

    public DownloadModel findDownload(int id) {
        for (var d : contentTable.getItems())
            if (d.getId() == id)
                return d;
        return null;
    }

    public void refreshTable() {
        contentTable.refresh();
    }

    public ObservableList<DownloadModel> getSelected() {
        return contentTable.getSelectionModel().getSelectedItems();
    }

    public void remove(ObservableList<DownloadModel> selectedItems) {
        contentTable.getItems().removeAll(selectedItems);
    }

    public void remove(DownloadModel dm) {
        contentTable.getItems().remove(dm);
    }

    public void addRows(List<DownloadModel> downloads) {
        contentTable.getItems().addAll(downloads);
        contentTable.sort();
    }

    public void clearSelection() {
        contentTable.getSelectionModel().clearSelection();
    }

    public DownloadModel getObservedDownload(DownloadModel dm) {
        return findDownload(dm.getId());
    }

}
