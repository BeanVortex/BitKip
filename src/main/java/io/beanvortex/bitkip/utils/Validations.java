package io.beanvortex.bitkip.utils;

import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.repo.DatabaseHelper;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class Validations {

    public static void validateInputChecks(TextField chunksField, TextField bytesField, TextField speedField, DownloadModel dm) {
        validateChunksInput(chunksField);
        validateSpeedInput(speedField);
        validateBytesInput(bytesField, dm);
    }

    public static void validateBytesInput(TextField bytesField, DownloadModel dm) {
        if (bytesField == null)
            return;
        bytesField.textProperty().addListener((o, old, newValue) -> {
            var fileSize = dm.getSize();
            if (!newValue.matches("\\d*")) {
                if (newValue.equals("-1"))
                    newValue = "0";
                newValue = newValue.replaceAll("\\D", "");
                bytesField.setText(newValue);
            } else {
                if (fileSize > 0 && !newValue.isBlank() && Long.parseLong(newValue) > fileSize) {
                    bytesField.setText(String.valueOf(fileSize));
                } else
                    return;
                long newV;
                if (newValue.length() > 1) {
                    var stripped = StringUtils.stripStart(newValue, "0");
                    if (stripped.isBlank()) {
                        bytesField.setText(String.valueOf(fileSize));
                        return;
                    }
                    newV = Long.parseLong(stripped);
                } else
                    newV = Long.parseLong(newValue);

                if (newV > fileSize)
                    bytesField.setText(String.valueOf(fileSize));
            }
        });
        bytesField.focusedProperty().addListener((o, old, newValue) -> {
            if (!newValue && bytesField.getText().isBlank())
                bytesField.setText(String.valueOf(dm.getSize()));
        });
    }

    public static void validateSpeedInput(TextField speedField) {
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

    public static void validateChunksInput(TextField chunksField) {
        if (chunksField == null)
            return;
        var threads = maxChunks(Long.MAX_VALUE);
        validateIntInputCheck(chunksField, (long) threads, 0, threads);
        chunksField.setText(String.valueOf(threads));
    }

    public static void validateIntInputCheck(TextField field, Long defaultVal, Integer min, Integer max) {
        if (field == null)
            return;
        field.textProperty().addListener((o, old, newValue) -> {
            if (!newValue.matches("\\d*"))
                field.setText(newValue.replaceAll("\\D", ""));
            else {
                if (newValue.isBlank()) {
                    field.setText(String.valueOf(min));
                    return;
                }
                var newV = 0;
                if (newValue.length() > 1) {
                    var stripped = StringUtils.stripStart(newValue, "0");
                    if (stripped.isBlank()) {
                        field.setText(String.valueOf(min));
                        return;
                    }
                    newV = Integer.parseInt(stripped);
                } else
                    newV = Integer.parseInt(newValue);
                if (min != null && newV < min)
                    field.setText(String.valueOf(min));
                else if (max != null && newV > max)
                    field.setText(String.valueOf(max));
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
        validateIntInputCheck(hourSpinner.getEditor(), null, 0, 23);
        validateIntInputCheck(minuteSpinner.getEditor(), null, 0, 59);
        validateIntInputCheck(secondSpinner.getEditor(), null, 0, 59);
    }

    public static int maxChunks(long fileSize) {
        if (fileSize < 2_000_000)
            return 0;
        int processors = Runtime.getRuntime().availableProcessors();
        if (processors < 10)
            return processors * 2;
        return processors;
    }

    public static boolean validateUri(String uri) {
        return uri.startsWith("http") || uri.startsWith("https") || uri.startsWith("ftp");
    }

    public static String fixURIChars(String uri) {
        uri = uri.replace("{", "%7b");
        uri = uri.replace("}", "%7d");
        uri = uri.replace("[", "%5b");
        uri = uri.replace("]", "%5d");
        uri = uri.replace("`", "%60");
        uri = uri.replace("\"", "%22");
        uri = uri.replace(" ", "%20");
        uri = uri.replace("<", "%3c");
        uri = uri.replace(">", "%3e");
        return uri;
    }

    public static void fillNotFetchedData(DownloadModel dm) throws IOException {
        if (dm.getSize() > 0)
            return;

        // when download added and size not fetched
        var connection = DownloadUtils.connectWithInternetCheck(dm.getUri(), dm.getCredentials(), true);
        var canResume = DownloadUtils.canResume(connection);
        var fileSize = DownloadUtils.getFileSize(connection);
        dm.setSize(fileSize);
        dm.setResumable(canResume);
        if (!canResume) dm.setChunks(0);
        else dm.setChunks(maxChunks(dm.getSize()));
        if (fileSize == -1 || fileSize == 0)
            return;
        var observedDownload = AppConfigs.mainTableUtils.getObservedDownload(dm);
        if (observedDownload != null) {
            observedDownload.setSize(fileSize);
            observedDownload.setResumable(canResume);
            observedDownload.setChunks(dm.getChunks());
            AppConfigs.openDownloadings.stream()
                    .filter(dc -> dc.getDownloadModel().equals(dm))
                    .forEach(dc -> dc.setDownloadModel(dm));
            String[] cols = {DownloadsRepo.COL_SIZE, DownloadsRepo.COL_RESUMABLE, DownloadsRepo.COL_CHUNKS};
            String[] values = {String.valueOf(fileSize), canResume ? "1" : "0", String.valueOf(dm.getChunks())};
            DatabaseHelper.updateCols(cols, values, DatabaseHelper.DOWNLOADS_TABLE_NAME, dm.getId());
        }

    }
}
