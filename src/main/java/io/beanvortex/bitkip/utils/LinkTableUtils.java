package io.beanvortex.bitkip.utils;

import io.beanvortex.bitkip.controllers.BatchList;
import io.beanvortex.bitkip.models.LinkModel;
import io.beanvortex.bitkip.models.QueueModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.beanvortex.bitkip.config.AppConfigs.queuesPath;
import static io.beanvortex.bitkip.config.observers.QueueSubject.getQueues;
import static io.beanvortex.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static io.beanvortex.bitkip.utils.Defaults.staticQueueNames;

public class LinkTableUtils {

    private final TableView<LinkModel> table;
    private final ObservableList<LinkModel> data = FXCollections.observableArrayList();
    private static final KeyCodeCombination DEL = new KeyCodeCombination(KeyCode.DELETE);

    private final Stage stage;
    private final ComboBox<QueueModel> comboQueue;
    private TableColumn<LinkModel, QueueModel> queuesCol;

    public LinkTableUtils(TableView<LinkModel> table, List<LinkModel> links, ComboBox<QueueModel> comboQueue, Stage stage) {
        this.table = table;
        this.stage = stage;
        this.comboQueue = comboQueue;
        data.addAll(links);
    }

    public void tableInits() {
        var nameCol = new TableColumn<LinkModel, String>("Name");
        var sizeCol = new TableColumn<LinkModel, String>("Size");
        var chunksCol = new TableColumn<LinkModel, Integer>("Chunks");
        var resumeCol = new TableColumn<LinkModel, String>("Resumable");
        queuesCol = new TableColumn<>("Queue");
        var linkCol = new TableColumn<LinkModel, String>("Link");
        var pathCol = new TableColumn<LinkModel, String>("Path");

        nameCol.setPrefWidth(300);
        sizeCol.setPrefWidth(90);
        chunksCol.setPrefWidth(70);
        resumeCol.setPrefWidth(90);
        queuesCol.setPrefWidth(120);
        linkCol.setPrefWidth(200);
        pathCol.setPrefWidth(200);
        nameCol.setSortType(TableColumn.SortType.DESCENDING);

        List<TableColumn<LinkModel, ?>> listOfColumns = List.of(nameCol, sizeCol, chunksCol, resumeCol,
                queuesCol, linkCol, pathCol);
        table.getColumns().addAll(listOfColumns);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("sizeString"));
        chunksCol.setCellValueFactory(new PropertyValueFactory<>("chunks"));
        resumeCol.setCellValueFactory(new PropertyValueFactory<>("resumableString"));
        linkCol.setCellValueFactory(new PropertyValueFactory<>("uri"));
        pathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        queuesCol.setCellValueFactory(new PropertyValueFactory<>("queuesString"));

        linkCol.setCellFactory(TextFieldTableCell.forTableColumn());
        linkCol.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setUri(e.getNewValue()));
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pathCol.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setPath(e.getNewValue()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setName(e.getNewValue()));
        var queuesToShow = getQueues().stream()
                .filter(qm -> qm.getName().equals(ALL_DOWNLOADS_QUEUE) || !staticQueueNames.contains(qm.getName()))
                .toList();
        queuesCol.setCellFactory(p -> new ComboBoxTableCell<>(FXCollections.observableArrayList(queuesToShow)));
        queuesCol.setOnEditCommit(e -> changeLinkQueues(e.getRowValue(), e.getNewValue(), null, true));

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
                    var refreshLbl = new Label("refresh");
                    var deleteLbl = new Label("delete");
                    var lbls = List.of(refreshLbl, deleteLbl);
                    var keyCodes = List.of(new KeyCodeCombination(KeyCode.F5), DEL);
                    var menuItems = MenuUtils.createMapMenuItems(lbls, keyCodes);
                    cMenu.getItems().addAll(menuItems.values());
                    menuItems.get(refreshLbl).setOnAction(e -> links.forEach(this::updateLink));
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
            l.setChunks(Validations.maxChunks(l.getSize()));
            l.setResumable(l.getResumable());
            refreshTable();
        }
    }

    public LinkModel findLinkModel(LinkModel link) {
        for (var d : data)
            if (link.getUri().equals(d.getUri()))
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

    public void updateQueues() {
        var queuesToShow = getQueues().stream()
                .filter(qm -> qm.getName().equals(ALL_DOWNLOADS_QUEUE) || !staticQueueNames.contains(qm.getName()))
                .toList();
        queuesCol.setCellFactory(p -> new ComboBoxTableCell<>(FXCollections.observableArrayList(queuesToShow)));
    }

    public void changeQueues(QueueModel queue, BatchList.LocationData location) {
        location.setChange(false);
        table.getItems().forEach(lm -> changeLinkQueues(lm, queue, location, false));
        refreshTable();
    }

    public void changeLinkQueues(LinkModel lm, QueueModel newQueue, BatchList.LocationData location, boolean fromTable) {
        var queues = lm.getQueues()
                .stream().filter(qm -> staticQueueNames.contains(qm.getName()))
                .toList();
        lm.getQueues().clear();
        lm.getQueues().addAll(queues);
        lm.setPath(lm.getSelectedPath());
        if (fromTable)
            comboQueue.getSelectionModel().select(comboQueue.getItems().size() - 1);
        if (!newQueue.getName().equals(ALL_DOWNLOADS_QUEUE))
            lm.getQueues().add(newQueue);

        if (newQueue.hasFolder()) {
            var folder = new File(queuesPath + newQueue.getName());
            var path = folder.getAbsolutePath();
            if (!path.endsWith(File.separator))
                path += File.separator;
            lm.setPath(path);
            if (location != null)
                location.getLocationField().setText(path);
        } else if (location != null) {
            location.revertPath();
            lm.setPath(location.getFirstPath());
        }
    }
}
