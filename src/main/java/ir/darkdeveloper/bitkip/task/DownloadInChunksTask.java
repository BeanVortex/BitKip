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

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.DownloadOpUtils.openFile;

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
        try {
            downloadInChunks(url, fileSize);
        }catch (Exception e){
            log.error(e.getMessage());
        }
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
        var tempFolderPath = Paths.get(downloadModel.getFilePath()).getParent().toString() + File.separator;
        if (tempFolderPath.contains("BitKip"))
            tempFolderPath = Paths.get(downloadModel.getFilePath()).getParent() + File.separator + ".temp" + File.separator;
        if (!Files.exists(Path.of(tempFolderPath)))
            new File(tempFolderPath).mkdir();
        var lastPartSize = fileSize - ((chunks - 1) * bytesForEach);
        for (int i = 0; i < chunks; i++, fromContinue = to, to += bytesForEach) {
            var filePath = tempFolderPath + downloadModel.getName() + "#" + i;
            filePaths.add(Paths.get(filePath));
            var partFile = new File(filePath);
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
            c = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Limited downloading part " + partFile.getName());
                    performLimitedDownload(url, fromContinue, finalFrom, finalTo, partFile, fileSize);
                } catch (IOException | InterruptedException e) {
                    if (e instanceof IOException)
                        log.error(e.getMessage());
                    this.pause();
                }
                return null;
            }, executor);
        } else {
            c = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("Downloading part " + partFile.getName());
                    performDownload(url, fromContinue, finalFrom, finalTo, partFile, fileSize);
                } catch (IOException | InterruptedException e) {
                    if (e instanceof IOException)
                        log.error(e.getMessage());
                    this.pause();
                }
                return null;
            }, executor);
        }
        c.whenComplete((unused, throwable) -> Thread.currentThread().interrupt());
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
                if (!paused) {
                    retries++;
                    log.warn("Downloading part " + partFile.getName() + " failed. retry count : " + retries);
                    Thread.sleep(2000);
                    var currFileSize = getCurrentFileSize(partFile);
                    performDownload(url, fromContinue, fromContinue + currFileSize, to, partFile, currFileSize);
                }
            } catch (ClosedChannelException ignore) {
            }
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
                if (!paused) {
                    retries++;
                    log.warn("Downloading part " + partFile.getName() + " failed. retry count : " + retries);
                    Thread.sleep(2000);
                    var currFileSize = getCurrentFileSize(partFile);
                    performLimitedDownload(url, fromContinue, fromContinue + currFileSize, to, partFile, currFileSize);
                }
            }
            var currFileSize = getCurrentFileSize(partFile);
            if (!paused && currFileSize != (to - fromContinue + 1))
                performLimitedDownload(url, fromContinue, fromContinue + currFileSize, to, partFile, currFileSize);
        }
    }


    private void calculateSpeedAndProgressChunks(long fileSize) {
        executor.submit(() -> {
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

                    Thread.sleep(ONE_SEC);
                }
            } catch (InterruptedException | NoSuchFileException ignore) {
            } catch (IOException e) {
                log.error(e.getLocalizedMessage());
            }
        });
    }

    @Override
    public void pause() {
        paused = true;
        log.info("Paused download: " + downloadModel.getName());
        try {
            //this will cause execution get out of transferFrom
            for (var channel : fileChannels)
                channel.close();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    @Override
    protected void failed() {
        log.info("Failed download: " + downloadModel.getName());
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
                    log.info("File successfully downloaded: " + download.getName());
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
                        openFile(download);
                } else
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                            .forEach(DownloadingController::onPause);

                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadLastTryDate(download);

                currentDownloadings.remove(download);
                mainTableUtils.refreshTable();
            }
            if (executor != null && !blocking)
                executor.shutdownNow();
            System.gc();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
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
