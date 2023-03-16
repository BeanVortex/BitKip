package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.controllers.DownloadingController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.MainTableUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
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
import java.util.concurrent.Executors;

import static ir.darkdeveloper.bitkip.config.AppConfigs.currentDownloadings;
import static ir.darkdeveloper.bitkip.config.AppConfigs.openDownloadings;

public class DownloadInChunksTask extends DownloadTask {
    private final int chunks;
    private final MainTableUtils mainTableUtils;
    private final Long limit;
    private final List<FileChannel> fileChannels = new ArrayList<>();
    private final List<Path> filePaths = new ArrayList<>();
    private volatile boolean paused;
    private volatile boolean isCalculating;
    private final boolean isLimited;
    private ExecutorService executor;

    public DownloadInChunksTask(DownloadModel downloadModel, MainTableUtils mainTableUtils, Long limit) {
        super(downloadModel);
        if (downloadModel.getChunks() == 0)
            throw new IllegalArgumentException("To download file in chunks, chunks must not be 0");
        this.chunks = downloadModel.getChunks();
        this.mainTableUtils = mainTableUtils;
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
        var partsExecutor = Executors.newCachedThreadPool();
        var statusExecutor = calculateSpeedAndProgressChunks(fileSize);
        var futures = prepareParts(url, fileSize, partsExecutor);
        if (!futures.isEmpty()) {
            isCalculating = true;
            var futureArr = new CompletableFuture[futures.size()];
            futures.toArray(futureArr);
            CompletableFuture.allOf(futureArr).get();
        }
        partsExecutor.shutdown();
        statusExecutor.shutdown();
        executor.shutdown();
        System.gc();
    }

    private List<CompletableFuture<Void>> prepareParts(URL url, long fileSize,
                                                       ExecutorService partsExecutor) throws IOException {
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

            if (isLimited)
                addFuturesLimited(url, partsExecutor, futures, to, from, partFile, existingFileSize);
            else
                addFutures(url, partsExecutor, futures, to, from, partFile, existingFileSize);
        }
        return futures;
    }

    private void addFutures(URL url, ExecutorService partsExecutor, ArrayList<CompletableFuture<Void>> futures, long to, long from, File partFile, long existingFileSize) {
        long finalTo = to - 1;
        var c = CompletableFuture.runAsync(() -> {
            try {
                var con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(3000);
                con.setConnectTimeout(3000);
                con.addRequestProperty("Range", "bytes=" + from + "-" + finalTo);
                var out = new FileOutputStream(partFile, partFile.exists());
                var fileChannel = out.getChannel();
                fileChannels.add(fileChannel);
                var byteChannel = Channels.newChannel(con.getInputStream());
                fileChannel.transferFrom(byteChannel, existingFileSize, Long.MAX_VALUE);
                fileChannel.close();
                con.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                this.pause();
                throw new RuntimeException(e);
            }
        }, partsExecutor);
        futures.add(c);
    }

    private void addFuturesLimited(URL url, ExecutorService partsExecutor, ArrayList<CompletableFuture<Void>> futures,
                                   long to, long from, File partFile, long existingFileSize) {
        long finalTo = to - 1;
        var bytesToDownloadEachInCycle = limit / chunks;
        var c = CompletableFuture.runAsync(() -> {
            try {
                var con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(3000);
                con.setConnectTimeout(3000);
                con.addRequestProperty("Range", "bytes=" + from + "-" + finalTo);
                var out = new FileOutputStream(partFile, partFile.exists());
                var fileChannel = out.getChannel();
                fileChannels.add(fileChannel);
                var byteChannel = Channels.newChannel(con.getInputStream());
                long finalExistingFileSize = existingFileSize;
                while (from + finalExistingFileSize < finalTo) {
                    fileChannel.transferFrom(byteChannel, finalExistingFileSize, bytesToDownloadEachInCycle);
                    finalExistingFileSize += bytesToDownloadEachInCycle;
                    Thread.sleep(ONE_SEC);
                }
                fileChannel.close();
                con.disconnect();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                this.pause();
                throw new RuntimeException(e);
            }
        }, partsExecutor);
        futures.add(c);
    }


    private ExecutorService calculateSpeedAndProgressChunks(long fileSize) {
        var statusExecutor = Executors.newCachedThreadPool();
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
        return statusExecutor;
    }

    @Override
    public void pause() {
        paused = true;
        succeeded();
    }

    @Override
    protected void failed() {
        paused = true;
        pause();
    }

    @Override
    protected void succeeded() {
        try {
            var index = currentDownloadings.indexOf(downloadModel);
            if (index != -1) {
                for (var channel : fileChannels)
                    channel.close();
                var download = currentDownloadings.get(index);
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
                            .forEach(dc -> dc.onComplete(download));
                } else
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                            .forEach(DownloadingController::onPause);

                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadLastTryDate(download);
                currentDownloadings.remove(index);
                mainTableUtils.refreshTable();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
}
