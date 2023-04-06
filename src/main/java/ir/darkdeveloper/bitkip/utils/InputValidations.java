package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;

public class InputValidations {

    public static void validInputChecks(TextField chunksField, TextField bytesField,
                                        TextField speedField, DownloadModel dm) {
        validChunksInputChecks(chunksField);
        validSpeedInputChecks(speedField);
        validBytesInputChecks(bytesField, chunksField, speedField, dm);
    }

    private static void validBytesInputChecks(TextField bytesField, TextField chunksField,
                                              TextField speedField, DownloadModel dm) {
        if (bytesField == null || speedField == null)
            return;
        speedField.textProperty().addListener((o, old, newValue) -> bytesField.setDisable(!newValue.equals("0")));
        if (chunksField != null)
            chunksField.textProperty().addListener((o, old, newValue) -> bytesField.setDisable(!newValue.equals("0")));

        bytesField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                bytesField.setText(newValue.replaceAll("\\D", ""));
            if (chunksField != null)
                chunksField.setDisable(!bytesField.getText().equals(dm.getSize() + ""));
            speedField.setDisable(!bytesField.getText().equals(dm.getSize() + ""));
            if (newValue.isBlank())
                bytesField.setText("" + dm.getSize());
        });
        bytesField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && bytesField.getText().isBlank())
                bytesField.setText("" + dm.getSize());
        });
    }

    public static void validSpeedInputChecks(TextField speedField) {
        if (speedField == null)
            return;
        speedField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d+\\.?\\d*"))
                speedField.setText(newValue.replaceAll("[a-zA-Z`!@#$%^&*()?<>,;:'\"\\-_+=]*", ""));
        });
        speedField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && speedField.getText().isBlank())
                speedField.setText("0");
        });
    }

    public static void validChunksInputChecks(TextField chunksField) {
        if (chunksField == null)
            return;
        chunksField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                chunksField.setText(newValue.replaceAll("\\D", ""));
            if (!chunksField.getText().isBlank()) {
                var chunks = Integer.parseInt(chunksField.getText());
                var cores = Runtime.getRuntime().availableProcessors();
                if (chunks > cores * 2)
                    chunks = cores * 2;
                if (chunks == 1)
                    chunks = 2;
                chunksField.setText(chunks + "");
            }
        });
        chunksField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && chunksField.getText().isBlank())
                chunksField.setText("0");
        });
    }

    public static void validIntInputCheck(TextField field, long defaultVal) {
        if (field == null)
            return;
        field.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("\\D", ""));
                if (field.getText().isBlank())
                    field.setText("" + defaultVal);
            }
        });
        field.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && field.getText().isBlank())
                field.setText("" + defaultVal);
        });
    }

    public static void validIntInputCheck(TextField field, long defaultVal, long minValue, long maxValue) {
        if (field == null)
            return;
        field.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("\\D", ""));
                if (field.getText().isBlank())
                    field.setText("" + defaultVal);
            } else {
                if (newValue.isBlank())
                    return;
                if (Long.parseLong(newValue) > maxValue)
                    field.setText("" + maxValue);
                if (Long.parseLong(newValue) < minValue)
                    field.setText("" + minValue);
            }
        });
        field.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && field.getText().isBlank())
                field.setText("" + defaultVal);
        });
    }

    public static void prepareLinkFromClipboard(TextField urlField) {
        var clip = Clipboard.getSystemClipboard();
        var clipContent = clip.getString();
        if (clipContent == null)
            return;
        if (clipContent.startsWith("http") || clipContent.startsWith("https"))
            urlField.setText(clipContent);
    }

    public static void validTimePickerInputs(Spinner<Integer> hourSpinner,
                                             Spinner<Integer> minuteSpinner,
                                             Spinner<Integer> secondSpinner) {
        validIntInputCheck(hourSpinner.getEditor(), 15);
        validIntInputCheck(minuteSpinner.getEditor(), 30);
        validIntInputCheck(secondSpinner.getEditor(), 0);

        hourSpinner.getEditor().textProperty().addListener((o, o2, n) -> {
            if (n == null)
                return;
            if (n.isBlank())
                hourSpinner.getEditor().setText("0");
            if (!n.isBlank() && Integer.parseInt(n) > 23)
                hourSpinner.getEditor().setText("23");
        });
        minuteSpinner.getEditor().textProperty().addListener((o, o2, n) -> {
            if (n == null)
                return;
            if (n.isBlank())
                minuteSpinner.getEditor().setText("0");
            if (!n.isBlank() && Integer.parseInt(n) > 59)
                minuteSpinner.getEditor().setText("59");
        });
        secondSpinner.getEditor().textProperty().addListener((o, o2, n) -> {
            if (n == null)
                return;
            if (n.isBlank())
                secondSpinner.getEditor().setText("0");
            if (!n.isBlank() && Integer.parseInt(n) > 59)
                secondSpinner.getEditor().setText("59");
        });
    }
}
