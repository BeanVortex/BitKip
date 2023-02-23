package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;

public class TableUtils {


    private final TableView<DownloadModel> contentTable;
    private final ObservableList<DownloadModel> data = FXCollections.observableArrayList();
    private final List<DownloadModel> currentDownloading = AppConfigs.currentDownloading;

    public TableUtils(TableView<DownloadModel> contentTable) {
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
                var selectedItems = contentTable.getSelectionModel().getSelectedItems();
                if (!row.isEmpty() && event.getButton().equals(MouseButton.SECONDARY)) {
                    var cMenu = new ContextMenu();
                    var resumeLbl = new Label("resume");
                    var pauseLbl = new Label("pause");
                    var deleteLbl = new Label("delete");
                    var deleteWithFileLbl = new Label("delete with file");
                    if (selectedItems.size() == 1) {
                        var dm = selectedItems.get(0);
                        var lbls = List.of(resumeLbl, pauseLbl, deleteLbl, deleteWithFileLbl);
                        var menuItems = MenuUtils.createMenuItems(lbls);
                        switch (dm.getDownloadStatus()) {
                            case Downloading -> {
                                menuItems.get(0).setDisable(true);
                            }
                            case Paused -> {
                                menuItems.get(1).setDisable(true);
                            }
                            case Completed -> {
                                menuItems.get(0).setDisable(true);
                                menuItems.get(1).setDisable(true);
                            }
                        }

                        cMenu.getItems().addAll(menuItems);
                        row.setContextMenu(cMenu);
                        menuItemOperations(dm, menuItems);
                    }
                    cMenu.show(row, event.getX(), event.getY());
                }
            });
            return row;
        };
    }

    // sequence is important where labels defined
    private void menuItemOperations(DownloadModel dm, List<MenuItem> menuItems) {
        // resume
        menuItems.get(0).setOnAction(event -> {
            dm.setLastTryDate(LocalDateTime.now());
            NewDownloadUtils.startDownload(dm, this, null, null, true);
        });

        // pause
        menuItems.get(1).setOnAction(event -> {
            var download = AppConfigs.currentDownloading.get(AppConfigs.currentDownloading.indexOf(dm));
            download.getDownloadTask().pause();
        });

        // delete
        menuItems.get(2).setOnAction(event -> {
            var index = AppConfigs.currentDownloading.indexOf(dm);
            var download = dm;
            if (index != -1)
                download = AppConfigs.currentDownloading.get(index);
            if (download.getDownloadTask() != null && download.getDownloadTask().isRunning())
                download.getDownloadTask().pause();
            DownloadsRepo.deleteDownload(dm);
            contentTable.getItems().remove(dm);
        });


        // delete with file
        menuItems.get(3).setOnAction(event -> {
            try {
                var index = AppConfigs.currentDownloading.indexOf(dm);
                var download = dm;
                if (index != -1)
                    download = AppConfigs.currentDownloading.get(index);
                if (download.getDownloadTask() != null && download.getDownloadTask().isRunning())
                    download.getDownloadTask().pause();
                if (download.getChunks() == 0)
                    Files.deleteIfExists(Path.of(download.getFilePath()));
                else
                    for (int i = 0; i < download.getChunks(); i++)
                        Files.deleteIfExists(Path.of(download.getFilePath() + "#" + i));

                DownloadsRepo.deleteDownload(dm);
                contentTable.getItems().remove(dm);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }

    private EventHandler<? super MouseEvent> onItemsClicked() {
        return event -> {
            if (event.getButton().equals(MouseButton.SECONDARY)) {
                var selectedItems = contentTable.getSelectionModel().getSelectedItems();
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
        if (downTask.isRunning() && currentDownloading.size() != 0) {
            var i = findDownload(dm.getId());
            if (i != null) {
                i.setSpeed(speed);
                i.setDownloadStatus(DownloadStatus.Downloading);
                i.setSpeedString(IOUtils.formatBytes(speed));
                if (speed != 0) {
                    long delta = dm.getSize() - bytesDownloaded;
                    // todo bug chunks
                    var remaining = DurationFormatUtils.formatDuration((delta / speed) * 1000, "dd:HH:mm:ss");
                    i.setRemainingTime(remaining);
                }
                contentTable.refresh();
            }
        }
    }

    public void updateDownloadProgress(float progress, DownloadModel dm) {
        var downTask = dm.getDownloadTask();
        if (downTask.isRunning() && currentDownloading.size() != 0) {
            var i2 = currentDownloading.get(currentDownloading.indexOf(dm));
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
}
