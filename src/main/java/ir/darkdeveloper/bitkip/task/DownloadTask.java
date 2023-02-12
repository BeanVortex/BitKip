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

    abstract void pause();


    static long saveOrGetFileSize(HttpURLConnection connection, File file, DownloadModel download) {
        if (!file.exists() && download.getSize() == 0) {
            long fileSize = connection.getContentLengthLong();
            download.setSize(fileSize);
            DownloadsRepo.updateDownloadSize(download);
        }
        return download.getSize();
    }

    static long getCurrentFileSize(File file) throws IOException {
        return Files.size(Path.of(file.getPath()));
    }
}