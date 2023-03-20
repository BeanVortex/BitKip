package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.LinkModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

public class LinkTableUtils {

    private final TableView<LinkModel> table;
    private final ObservableList<LinkModel> data = FXCollections.observableArrayList();
    private static final KeyCodeCombination DEL = new KeyCodeCombination(KeyCode.DELETE);

    private final Stage stage;

    public LinkTableUtils(TableView<LinkModel> table, List<LinkModel> links, Stage stage) {
        this.table = table;
        this.stage = stage;
        data.addAll(links);
    }

    public void tableInits() {
        var nameCol = new TableColumn<LinkModel, String>("Name");
        var sizeCol = new TableColumn<LinkModel, String>("Size");
        var chunksCol = new TableColumn<LinkModel, Integer>("Chunks");
        var resumeCol = new TableColumn<LinkModel, String>("Resumeable");
        var queuesCol = new TableColumn<LinkModel, String>("Queues");
        var linkCol = new TableColumn<LinkModel, String>("Link");

        nameCol.setPrefWidth(300);
        sizeCol.setPrefWidth(90);
        chunksCol.setPrefWidth(70);
        resumeCol.setPrefWidth(90);
        queuesCol.setPrefWidth(70);
        linkCol.setPrefWidth(200);
        nameCol.setSortType(TableColumn.SortType.DESCENDING);

        List<TableColumn<LinkModel, ?>> listOfColumns = List.of(nameCol, sizeCol, chunksCol, resumeCol, queuesCol, linkCol);
        table.getColumns().addAll(listOfColumns);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("sizeString"));
        chunksCol.setCellValueFactory(new PropertyValueFactory<>("chunks"));
        resumeCol.setCellValueFactory(new PropertyValueFactory<>("resumeableString"));
        queuesCol.setCellValueFactory(new PropertyValueFactory<>("queuesString"));
        linkCol.setCellValueFactory(new PropertyValueFactory<>("link"));

        linkCol.setCellFactory(TextFieldTableCell.forTableColumn());
        linkCol.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setLink(e.getNewValue()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setName(e.getNewValue()));
        chunksCol.setCellFactory(param -> new ChunksCellFactory());
        chunksCol.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setChunks(e.getNewValue()));

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        table.getItems().clear();
        table.getItems().addAll(data);
        table.getSortOrder().add(nameCol);
        table.setEditable(true);
        table.setRowFactory(getTableViewTableRowCallback());
        shortcutActions();
    }

    private void shortcutActions() {
        Runnable delete = () -> {
            var selectedItems = getSelected();
            var notObservedDms = new ArrayList<>(selectedItems);
            table.getItems().removeAll(notObservedDms);
        };
        stage.getScene().getAccelerators().put(DEL, delete);
    }

    private Callback<TableView<LinkModel>, TableRow<LinkModel>> getTableViewTableRowCallback() {
        return param -> {
            var row = new TableRow<LinkModel>();
            row.setOnMouseClicked(event -> {
                var links = table.getSelectionModel().getSelectedItems();
                if (!row.isEmpty() && event.getButton().equals(MouseButton.SECONDARY)) {
                    var cMenu = new ContextMenu();
                    var deleteLbl = new Label("delete");
                    var lbls = List.of(deleteLbl);
                    var keyCodes = List.of(DEL);
                    var menuItems = MenuUtils.createMapMenuItems(lbls, keyCodes);
                    cMenu.getItems().addAll(menuItems.values());
                    menuItems.get(deleteLbl).setOnAction(e -> links.forEach(ln -> table.getItems().remove(ln)));
                    row.setContextMenu(cMenu);
                    cMenu.show(row, event.getX(), event.getY());
                }
            });
            return row;
        };
    }


    public void updateLink(LinkModel lm) {
        var l = findLinkModel(lm);
        if (l != null) {
            l.setSize(lm.getSize());
            l.setName(lm.getName());
            l.setResumeable(l.getResumeable());
            refreshTable();
        }
    }

    public LinkModel findLinkModel(LinkModel link) {
        for (var d : data)
            if (link.getLink().equals(d.getLink()))
                return d;
        return null;
    }

    public ObservableList<LinkModel> getSelected() {
        return table.getSelectionModel().getSelectedItems();
    }

    public void refreshTable() {
        table.refresh();
    }

    public ObservableList<LinkModel> getLinks() {
        return table.getItems();
    }
}
