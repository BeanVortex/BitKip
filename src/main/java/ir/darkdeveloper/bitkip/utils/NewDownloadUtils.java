package ir.darkdeveloper.bitkip.utils;

import javafx.scene.control.TextField;

public class NewDownloadUtils {

    public static void validInputChecks(TextField chunksField, TextField bytesField, TextField speedField) {
        chunksField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                chunksField.setText(newValue.replaceAll("\\D", ""));
            if (!chunksField.getText().isBlank()) {
                var chunks = Integer.parseInt(chunksField.getText());
                var cores = Runtime.getRuntime().availableProcessors();
                if (chunks > cores * 2)
                    chunks = cores * 2;
                chunksField.setText(chunks + "");
            }
            if (bytesField != null)
                bytesField.setDisable(!chunksField.getText().equals("0"));
            speedField.setDisable(!chunksField.getText().equals("0"));
        });
        chunksField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && chunksField.getText().isBlank())
                chunksField.setText("0");
        });
        speedField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                chunksField.setText(newValue.replaceAll("\\D", ""));
        });

        speedField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && speedField.getText().isBlank())
                speedField.setText("0");
        });

    }


}
