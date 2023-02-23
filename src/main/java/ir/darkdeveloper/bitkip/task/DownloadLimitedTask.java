package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.utils.TableUtils;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DownloadLimitedTask extends DownloadTask {
    private boolean paused = false;
    private boolean isCalculating = false;
    private final long limit;
    private final boolean isSpeedLimited;
    private final TableUtils tableUtils;
    private final List<DownloadModel> currentDownloading = AppConfigs.currentDownloading;
    private File file;
    private ExecutorService executor;


    /**
     * if not isSpeedLimited, then valueLimit
     **/
    public DownloadLimitedTask(DownloadModel downloadModel, long limit, boolean isSpeedLimited, TableUtils tableUtils) {
        super(downloadModel);
        this.limit = limit;
        this.isSpeedLimited = isSpeedLimited;
        this.tableUtils = tableUtils;
    }


    @Override
    protected Long call() throws Exception {
        var url = new URL(downloadModel.getUrl());
        var connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(3000);
        file = new File(downloadModel.getFilePath());
        // for resume
        configureResume(connection, file);
        if (file.exists() && isCompleted(downloadModel, file))
            return 0L;

        var in = connection.getInputStream();
        var fileSize = downloadModel.getSize();

        var out = new FileOutputStream(file, file.exists());
        var fileChannel = out.getChannel();

        Platform.runLater(() -> {
            setOnCancelled(closeFileChannelEvent(fileChannel));
            setOnSucceeded(closeFileChannelEvent(fileChannel));
        });
        var existingFileSize = 0L;
        if (file.exists())
            existingFileSize = getCurrentFileSize(file);

        if (isSpeedLimited)
            return downloadSpeedLimited(fileChannel, in, file, limit, fileSize, existingFileSize);
        else {
            var statusExecutor = calculateSpeedAndProgress(file, fileSize);
            if (statusExecutor != null)
                downloadValueLimited(fileChannel, in, limit, existingFileSize, statusExecutor);
            return getCurrentFileSize(file);
        }

    }

    private long downloadSpeedLimited(FileChannel fileChannel, InputStream in,
                                      File file, long limit, long fileSize,
                                      long existingFileSize) throws IOException, InterruptedException {
        var start = System.currentTimeMillis();
        var byteChannel = Channels.newChannel(in);
        do {
            var beforeDown = System.currentTimeMillis();
            fileChannel.transferFrom(byteChannel, existingFileSize, limit);
            var afterDown = System.currentTimeMillis();

            var timeToWait = ONE_SEC;
            var downloadTime = afterDown - beforeDown;
            if (timeToWait >= downloadTime)
                timeToWait = ONE_SEC - downloadTime;
            else
                timeToWait = 0;
            Thread.sleep(timeToWait);
            var currentFileSize = getCurrentFileSize(file);
            existingFileSize = currentFileSize;
            updateProgress(currentFileSize, fileSize);
            updateValue(existingFileSize);
        } while (existingFileSize < fileSize && !paused);
        System.out.println("Lasted: " + (System.currentTimeMillis() - start));
        return existingFileSize;
    }

    private void downloadValueLimited(FileChannel fileChannel, InputStream in,
                                      long limit, long existingFileSize,
                                      ExecutorService statusExecutor) throws IOException {
        var byteChannel = Channels.newChannel(in);
        var s = System.currentTimeMillis();
        fileChannel.transferFrom(byteChannel, existingFileSize, limit);
        var e = System.currentTimeMillis() - s;
        paused = true;
        if (!statusExecutor.isShutdown())
            statusExecutor.shutdown();

        System.out.println("Lasted: " + e);
    }

    @Override
    protected void succeeded() {
        try {
            var index = currentDownloading.indexOf(downloadModel);
            if (index != -1) {
                var download = currentDownloading.get(index);
                download.setDownloadStatus(DownloadStatus.Paused);
                if (file.exists() && getCurrentFileSize(file) == downloadModel.getSize()) {
                    download.setCompleteDate(LocalDateTime.now());
                    download.setDownloadStatus(DownloadStatus.Completed);
                    download.setProgress(100);
                }
                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadCompleteDate(download);
                currentDownloading.remove(index);
                tableUtils.refreshTable();
            }
            executor.shutdown();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void failed() {
        pause();
    }

    private ExecutorService calculateSpeedAndProgress(File file, long fileSize) {
        if (isCalculating)
            return null;

        var executorService = Executors.newCachedThreadPool();
        executorService.submit(() -> {
            try {
                isCalculating = true;
                while (!paused) {
                    Thread.sleep(1000);
                    var currentFileSize = getCurrentFileSize(file);
                    updateProgress(currentFileSize, fileSize);
                    updateValue(currentFileSize);
                }
                isCalculating = false;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        return executorService;
    }

    private EventHandler<WorkerStateEvent> closeFileChannelEvent(FileChannel fileChannel) {
        return event -> {
            try {
                fileChannel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void configureResume(HttpURLConnection connection, File file) throws IOException {
        if (file.exists()) {
            var existingFileSize = getCurrentFileSize(file);
            connection.addRequestProperty("Range", "bytes=" + existingFileSize + "-");
        }
    }

    private boolean isCompleted(DownloadModel download, File file) {
        try {
            var fs = download.getSize();
            var existingFileSize = getCurrentFileSize(file);
            if (fs != 0 && existingFileSize == fs) {
                System.out.println("already downloaded");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void pause() {
        paused = true;
        succeeded();
        cancel();
    }

    @Override
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
}
