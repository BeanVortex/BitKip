package ir.darkdeveloper.bitkip.task;


import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class DownloadTask<T> extends Task<T> {

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

//    public void onStart() throws Exception {
//        downloadTask = new DownloadTask(field, field2);
//        downloadTask.valueProperty().addListener((observable, oldValue, newValue) -> {
//            if (oldValue == null)
//                oldValue = 0L;
//            var speed = (newValue - oldValue) * 2;
////            long fileSize = DownloadTask.readConfig();
//            // todo :remaining time
//            if (newValue == 0)
//                speed = 0;
//
//            speed /= 1000;
//            remainingLabel.setText("%d kB/s%n%n".formatted(speed));
//        });
//        progress.progressProperty().bind(downloadTask.progressProperty());
//        downloadTask.progressProperty().addListener((o, old, newV) -> {
//            progressLabel.setText("" + ((int) (newV.floatValue() * 100)));
//
//        });
//        var t = new Thread(downloadTask);
//        t.setDaemon(true);
//        t.start();
//    }
//
//    public void onPause() {
//        if (downloadTask != null)
//            downloadTask.pause();
//    }