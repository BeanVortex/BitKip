package ir.darkdeveloper.bitkip.task;


import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.MainTableUtils;
import javafx.concurrent.Task;

import java.io.File;
import java.util.concurrent.ExecutorService;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentDownloadings;
import static ir.darkdeveloper.bitkip.config.AppConfigs.log;

/**
 * Value returned by tasks is for transfer speed
 * */
public abstract class DownloadTask extends Task<Long> {

    protected static final long ONE_SEC = 1000;
    protected DownloadModel downloadModel;

    public abstract void pause();

    public DownloadTask(DownloadModel downloadModel) {
        this.downloadModel = downloadModel;
    }


    protected boolean isCompleted(DownloadModel download, File file, MainTableUtils mainTableUtils) {
        try {
            var fs = download.getSize();
            var existingFileSize = IOUtils.getFileSize(file);
            if (fs != 0 && existingFileSize == fs) {
                download.setDownloadStatus(DownloadStatus.Completed);
                mainTableUtils.refreshTable();
                currentDownloadings.stream()
                        .filter(c -> c.equals(downloadModel))
                        .findAny()
                        .ifPresent(currentDownloadings::remove);
                return true;
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }
        return false;
    }

    public abstract void setExecutor(ExecutorService executor);

    public abstract boolean isPaused();

    public abstract void setBlocking(boolean blocking);

    public abstract void runBlocking();
}