package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.LinkModel;
import javafx.application.Platform;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class ChunksCellFactory extends TableCell<LinkModel, Integer> {

    private final TextField textField = new TextField();
    private boolean committed = false;

    public ChunksCellFactory() {
        textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                commitEdit(Integer.valueOf(textField.getText().isBlank() ? "0" : textField.getText()));
            }
        });
        InputValidations.validChunksInputChecks(textField);
    }

    @Override
    protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null)
            setText("");
        else
            setText(String.valueOf(item));
    }


    @Override
    public void commitEdit(Integer newValue) {
        super.commitEdit(newValue);
    }

    @Override
    public void startEdit() {
        super.startEdit();
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        if (!isEmpty() && !committed) {
            setGraphic(textField);
            textField.setText(getText());
            Platform.runLater(textField::requestFocus);
            committed = true;
        } else
            cancelEdit();

    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setContentDisplay(ContentDisplay.TEXT_ONLY);
        committed = false;
    }


}
