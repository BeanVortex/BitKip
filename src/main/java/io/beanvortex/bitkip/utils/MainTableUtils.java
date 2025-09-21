package io.beanvortex.bitkip.utils;

import io.beanvortex.bitkip.config.observers.QueueSubject;
import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.models.DownloadStatus;
import io.beanvortex.bitkip.models.QueueModel;
import io.beanvortex.bitkip.repo.QueuesRepo;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static io.beanvortex.bitkip.config.AppConfigs.*;
import static io.beanvortex.bitkip.utils.Defaults.staticQueueNames;
import static io.beanvortex.bitkip.utils.MenuUtils.moveDownloadsToQueue;
import static io.beanvortex.bitkip.utils.QueueUtils.startFastQueue;
import static io.beanvortex.bitkip.utils.ShortcutUtils.*;


public class MainTableUtils {

    public static final DecimalFormat dFormat = new DecimalFormat("##.#");

    private final ClipboardContent dragContent = new ClipboardContent();
    private final List<File> dragFiles = new ArrayList<>();

    private final TableView<DownloadModel> contentTable;

    private final TableColumn<DownloadModel, String> nameColumn = new TableColumn<>("Name");
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
        nameColumn.setPrefWidth(250);
        speedColumn.setPrefWidth(100);
        downloadedColumn.setPrefWidth(90);
        sizeColumn.setPrefWidth(90);
        statusColumn.setPrefWidth(140);
        remainingColumn.setPrefWidth(80);
        addDateColumn.setPrefWidth(150);
        addToQueueDateColumn.setPrefWidth(150);
        lastTryColumn.setPrefWidth(150);
        completeColumn.setPrefWidth(150);

