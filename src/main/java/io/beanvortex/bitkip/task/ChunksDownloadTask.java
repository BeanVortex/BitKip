package io.beanvortex.bitkip.task;

import io.beanvortex.bitkip.utils.DownloadOpUtils;
import io.beanvortex.bitkip.utils.IOUtils;
import io.beanvortex.bitkip.controllers.DetailsController;
import io.beanvortex.bitkip.exceptions.DeniedException;
import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.models.DownloadStatus;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import io.beanvortex.bitkip.utils.DownloadUtils;
import javafx.application.Platform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static io.beanvortex.bitkip.config.AppConfigs.*;

public class ChunksDownloadTask extends DownloadTask {
    private final int chunks;
    private long speedLimit;
    private final long byteLimit;
    private boolean isSpeedLimited;
    private final List<FileChannel> fileChannels = new ArrayList<>();
    private final List<Path> filePaths = new ArrayList<>();
    private volatile boolean paused;
    private volatile boolean isCalculating;
    private final boolean isByteLimited;
    private ExecutorService executor;
    private boolean blocking;
    private String url;
    private long bytesToDownloadEachInCycleLimited;
    private boolean newLimitSet;

    public ChunksDownloadTask(DownloadModel downloadModel, long speedLimit, long byteLimit) throws DeniedException {
        super(downloadModel);
        if (downloadModel.getChunks() == 0)
            throw new IllegalArgumentException("To download file in chunks, chunks must not be 0");
        if (byteLimit == 0)
            throw new DeniedException("File did not download due to 0 bytes chosen to download");
        this.chunks = downloadModel.getChunks();
        this.speedLimit = speedLimit;
        this.byteLimit = byteLimit;
        isByteLimited = true;
        isSpeedLimited = speedLimit != 0;
    }


    @Override
    protected Long call() {
        try {
            url = downloadModel.getUri();
            var file = new File(downloadModel.getFilePath());
            var fileSize = downloadModel.getSize();
            if (file.exists() && isCompleted(file, mainTableUtils))
                return 0L;
            var parentFolder = Path.of(file.getPath()).getParent().toFile();
            if (!parentFolder.exists())
                parentFolder.mkdir();
            downloadInChunks(fileSize);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return 0L;
    }


    private void downloadInChunks(long fileSize)
            throws IOException, InterruptedException, ExecutionException {
        calculateSpeedAndProgress(fileSize);
        var futures = prepareParts(fileSize);
        if (!futures.isEmpty()) {
            isCalculating = true;
            log.info("Downloading : " + downloadModel);
            for (var future : futures)
                future.get();
        }
    }

    private List<Future<?>> prepareParts(long fileSize) throws IOException {
        var futures = new ArrayList<Future<?>>();
        var bytesForEach = fileSize / chunks;
        var to = bytesForEach;
        var from = 0L;
        var fromContinue = 0L;
        var tempFolderPath = Paths.get(downloadModel.getFilePath()).getParent() + File.separator + ".temp" + File.separator;
        if (!Files.exists(Path.of(tempFolderPath)))
            new File(tempFolderPath).mkdir();
        var lastPartSize = fileSize - ((chunks - 1) * bytesForEach);
        for (int i = 0; i < chunks; i++, from = to, to += bytesForEach) {
            var filePath = tempFolderPath + downloadModel.getName() + "#" + i;
            filePaths.add(Paths.get(filePath));
            var partFile = new File(filePath);
            var existingFileSize = 0L;
            if (!partFile.exists())
                partFile.createNewFile();
            else {
                existingFileSize = IOUtils.getFileSize(partFile);
                if (i + 1 != chunks && existingFileSize == bytesForEach)
                    continue;

                if (i + 1 == chunks && existingFileSize == lastPartSize)
                    continue;
            }

            fromContinue = from + existingFileSize;

            if (i + 1 == chunks && to != fileSize) {
                to = fileSize;
                if (fromContinue == to)
                    break;
            }

            var finalTo = to - 1;
            addFutures(existingFileSize, futures, partFile, fromContinue, from, finalTo);
        }
        return futures;
    }

    private void addFutures(long fileSize, ArrayList<Future<?>> futures,
                            File partFile, long fromContinue, long from, long to) {
        Future<?> c;
        if (isSpeedLimited) {
            bytesToDownloadEachInCycleLimited = speedLimit / chunks;
            c = executor.submit(() -> {
                try {
                    performSpeedLimitedDownload(fromContinue, from, to,
                            partFile, fileSize, 0, 0);
                } catch (IOException e) {
                    log.error(e.getMessage());
                    this.pause();
                }
            });
        } else {
            c = executor.submit(() -> {
                try {
                    performDownload(fromContinue, from, to, partFile, fileSize, 0, 0);
                } catch (IOException e) {
                    log.error(e.getMessage());
                    this.pause();
                }
            });
        }
        futures.add(c);
    }


    private void performDownload(long fromContinue, long from, long to, File partFile,
                                 long existingFileSize, int rateLimitCount, int retries)
            throws IOException {
        try {
            var con = DownloadUtils.connect(url);
            con.addRequestProperty("Range", "bytes=" + fromContinue + "-" + to);
            var out = new FileOutputStream(partFile, partFile.exists());
            var fileChannel = out.getChannel();
            fileChannels.add(fileChannel);
            var byteChannel = Channels.newChannel(con.getInputStream());
            fileChannel.transferFrom(byteChannel, existingFileSize, to - fromContinue + 1);
            fileChannels.remove(fileChannel);
            fileChannel.close();
            con.disconnect();
        } catch (SocketTimeoutException | UnknownHostException | SocketException s) {
            retries++;
            if (!paused && (continueOnLostConnectionLost || retries != downloadRetryCount)) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignore) {
                }
                var currFileSize = IOUtils.getFileSize(partFile);
                performDownload(from + currFileSize, from, to, partFile, currFileSize, rateLimitCount, retries);
            }
        } catch (ClosedChannelException ignore) {
        }

