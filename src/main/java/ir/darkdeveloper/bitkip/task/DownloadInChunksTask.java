package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.controllers.DownloadingController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.utils.DownloadOpUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;

public class DownloadInChunksTask extends DownloadTask {
    private final int chunks;
    private final Long limit;
    private final List<FileChannel> fileChannels = new ArrayList<>();
    private final List<Path> filePaths = new ArrayList<>();
    private volatile boolean paused;
    private volatile boolean isCalculating;
    private final boolean isLimited;
    private ExecutorService executor;
    private int retries = 0;
    private boolean blocking;

    public DownloadInChunksTask(DownloadModel downloadModel, Long limit) {
        super(downloadModel);
        if (downloadModel.getChunks() == 0)
            throw new IllegalArgumentException("To download file in chunks, chunks must not be 0");
        this.chunks = downloadModel.getChunks();
        this.limit = limit;
        isLimited = limit != null;
    }


    @Override
    protected Long call() throws Exception {
        var url = new URL(downloadModel.getUrl());
        var file = new File(downloadModel.getFilePath());
        var fileSize = downloadModel.getSize();
        if (file.exists() && isCompleted(downloadModel, file, mainTableUtils))
            return 0L;
        downloadInChunks(url, fileSize);
        return 0L;
    }


    private void downloadInChunks(URL url, long fileSize)
            throws IOException, InterruptedException, ExecutionException {
        calculateSpeedAndProgressChunks(fileSize);
        var futures = prepareParts(url, fileSize);
        if (!futures.isEmpty()) {
            isCalculating = true;
            var futureArr = new CompletableFuture[futures.size()];
            futures.toArray(futureArr);
            CompletableFuture.allOf(futureArr).get();
        }
    }

    private List<CompletableFuture<Void>> prepareParts(URL url, long fileSize) throws IOException {
        var bytesForEach = fileSize / chunks;
        var futures = new ArrayList<CompletableFuture<Void>>();
        var to = bytesForEach;
        var from = 0L;
        var fromContinue = 0L;
        var filePath = downloadModel.getFilePath();
        var lastPartSize = fileSize - ((chunks - 1) * bytesForEach);
        for (int i = 0; i < chunks; i++, fromContinue = to, to += bytesForEach) {
            var name = filePath + "#" + i;
            filePaths.add(Paths.get(name));
            var partFile = new File(name);
            var existingFileSize = 0L;
            if (!partFile.exists()) {
                partFile.createNewFile();
                from = fromContinue;
            } else {
                existingFileSize = getCurrentFileSize(partFile);
                if (i + 1 != chunks && existingFileSize == bytesForEach)
                    continue;

                if (i + 1 == chunks && existingFileSize == lastPartSize)
                    continue;

                if (fromContinue == 0) {
                    if (i == 0) {
                        if (fromContinue >= to)
                            continue;
                        if (existingFileSize != 0)
                            from = existingFileSize + 1;
                    } else
                        from = fromContinue + existingFileSize;
                } else
                    from = fromContinue + existingFileSize;
            }

            if (i + 1 == chunks && to != fileSize) {
                to = fileSize;
                if (from == to)
                    break;
            }
            if (from != 0 && i == 0)
                from--;

            var finalTo = to - 1;
            var finalFrom = from;

            addFutures(url, existingFileSize, futures, partFile, finalTo, finalFrom, fromContinue);
        }
        return futures;
    }

    private void addFutures(URL url, long fileSize, ArrayList<CompletableFuture<Void>> futures,
                            File partFile, long finalTo, long finalFrom, long fromContinue) {
        CompletableFuture<Void> c;
        if (isLimited) {
            c = CompletableFuture.runAsync(() -> {
                try {
                    performLimitedDownload(url, fromContinue, finalFrom, finalTo, partFile, fileSize);
                } catch (IOException | InterruptedException e) {
                    this.pause();
                }
            }, executor);
        } else {
            c = CompletableFuture.runAsync(() -> {
                try {
                    performDownload(url, fromContinue, finalFrom, finalTo, partFile, fileSize);
                } catch (IOException | InterruptedException e) {
                    this.pause();
                }
            }, executor);
        }
        futures.add(c);
    }