        List<TableColumn<DownloadModel, ?>> listOfColumns = List.of(nameColumn, speedColumn,
                downloadedColumn, sizeColumn, statusColumn, remainingColumn, chunksColumn, addDateColumn,
                lastTryColumn, completeColumn, addToQueueDateColumn);
        contentTable.getColumns().addAll(listOfColumns);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        speedColumn.setCellValueFactory(new PropertyValueFactory<>("speedString"));
        downloadedColumn.setCellValueFactory(new PropertyValueFactory<>("downloadedString"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("sizeString"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("downloadStatusString"));
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

    private void initDragDrop(TableRow<DownloadModel> row) {
        row.setOnDragDetected(event -> {
            Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
            var selected = getSelected();
            if (!selected.isEmpty()) {
                // Put the selected items in clipboard
                String ids = selected.stream()
                        .map(DownloadModel::getId)
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                selected.forEach(dm -> dragFiles.add(new File(dm.getFilePath())));

                dragContent.putString(ids);
                dragContent.putFiles(dragFiles);
                dragboard.setContent(dragContent);

                Text dragView = new Text("📋 " + selected.size() + (selected.size() > 1 ? " items" : " item"));
                dragboard.setDragView(dragView.snapshot(null, null));

                dragboard.setDragViewOffsetX(-15);
                dragboard.setDragViewOffsetY(15);

                event.consume();
            }
        });
        contentTable.setOnDragDone(me -> {
            dragContent.clear();
            dragFiles.clear();
            me.consume();
        });
    }


    private Callback<TableView<DownloadModel>, TableRow<DownloadModel>> getTableViewTableRowCallback() {

        return param -> {
            var row = new TableRow<DownloadModel>();

            row.setOnMouseClicked(event -> {
                var selectedItems = getSelected();
                var cMenu = createMenu(selectedItems);
                row.setContextMenu(cMenu);
                if (!row.isEmpty() && event.getButton().equals(MouseButton.SECONDARY))
                    cMenu.show(row, event.getScreenX(), event.getScreenY());
            });
            row.setOnMousePressed(e -> row.setContextMenu(null));
            initDragDrop(row);

            return row;
        };
    }

    private ContextMenu createMenu(ObservableList<DownloadModel> selectedItems) {
        var cMenu = new ContextMenu();
        var openLbl = new Label("Open");
        var openFolderLbl = new Label("Open folder");
        var resumeLbl = new Label("Resume");
        var pauseLbl = new Label("Pause");
        var pauseAllLbl = new Label("Pause all");
        var refreshLbl = new Label("Refresh URL");
        var copyLbl = new Label("Copy URL");
        var restartLbl = new Label("Restart");
        var detailsLbl = new Label("Details");
        var credentialsLbl = new Label("Change credentials");
        var locationLbl = new Label("Change location");
        var exportLinkLbl = new Label("Export selected");
        var deleteFromQueueLbl = new Label("Delete from this queue");
        var addToFastQueueLbl = new Label("Add to fast queue");
        var deleteLbl = new Label("Delete");
        var deleteWithFileLbl = new Label("Delete with file");
        var lbls = List.of(openLbl, openFolderLbl, resumeLbl, pauseLbl, pauseAllLbl,
                refreshLbl, copyLbl, restartLbl, detailsLbl, credentialsLbl, locationLbl, exportLinkLbl, addToFastQueueLbl, deleteFromQueueLbl,
                deleteLbl, deleteWithFileLbl);
        var keyCodes = Arrays.asList(OPEN_KEY, OPEN_FOLDER_KEY, RESUME_KEY,
                PAUSE_KEY, PAUSE_ALL_KEY, REFRESH_KEY, COPY_KEY, RESTART_KEY, DETAILS_KEY, null,
                LOCATION_KEY, null, null, null, DELETE_KEY, DELETE_FILE_KEY);
        var menuItems = MenuUtils.createMapMenuItems(lbls, keyCodes);

        var addToQueueMenu = new Menu();
        var addToQueueLbl = new Label("Add to queue");
        addToQueueMenu.setGraphic(addToQueueLbl);
        initAddToQueueMenu(addToQueueMenu);
        for (var item : menuItems.values()) {
            if (item.getGraphic().equals(deleteFromQueueLbl))
                cMenu.getItems().add(addToQueueMenu);
            cMenu.getItems().add(item);
        }

        menuItems.put(addToQueueLbl, addToQueueMenu);
        MenuUtils.disableMenuItems(resumeLbl, pauseLbl, pauseAllLbl, openLbl, openFolderLbl, deleteFromQueueLbl, addToFastQueueLbl,
                refreshLbl, copyLbl, restartLbl, locationLbl, exportLinkLbl, addToQueueLbl, deleteLbl,
                deleteWithFileLbl, menuItems, selectedItems);

        menuItems.get(openLbl).setOnAction(e -> DownloadOpUtils.openFiles(getSelected()));
        menuItems.get(openFolderLbl).setOnAction(e -> DownloadOpUtils.openContainingFolder(getSelected().getFirst().getFilePath()));
        menuItems.get(resumeLbl).setOnAction(e -> DownloadOpUtils.resumeDownloads(getSelected(), 0, 0, null));
        menuItems.get(pauseLbl).setOnAction(e -> DownloadOpUtils.pauseDownloads(getSelected()));
        menuItems.get(pauseAllLbl).setOnAction(e -> DownloadOpUtils.pauseAllDownloads());
        menuItems.get(refreshLbl).setOnAction(e -> DownloadOpUtils.refreshDownload(getSelected()));
        menuItems.get(copyLbl).setOnAction(e -> FxUtils.setClipboard(getSelected().getFirst().getUri()));
        menuItems.get(restartLbl).setOnAction(e -> DownloadOpUtils.restartDownloads(getSelected(), null));
        menuItems.get(detailsLbl).setOnAction(e -> getSelected().forEach(FxUtils::newDetailsStage));
        menuItems.get(credentialsLbl).setOnAction(e -> FxUtils.newChangeCredentialsStage(getSelected()));
        menuItems.get(locationLbl).setOnAction(e -> DownloadOpUtils.changeLocation(getSelected(), e));
        menuItems.get(exportLinkLbl).setOnAction(e -> DownloadOpUtils.exportLinks(getSelectedUrls()));
        menuItems.get(addToFastQueueLbl).setOnAction(e -> {
            var fastQueue = QueueModel.createFastQueue(mainTableUtils.getSelected());
            moveDownloadsToQueue(mainTableUtils.getSelected(), fastQueue);
            if (startFastQueue)
                startFastQueue(fastQueue);
        });
        menuItems.get(deleteFromQueueLbl).setOnAction(e -> MenuUtils.deleteFromQueue());
        menuItems.get(deleteLbl).setOnAction(e -> DownloadOpUtils.deleteDownloads(getSelected(), false));
        menuItems.get(deleteWithFileLbl).setOnAction(ev -> DownloadOpUtils.deleteDownloads(getSelected(), true));
        return cMenu;
    }

    private void initAddToQueueMenu(Menu addToQueueMenu) {
        var addToQueueItems = new LinkedHashMap<MenuItem, QueueModel>();
        var queues = QueueSubject.getQueues();
        if (queues.isEmpty())
            queues = QueuesRepo.getAllQueues(false, false);
        queues.forEach(qm -> {
            if (staticQueueNames.stream().noneMatch(s -> qm.getName().equals(s))) {
                var addToQueueMenuItem = MenuUtils.createMenuItem(qm);
                addToQueueItems.put(addToQueueMenuItem, qm);
            }
        });
        addToQueueMenu.getItems().addAll(addToQueueItems.keySet());
        MenuUtils.initAddToQueueMenu(addToQueueMenu, addToQueueItems);
    }


    private EventHandler<? super MouseEvent> onItemsClicked() {
        return event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                var selected = getSelected();
                if (!selected.isEmpty()) {
                    var dm = getSelected().getFirst();
                    DownloadOpUtils.openDetailsStage(dm);
                }
            }
        };
    }

    public void addRow(DownloadModel download) {
        var items = contentTable.getItems();
        if (!items.isEmpty()) {
            var dQueues = download.getQueues();
            for (var q : dQueues) {
                if (q.getName().equals(currentSelectedQueue)){
                    items.add(download);
                    contentTable.sort();
                    break;
                }
            }
        } else {
            items.add(download);
            contentTable.sort();
        }

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
        contentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        contentTable.setOnMouseClicked(onItemsClicked());
        contentTable.setRowFactory(getTableViewTableRowCallback());
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
                long delta = dm.getSize() - bytesDownloaded;
                if (speed != 0 && delta >= 0) {
                    try {
                        var remaining = DurationFormatUtils.formatDuration((delta / speed) * 1000, "dd:HH:mm:ss");
                        i.setRemainingTime(remaining);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    }
                }
                refreshTable();
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
            }
        }
        refreshTable();
    }

    public void updateDownloadedNoSize(long bytes, DownloadModel dm) {
        var downTask = dm.getDownloadTask();
        if (!downTask.isPaused() && currentDownloadings.contains(dm)) {
            var i = findDownload(dm.getId());
            if (i != null) {
                i.setSpeed(0);
                i.setDownloadStatus(DownloadStatus.Downloading);
                i.setSpeedString(IOUtils.formatBytes(0));
                i.setDownloaded(bytes);
                i.setRemainingTime("Not Clear");
                refreshTable();
            }
        }
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

    public List<String> getSelectedUrls() {
        return getSelected().stream()
                .map(DownloadModel::getUri)
                .toList();
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