        // when connection has been closed by the server
        var currFileSize = IOUtils.getFileSize(partFile);
        if (!paused && currFileSize != (to - from + 1)
                && (continueOnLostConnectionLost || downloadRateLimitCount < rateLimitCount)) {
            rateLimitCount++;
            performDownload(from + currFileSize, from, to, partFile, currFileSize, rateLimitCount, retries);
        }

    }


    private void performSpeedLimitedDownload(long fromContinue, long from, long to,
                                             File partFile, long existingFileSize,
                                             int rateLimitCount, int retries) throws IOException {
        if (retries != downloadRetryCount) {
            try {
                var con = DownloadUtils.connect(url);
                if (!downloadModel.isResumable())
                    con.setRequestProperty("User-Agent", userAgent);
                con.addRequestProperty("Range", "bytes=" + fromContinue + "-" + to);
                var out = new FileOutputStream(partFile, partFile.exists());
                var fileChannel = out.getChannel();
                fileChannels.add(fileChannel);
                var byteChannel = Channels.newChannel(con.getInputStream());
                long finalExistingFileSize = existingFileSize;
                while (from + finalExistingFileSize < to) {
                    fileChannel.transferFrom(byteChannel, finalExistingFileSize, bytesToDownloadEachInCycleLimited);
                    finalExistingFileSize += bytesToDownloadEachInCycleLimited;
                    try {
                        Thread.sleep(ONE_SEC);
                    } catch (InterruptedException ignore) {
                    }
                }
                fileChannel.close();
                con.disconnect();
            } catch (SocketTimeoutException | UnknownHostException s) {
                if (!paused) {
                    retries++;
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignore) {
                    }
                    var currFileSize = IOUtils.getFileSize(partFile);
                    performSpeedLimitedDownload(from + currFileSize, from, to, partFile,
                            currFileSize, rateLimitCount, retries);
                }
            } catch (ClosedChannelException ignore) {
            }
            var currFileSize = IOUtils.getFileSize(partFile);
            if (!paused && currFileSize != (to - from + 1) && downloadRateLimitCount < rateLimitCount) {
                rateLimitCount++;
                performSpeedLimitedDownload(from + currFileSize, from, to, partFile, currFileSize
                        , rateLimitCount, retries);
            }
        }
    }


    private void calculateSpeedAndProgress(long fileSize) {
        Runnable runnable = () -> {
            Thread.currentThread().setName("calculator: " + Thread.currentThread().getName());
            try {
                while (!isCalculating) Thread.onSpinWait();
                Thread.sleep(ONE_SEC);
                while (!paused) {
                    var currentFileSize = 0L;
                    for (int i = 0; i < chunks; i++)
                        currentFileSize += Files.size(filePaths.get(i));
                    updateProgress(currentFileSize, fileSize);
                    updateValue(currentFileSize);
                    if (isByteLimited && currentFileSize >= byteLimit)
                        pause();
                    Thread.sleep(ONE_SEC);
                }
            } catch (InterruptedException | NoSuchFileException ignore) {
            } catch (IOException e) {
                log.error(e.getLocalizedMessage());
            }
        };
        if (lessCpuIntensive)
            new Thread(runnable).start();
        else
            executor.submit(runnable);
    }

    @Override
    public void pause() {
        paused = true;
        log.info("Paused download: " + downloadModel);
        try {
            //this will cause execution get out of transferFrom
            for (var channel : new ArrayList<>(fileChannels))
                if (channel != null)
                    channel.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    protected void failed() {
        log.info("Failed download: " + downloadModel);
        pause();
    }

    @Override
    protected void succeeded() {

        if (!Platform.isFxApplicationThread())
            runFinalization();
        else executor.submit(this::runFinalization);
    }

    private void runFinalization() {
        try {
            for (var channel : new ArrayList<>(fileChannels))
                if (channel != null)
                    channel.close();
            var dmOpt = currentDownloadings.stream()
                    .filter(c -> c.equals(downloadModel))
                    .findFirst();
            if (dmOpt.isPresent()) {
                var download = dmOpt.get();
                if (!newLimitSet)
                    download.setDownloadStatus(DownloadStatus.Paused);
                if (IOUtils.mergeFiles(download, chunks, filePaths)) {
                    log.info("File successfully downloaded: " + download);
                    download.setCompleteDate(LocalDateTime.now());
                    download.setDownloadStatus(DownloadStatus.Completed);
                    download.setDownloaded(download.getSize());
                    download.setProgress(100);
                    mainTableUtils.refreshTable();
                    DownloadsRepo.updateDownloadCompleteDate(download);
                    updateProgress(1, 1);
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                            .findFirst().ifPresentOrElse(dc -> dc.onComplete(download),
                                    () -> {
                                        if (download.isShowCompleteDialog())
                                            DownloadOpUtils.openDetailsStage(download);
                                    });
                    if (download.isOpenAfterComplete())
                        DownloadOpUtils.openFile(download);
                } else if (!newLimitSet)
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                            .forEach(DetailsController::onPause);

                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadLastTryDate(download);

            }
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            currentDownloadings.remove(downloadModel);
            mainTableUtils.refreshTable();
            if (executor != null && !blocking)
                executor.shutdownNow();
            System.gc();
            if (!newLimitSet) whenDone();
            else newLimitSet = false;
        }
    }

    @Override
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    @Override
    public void runBlocking() {
        try {
            call();
        } catch (Exception e) {
            log.error(e.getMessage());
            failed();
            return;
        }
        succeeded();
    }

    public void setSpeedLimit(long speedLimit) {
        this.speedLimit = speedLimit;
        if (isSpeedLimited && speedLimit != 0)
            bytesToDownloadEachInCycleLimited = speedLimit / chunks;
        else {
            newLimitSet = true;
            pause();
            try {
                DownloadOpUtils.triggerDownload(downloadModel, speedLimit, downloadModel.getSize(), true, false);
            } catch (ExecutionException | InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        this.isSpeedLimited = true;
    }
}
