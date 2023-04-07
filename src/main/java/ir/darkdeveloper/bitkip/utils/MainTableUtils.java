package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentDownloadings;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.staticQueueNames;
import static ir.darkdeveloper.bitkip.utils.MenuUtils.createMenuItem;
import static ir.darkdeveloper.bitkip.utils.ShortcutUtils.*;


public class MainTableUtils {


    private final TableView<DownloadModel> contentTable;

    private final TableColumn<DownloadModel, String> nameColumn = new TableColumn<>("Name");
    private final TableColumn<DownloadModel, String> progressColumn = new TableColumn<>("Progress");
    private final TableColumn<DownloadModel, String> speedColumn = new TableColumn<>("Speed");
    private final TableColumn<DownloadModel, String> downloadedColumn = new TableColumn<>("Downloaded");
    private final TableColumn<DownloadModel, String> sizeColumn = new TableColumn<>("Size");
    private final TableColumn<DownloadModel, String> statusColumn = new TableColumn<>("Status");
    private final TableColumn<DownloadModel, String> remainingColumn = new TableColumn<>("Remaining");
    private final TableColumn<DownloadModel, Integer> chunksColumn = new TableColumn<>("Chunks");
    private final TableColumn<DownloadModel, String> addDateColumn = new TableColumn<>("Added on");
    private final TableColumn<DownloadModel, String> addToQueueDateColumn = new TableColumn<>("Added to queue on");
    private final TableColumn<DownloadModel, String> lastTryColumn = new TableColumn<>("Last try");
    private final TableColumn<DownloadModel, String> completeColumn = new TableColumn<>("Completed On");

    public MainTableUtils(TableView<DownloadModel> contentTable) {
        this.contentTable = contentTable;
    }

    public void tableInits() {
        nameColumn.setPrefWidth(200);
        speedColumn.setPrefWidth(100);
        downloadedColumn.setPrefWidth(90);
        sizeColumn.setPrefWidth(90);
        statusColumn.setPrefWidth(120);
        remainingColumn.setPrefWidth(80);
        addDateColumn.setPrefWidth(150);
        addToQueueDateColumn.setPrefWidth(150);
        lastTryColumn.setPrefWidth(150);
        completeColumn.setPrefWidth(150);

        List<TableColumn<DownloadModel, ?>> listOfColumns = List.of(nameColumn, progressColumn, speedColumn,
                downloadedColumn, sizeColumn, statusColumn, remainingColumn, chunksColumn, addDateColumn,
                lastTryColumn, completeColumn, addToQueueDateColumn);
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
        addToQueueDateColumn.setCellValueFactory(new PropertyValueFactory<>("addToQueueDateString"));
        lastTryColumn.setCellValueFactory(new PropertyValueFactory<>("lastTryDateString"));
        completeColumn.setCellValueFactory(new PropertyValueFactory<>("completeDateString"));
        contentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
        var addToQueueItems = new LinkedHashMap<MenuItem, QueueModel>();
        var queues = AppConfigs.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, false);
        queues.forEach(qm -> {
            if (staticQueueNames.stream().noneMatch(s -> qm.getName().equals(s))) {
                var defaultColor = ((Label) addToQueueMenu.getGraphic()).getTextFill();
                var addToQueueMenuItem = createMenuItem(qm, defaultColor);
                addToQueueItems.put(addToQueueMenuItem, qm);
            }
        });
        addToQueueMenu.getItems().addAll(addToQueueItems.keySet());
        MenuUtils.initAddToQueueMenu(addToQueueMenu, this, addToQueueItems);
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
        menuItems.get(lbls.get(5)).setOnAction(e -> MenuUtils.deleteFromQueue(this));
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

    public void setDownloads(List<DownloadModel> dms, boolean addDateSort) {
        if (addDateSort) {
            addDateColumn.setSortType(TableColumn.SortType.DESCENDING);
            contentTable.getSortOrder().clear();
            contentTable.getSortOrder().add(addDateColumn);
        } else {
            addToQueueDateColumn.setSortType(TableColumn.SortType.DESCENDING);
            contentTable.getSortOrder().clear();
            contentTable.getSortOrder().add(addToQueueDateColumn);
        }
        contentTable.getItems().setAll(dms);
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
