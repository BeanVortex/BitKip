package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.controllers.DownloadingController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.utils.MainTableUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentDownloadings;
import static ir.darkdeveloper.bitkip.config.AppConfigs.openDownloadings;


public class DownloadLimitedTask extends DownloadTask {
    private boolean paused;
    private boolean isCalculating;
    private final long limit;
    private final boolean isSpeedLimited;
    private final MainTableUtils mainTableUtils;
    private File file;
    private ExecutorService executor;
    private FileChannel fileChannel;


    /**
     * if not isSpeedLimited, then valueLimit
     **/
    public DownloadLimitedTask(DownloadModel downloadModel, long limit, boolean isSpeedLimited, MainTableUtils mainTableUtils) {
        super(downloadModel);
        this.limit = limit;
        this.isSpeedLimited = isSpeedLimited;
        this.mainTableUtils = mainTableUtils;
    }


    @Override
    protected Long call() throws IOException, InterruptedException {
        file = new File(downloadModel.getFilePath());
        if (file.exists() && isCompleted(downloadModel, file))
            return 0L;
        performDownload();
        return getCurrentFileSize(file);
    }

    private void performDownload() throws IOException, InterruptedException {
        ExecutorService statusExecutor = null;
        try {
            var url = new URL(downloadModel.getUrl());
            var connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(3000);
            connection.setConnectTimeout(3000);
            configureResume(connection, file);
            var in = connection.getInputStream();
            var fileSize = downloadModel.getSize();

            var out = new FileOutputStream(file, file.exists());
            fileChannel = out.getChannel();

            var existingFileSize = 0L;
            if (file.exists())
                existingFileSize = getCurrentFileSize(file);

            if (isSpeedLimited)
                downloadSpeedLimited(fileChannel, in, file, limit, fileSize, existingFileSize);
            else {
                statusExecutor = calculateSpeedAndProgress(file, fileSize);
                if (statusExecutor != null)
                    downloadValueLimited(fileChannel, in, limit, existingFileSize);
            }
        } catch (SocketTimeoutException | UnknownHostException s) {
            s.printStackTrace();
            Thread.sleep(2000);
            performDownload();
        }
        var currFileSize = getCurrentFileSize(file);
        if (!paused && currFileSize != downloadModel.getSize())
            performDownload();
        paused = true;
        if (statusExecutor != null)
            statusExecutor.shutdown();
    }

    private void downloadSpeedLimited(FileChannel fileChannel, InputStream in,
                                      File file, long limit, long fileSize,
                                      long existingFileSize) throws IOException, InterruptedException {
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
    }

    private void downloadValueLimited(FileChannel fileChannel, InputStream in,
                                      long limit, long existingFileSize) throws IOException {
        var byteChannel = Channels.newChannel(in);
        fileChannel.transferFrom(byteChannel, existingFileSize, limit);
    }

    @Override
    protected void succeeded() {
        try {
            fileChannel.close();
            var index = currentDownloadings.indexOf(downloadModel);
            if (index != -1) {
                var download = currentDownloadings.get(index);
                download.setDownloadStatus(DownloadStatus.Paused);
                if (file.exists() && getCurrentFileSize(file) == downloadModel.getSize()) {
                    download.setCompleteDate(LocalDateTime.now());
                    download.setDownloadStatus(DownloadStatus.Completed);
                    download.setProgress(100);
                    download.setDownloaded(downloadModel.getSize());
                    updateProgress(1, 1);
                    DownloadsRepo.updateDownloadCompleteDate(download);
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                            .forEach(dc -> dc.onComplete(download));
                } else
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                            .forEach(DownloadingController::onPause);
                download.setDownloaded(getCurrentFileSize(file));
                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadLastTryDate(download);
                currentDownloadings.remove(index);
                mainTableUtils.refreshTable();
            }
            executor.shutdown();
            System.gc();
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
                    Thread.sleep(ONE_SEC);
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
    }

    @Override
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
}
