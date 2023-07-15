package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;

import java.io.IOException;

import static ir.darkdeveloper.bitkip.config.AppConfigs.mainTableUtils;
import static ir.darkdeveloper.bitkip.config.AppConfigs.openDownloadings;

public class Validations {

    public static void validateInputChecks(TextField chunksField, TextField bytesField,
                                           TextField speedField, DownloadModel dm) {
        validateChunksInputChecks(chunksField);
        validateSpeedInputChecks(speedField);
        validateBytesInputChecks(bytesField, chunksField, speedField, dm);
    }

    private static void validateBytesInputChecks(TextField bytesField, TextField chunksField,
                                                 TextField speedField, DownloadModel dm) {
        if (bytesField == null || speedField == null)
            return;
        bytesField.setDisable(true);
        speedField.textProperty().addListener((o, old, newValue) -> bytesField.setDisable(!newValue.equals("0")));
        if (chunksField != null)
            chunksField.textProperty().addListener((o, old, newValue) -> bytesField.setDisable(!newValue.equals("0")));

        bytesField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                bytesField.setText(newValue.replaceAll("\\D", ""));
            if (chunksField != null && !newValue.equals(String.valueOf(dm.getSize())))
                chunksField.setDisable(!dm.isResumable());
            speedField.setDisable(!bytesField.getText().equals(String.valueOf(dm.getSize())));
            if (newValue.isBlank())
                bytesField.setText(String.valueOf(dm.getSize()));
        });
        bytesField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && bytesField.getText().isBlank())
                bytesField.setText(String.valueOf(dm.getSize()));
        });
    }

    public static void validateSpeedInputChecks(TextField speedField) {
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

    public static void validateChunksInputChecks(TextField chunksField) {
        if (chunksField == null)
            return;
        var threads = maxChunks(Long.MAX_VALUE);
        chunksField.setText(String.valueOf(threads));
        chunksField.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                chunksField.setText(newValue.replaceAll("\\D", ""));
            if (!chunksField.getText().isBlank()) {
                var chunks = Integer.parseInt(chunksField.getText());
                if (chunks > threads)
                    chunks = threads;
                if (chunks == 1)
                    chunks = 2;
                chunksField.setText(String.valueOf(chunks));
            }
        });
        chunksField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && chunksField.getText().isBlank())
                chunksField.setText(String.valueOf(threads));
        });
    }

    public static void validateIntInputCheck(TextField field, Long defaultVal) {
        if (field == null)
            return;
        field.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("\\D", ""));
                if (defaultVal != null && field.getText().isBlank())
                    field.setText(String.valueOf(defaultVal));
            }
        });
        field.focusedProperty().addListener((o, old, newValue) -> {
            if (defaultVal != null && !newValue && field.getText().isBlank())
                field.setText(String.valueOf(defaultVal));
        });
    }

    public static void validateIntInputCheck(TextField field, long defaultVal, long minValue, long maxValue) {
        if (field == null)
            return;
        field.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("\\D", ""));
                if (field.getText().isBlank())
                    field.setText(String.valueOf(defaultVal));
            } else {
                if (newValue.isBlank())
                    return;
                if (Long.parseLong(newValue) > maxValue)
                    field.setText(String.valueOf(maxValue));
                if (Long.parseLong(newValue) < minValue)
                    field.setText(String.valueOf(minValue));
            }
        });
        field.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && field.getText().isBlank())
                field.setText(String.valueOf(defaultVal));
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

    public static void validateTimePickerInputs(Spinner<Integer> hourSpinner,
                                                Spinner<Integer> minuteSpinner,
                                                Spinner<Integer> secondSpinner) {
        validateIntInputCheck(hourSpinner.getEditor(), null);
        validateIntInputCheck(minuteSpinner.getEditor(), null);
        validateIntInputCheck(secondSpinner.getEditor(), null);

        hourSpinner.getEditor().textProperty().addListener((o, o2, n) -> {
            if (n == null)
                return;
            if (n.isBlank())
                hourSpinner.getEditor().setText("0");
            if (!n.isBlank() && Integer.parseInt(n) > 23)
                hourSpinner.getEditor().setText("23");
        });
        zeroToSixtySpinner(minuteSpinner);
        zeroToSixtySpinner(secondSpinner);
    }

    private static void zeroToSixtySpinner(Spinner<Integer> minuteSpinner) {
        minuteSpinner.getEditor().textProperty().addListener((o, o2, n) -> {
            if (n == null)
                return;
            if (n.isBlank())
                minuteSpinner.getEditor().setText("0");
            if (!n.isBlank() && Integer.parseInt(n) > 59)
                minuteSpinner.getEditor().setText("59");
        });
    }


    public static int maxChunks(long fileSize) {
        if (fileSize < 2_000_000)
            return 0;
        return Runtime.getRuntime().availableProcessors() * 2;
    }

    public static boolean validateUrl(String url) {
        return url.startsWith("http") || url.startsWith("https") || url.startsWith("ftp");
    }

    public static void validateDownloadModel(DownloadModel dm) throws IOException {
        if (dm.getSize() > 0)
            return;

        // when added through batch list and size not fetched
        var connection = DownloadUtils.connect(dm.getUrl(), true);
        var canResume = DownloadUtils.canResume(connection);
        var fileSize = DownloadUtils.getFileSize(connection);
        dm.setSize(fileSize);
        dm.setResumable(canResume);
        if (!canResume)
            dm.setChunks(0);
        if (fileSize == -1 || fileSize == 0)
            return;
        var observedDownload = mainTableUtils.getObservedDownload(dm);
        if (observedDownload != null) {
            observedDownload.setSize(fileSize);
            observedDownload.setResumable(canResume);
            openDownloadings.stream()
                    .filter(dc -> dc.getDownloadModel().equals(dm))
                    .forEach(dc -> dc.setDownloadModel(dm));
            DownloadsRepo.updateDownloadProperty(DownloadsRepo.COL_SIZE, String.valueOf(fileSize), dm.getId());
            DownloadsRepo.updateDownloadProperty(DownloadsRepo.COL_RESUMABLE, String.valueOf(canResume), dm.getId());
        }

    }
}