    private void performDownload(URL url, long fromContinue, long from, long to, File partFile, long existingFileSize)
            throws InterruptedException, IOException {
        if (retries != downloadRetryCount) {
            try {
                var con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(3000);
                con.setConnectTimeout(3000);
                con.addRequestProperty("Range", "bytes=" + from + "-" + to);
                var out = new FileOutputStream(partFile, partFile.exists());
                var fileChannel = out.getChannel();
                fileChannels.add(fileChannel);
                var byteChannel = Channels.newChannel(con.getInputStream());
                fileChannel.transferFrom(byteChannel, existingFileSize, Long.MAX_VALUE);
                fileChannel.close();
                con.disconnect();
            } catch (SocketTimeoutException | UnknownHostException | SocketException s) {
                retries++;
                if (!paused) {
                    Thread.sleep(2000);
                    var currFileSize = getCurrentFileSize(partFile);
                    performDownload(url, fromContinue, fromContinue + currFileSize, to, partFile, currFileSize);
                }
            } catch (ClosedChannelException ignore) {}
            var currFileSize = getCurrentFileSize(partFile);
            if (!paused && currFileSize != (to - fromContinue + 1))
                performDownload(url, fromContinue, fromContinue + currFileSize, to, partFile, currFileSize);
        }
    }


    private void performLimitedDownload(URL url, long fromContinue, long from, long to, File partFile, long existingFileSize)
           throws IOException, InterruptedException {
       if (retries != downloadRetryCount) {
            try {
                var bytesToDownloadEachInCycle = limit / chunks;
                var con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(3000);
                con.setConnectTimeout(3000);
                con.addRequestProperty("Range", "bytes=" + from + "-" + to);
                var out = new FileOutputStream(partFile, partFile.exists());
                var fileChannel = out.getChannel();
                fileChannels.add(fileChannel);
                var byteChannel = Channels.newChannel(con.getInputStream());
                long finalExistingFileSize = existingFileSize;
                while (fromContinue + finalExistingFileSize < to) {
                    fileChannel.transferFrom(byteChannel, finalExistingFileSize, bytesToDownloadEachInCycle);
                    finalExistingFileSize += bytesToDownloadEachInCycle;
                    Thread.sleep(ONE_SEC);
                }
                fileChannel.close();
                con.disconnect();
            } catch (SocketTimeoutException | UnknownHostException s) {
                s.printStackTrace();
                retries++;
                if (!paused) {
                    Thread.sleep(2000);
                    var currFileSize = getCurrentFileSize(partFile);
                    performDownload(url, fromContinue, fromContinue + currFileSize, to, partFile, currFileSize);
                }
            }
            var currFileSize = getCurrentFileSize(partFile);
            if (!paused && currFileSize != (to - fromContinue + 1))
                performDownload(url, fromContinue, fromContinue + currFileSize, to, partFile, currFileSize);
        }
    }


    private void calculateSpeedAndProgressChunks(long fileSize) {
        executor.submit(() -> {
            try {
                while (!isCalculating) Thread.onSpinWait();
                while (!paused) {
                    var currentFileSize = 0L;

                    Thread.sleep(ONE_SEC);
                    for (int i = 0; i < chunks; i++)
                        currentFileSize += Files.size(filePaths.get(i));

                    updateProgress(currentFileSize, fileSize);
                    updateValue(currentFileSize);
                }
            } catch (NoSuchFileException ignore) {
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void pause() {
        paused = true;
        try {
            //this will cause execution get out of transferFrom
            for (var channel : fileChannels)
                channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void failed() {
        paused = true;
        pause();
    }

    @Override
    protected void succeeded() {
        try {
            for (var channel : fileChannels)
                channel.close();
            var dmOpt = currentDownloadings.stream()
                    .filter(c -> c.equals(downloadModel))
                    .findFirst();
            if (dmOpt.isPresent()) {
                var download = dmOpt.get();
                download.setDownloadStatus(DownloadStatus.Paused);
                if (IOUtils.mergeFiles(download, chunks, filePaths)) {
                    download.setCompleteDate(LocalDateTime.now());
                    download.setDownloadStatus(DownloadStatus.Completed);
                    download.setDownloaded(download.getSize());
                    download.setProgress(100);
                    mainTableUtils.refreshTable();
                    updateProgress(1, 1);
                    DownloadsRepo.updateDownloadCompleteDate(download);
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                            .findFirst().ifPresentOrElse(dc -> dc.onComplete(download),
                                    () -> {
                                        if (download.isShowCompleteDialog())
                                            DownloadOpUtils.openDownloadingStage(download);
                                    });
                    if (download.isOpenAfterComplete())
                        hostServices.showDocument(download.getFilePath());
                } else
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                            .forEach(DownloadingController::onPause);

                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadLastTryDate(download);

                currentDownloadings.remove(download);
                mainTableUtils.refreshTable();
            }
            if (executor != null && !blocking)
                executor.shutdown();
            System.gc();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
}
