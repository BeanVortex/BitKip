package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

import java.time.LocalDateTime;
import java.util.List;

public class TableUtils {


    private final TableView<DownloadModel> contentTable;
    private final ObservableList<DownloadModel> data = FXCollections.observableArrayList();

    public TableUtils(TableView<DownloadModel> contentTable) {
        this.contentTable = contentTable;
    }

    public void tableInits() {
        var nameColumn = new TableColumn<DownloadModel, String>("Name");
        var progressColumn = new TableColumn<DownloadModel, Double>("Progress");
        var sizeColumn = new TableColumn<DownloadModel, Long>("Size");
        var remainingColumn = new TableColumn<DownloadModel, Integer>("Remaining");
        var chunksColumn = new TableColumn<DownloadModel, Integer>("Chunks");
        var addDateColumn = new TableColumn<DownloadModel, String>("Add Date");
        var lastTryColumn = new TableColumn<DownloadModel, String>("Last Try");
        var completeColumn = new TableColumn<DownloadModel, String>("Complete On");

        nameColumn.setPrefWidth(200);
        sizeColumn.setPrefWidth(80);
        remainingColumn.setPrefWidth(80);
        addDateColumn.setPrefWidth(135);
        lastTryColumn.setPrefWidth(135);
        completeColumn.setPrefWidth(135);
        addDateColumn.setSortType(TableColumn.SortType.DESCENDING);

        List<TableColumn<DownloadModel, ?>> listOfColumns = List.of(nameColumn, progressColumn, sizeColumn,
                remainingColumn, chunksColumn, addDateColumn, lastTryColumn, completeColumn);
        contentTable.getColumns().addAll(listOfColumns);

        nameColumn.setCellValueFactory(p -> p.getValue().getNameProperty());
        sizeColumn.setCellValueFactory(p -> p.getValue().getSizeProperty().asObject());
        progressColumn.setCellValueFactory(p -> p.getValue().getProgressProperty().asObject());
        remainingColumn.setCellValueFactory(p -> p.getValue().getRemainingTimeProperty().asObject());
        chunksColumn.setCellValueFactory(p -> p.getValue().getChunksProperty().asObject());
        addDateColumn.setCellValueFactory(p -> p.getValue().getAddDateProperty());
        lastTryColumn.setCellValueFactory(p -> p.getValue().getLastTryDateProperty());
        completeColumn.setCellValueFactory(p -> p.getValue().getCompleteDateProperty());

        contentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        data.addAll(DownloadsRepo.getDownloads());
        contentTable.getItems().addAll(data);
        contentTable.getSortOrder().add(addDateColumn);

        contentTable.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.SECONDARY)) {
                var selectedItems = contentTable.getSelectionModel().getSelectedItems();
                if (selectedItems.size() > 1)
                    for (int i = 0; i < selectedItems.size(); i++) {
                        var d = LocalDateTime.now();
                        selectedItems.get(i).setName("df");
                        selectedItems.get(i).setProgress(13);
                        selectedItems.get(i).setCompleteDate(d);
                        selectedItems.get(i).getNameProperty().setValue("df");
                        selectedItems.get(i).getProgressProperty().setValue(160.5);
                        selectedItems.get(i).getCompleteDateProperty().setValue(d.toString());
                    }
            }
        });
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

    public void addRow(DownloadModel download) {
        contentTable.getItems().add(download);
        data.add(download);
        contentTable.sort();
    }
}
