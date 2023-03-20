package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentDownloadings;
import static ir.darkdeveloper.bitkip.utils.ShortcutUtils.*;


public class MainTableUtils {


    private final TableView<DownloadModel> contentTable;
    private final ObservableList<DownloadModel> data = FXCollections.observableArrayList();

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
        data.addAll(downloadList);
        contentTable.getItems().clear();
        contentTable.getItems().addAll(data);
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
                    var downloadingLbl = new Label("details");
                    var deleteLbl = new Label("delete");
                    var deleteWithFileLbl = new Label("delete with file");
                    var lbls = List.of(openLbl, resumeLbl, pauseLbl, downloadingLbl, deleteLbl, deleteWithFileLbl);
                    var keyCodes = List.of(OPEN_KEY, RESUME_KEY, PAUSE_KEY, DOWNLOADING_STAGE_KEY, DELETE_KEY, DELETE_FILE_KEY);
                    var menuItems = MenuUtils.createMapMenuItems(lbls, keyCodes);
                    MenuUtils.disableMenuItems(resumeLbl, pauseLbl,openLbl, menuItems, selectedItems);
                    cMenu.getItems().addAll(menuItems.values());
                    menuItemOperations(menuItems, lbls);
                    row.setContextMenu(cMenu);
                    cMenu.show(row, event.getX(), event.getY());
                }
            });
            return row;
        };
    }

    // sequence is important where labels defined
    private void menuItemOperations(LinkedHashMap<Label, MenuItem> menuItems, List<Label> lbls) {
        //OPEN
        menuItems.get(lbls.get(0)).setOnAction(e -> DownloadOpUtils.openFiles(getSelected()));
        // resume
        menuItems.get(lbls.get(1)).setOnAction(e ->
                DownloadOpUtils.resumeDownloads(this, getSelected(), null, null));
        // pause
        menuItems.get(lbls.get(2)).setOnAction(e -> DownloadOpUtils.pauseDownloads(this));
        //details
        menuItems.get(lbls.get(3)).setOnAction(e ->
                getSelected().forEach(dm -> FxUtils.newDownloadingStage(dm, this)));
        // delete
        menuItems.get(lbls.get(4)).setOnAction(e -> DownloadOpUtils.deleteDownloads(this, false));
        // delete with file
        menuItems.get(lbls.get(5)).setOnAction(ev -> DownloadOpUtils.deleteDownloads(this, true));
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
        data.add(download);
        contentTable.sort();
    }

    public void setDownloads(List<DownloadModel> downloadModels) {
        contentTable.getItems().setAll(downloadModels);
        data.setAll(downloadModels);
        contentTable.sort();
    }

    public void updateDownloadSpeedAndRemaining(long speed, DownloadModel dm, Long bytesDownloaded) {
        var downTask = dm.getDownloadTask();
        if (downTask.isRunning() && currentDownloadings.size() != 0) {
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
        if (downTask.isRunning() && currentDownloadings.size() != 0) {
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
        contentTable.refresh();

    }


    public DownloadModel findDownload(int id) {
        for (var d : data)
            if (id == d.getId())
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

    public void addRows(List<DownloadModel> downloads) {
        contentTable.getItems().addAll(downloads);
        data.addAll(downloads);
        contentTable.sort();
    }

    public void clearSelection() {
        contentTable.getSelectionModel().clearSelection();
    }
}
