package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.task.DownloadTask;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.nio.file.Path;
import java.text.DecimalFormat;
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
        var sizeColumn = new TableColumn<DownloadModel, String>("Size");
        var statusColumn = new TableColumn<DownloadModel, String>("Status");
        var remainingColumn = new TableColumn<DownloadModel, String>("Remaining");
        var chunksColumn = new TableColumn<DownloadModel, Integer>("Chunks");
        var addDateColumn = new TableColumn<DownloadModel, String>("Added on");
        var lastTryColumn = new TableColumn<DownloadModel, String>("Last try");
        var completeColumn = new TableColumn<DownloadModel, String>("Completed On");

        nameColumn.setPrefWidth(200);
        speedColumn.setPrefWidth(100);
        sizeColumn.setPrefWidth(80);
        statusColumn.setPrefWidth(120);
        remainingColumn.setPrefWidth(80);
        addDateColumn.setPrefWidth(135);
        lastTryColumn.setPrefWidth(135);
        completeColumn.setPrefWidth(135);
        addDateColumn.setSortType(TableColumn.SortType.DESCENDING);

        List<TableColumn<DownloadModel, ?>> listOfColumns = List.of(nameColumn, progressColumn, speedColumn, sizeColumn,
                statusColumn, remainingColumn, chunksColumn, addDateColumn, lastTryColumn, completeColumn);
        contentTable.getColumns().addAll(listOfColumns);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progressString"));
        speedColumn.setCellValueFactory(new PropertyValueFactory<>("speedString"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("sizeString"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("downloadStatus"));
        remainingColumn.setCellValueFactory(new PropertyValueFactory<>("remainingTime"));
        chunksColumn.setCellValueFactory(new PropertyValueFactory<>("chunks"));
        addDateColumn.setCellValueFactory(new PropertyValueFactory<>("addDateString"));
        lastTryColumn.setCellValueFactory(new PropertyValueFactory<>("lastTryDateString"));
        completeColumn.setCellValueFactory(new PropertyValueFactory<>("completeDateString"));

        contentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        var downloadList = DownloadsRepo.getDownloads().stream()
                .peek(downloadModel -> {
                    downloadModel.setDownloadStatus(DownloadStatus.Paused);
                    if (downloadModel.getProgress() == 100)
                        downloadModel.setDownloadStatus(DownloadStatus.Completed);
                }).toList();
        data.addAll(downloadList);
        contentTable.getItems().addAll(data);
        contentTable.getSortOrder().add(addDateColumn);

        contentTable.setOnMouseClicked(onItemsClicked());
        contentTable.setRowFactory(param -> {
            var row = new TableRow<DownloadModel>();
            row.setOnMouseClicked(event -> {
                var selectedItems = contentTable.getSelectionModel().getSelectedItems();
                if (selectedItems.size() == 1 && !row.isEmpty()
                        && event.getButton().equals(MouseButton.SECONDARY))
                    System.out.println(row.getIndex());
            });
            return row;
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
                contentTable.refresh();
            }
        }
    }


    public DownloadModel findDownload(int id) {
        for (var d : data)
            if (id == d.getId())
                return d;
        return null;
    }
}
