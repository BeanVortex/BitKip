package ir.darkdeveloper.bitkip.task;


import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class DownloadTask extends Task<Long> {

    long ONE_SEC = 1000;
    DownloadModel downloadModel;

    public abstract void pause();

    public DownloadTask(DownloadModel downloadModel) {
        this.downloadModel = downloadModel;
    }

    static long getCurrentFileSize(File file) throws IOException {
        return Files.size(Path.of(file.getPath()));
    }

}