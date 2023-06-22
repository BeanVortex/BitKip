package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.controllers.DetailsController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.utils.DownloadOpUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;

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
    private boolean blocking;
    private String url;

    public DownloadInChunksTask(DownloadModel downloadModel, Long limit) {
        super(downloadModel);
        if (downloadModel.getChunks() == 0)
            throw new IllegalArgumentException("To download file in chunks, chunks must not be 0");
        this.chunks = downloadModel.getChunks();
        this.limit = limit;
        isLimited = limit != null;
    }


    @Override
    protected Long call() {
        try {
            url = downloadModel.getUrl();
            var file = new File(downloadModel.getFilePath());
            var fileSize = downloadModel.getSize();
            if (file.exists() && isCompleted(downloadModel, file, mainTableUtils))
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
        calculateSpeedAndProgressChunks(fileSize);
        var futures = prepareParts(fileSize);
        if (!futures.isEmpty()) {
            isCalculating = true;
            log.info("Downloading : " + downloadModel);
            var futureArr = new CompletableFuture[futures.size()];
            futures.toArray(futureArr);
            CompletableFuture.allOf(futureArr).get();
        }
    }

    private List<CompletableFuture<Void>> prepareParts(long fileSize) throws IOException {
        var bytesForEach = fileSize / chunks;
        var futures = new ArrayList<CompletableFuture<Void>>();
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

    private void addFutures(long fileSize, ArrayList<CompletableFuture<Void>> futures,
                            File partFile, long fromContinue, long from, long to) {
        CompletableFuture<Void> c;
        if (isLimited) {
            c = CompletableFuture.supplyAsync(() -> {
                try {
                    performLimitedDownload(fromContinue, from, to, partFile, fileSize, 0, 0);
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
                    performDownload(fromContinue, from, to, partFile, fileSize, 0, 0);
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


    private void performDownload(long fromContinue, long from, long to, File partFile,
                                 long existingFileSize, int rateLimitCount, int retries)
            throws InterruptedException, IOException {
        try {
            var con = NewDownloadUtils.connect(url, 3000, 3000, false);
            con.addRequestProperty("Range", "bytes=" + fromContinue + "-" + to);
            if (!downloadModel.isResumable())
                con.setRequestProperty("User-Agent", userAgent);
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
                log.warn("Downloading part " + partFile.getName() + " failed. retry count : " + retries);
                Thread.sleep(2000);
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
            log.warn("Downloading part " + partFile.getName() + " limited. retry rate limit count : " + rateLimitCount);
            performDownload(from + currFileSize, from, to, partFile, currFileSize, rateLimitCount, retries);
        }

    }


    private void performLimitedDownload(long fromContinue, long from, long to,
                                        File partFile, long existingFileSize,
                                        int rateLimitCount, int retries) throws IOException, InterruptedException {
        if (retries != downloadRetryCount) {
            try {
                var bytesToDownloadEachInCycle = limit / chunks;
                var con = NewDownloadUtils.connect(url, 3000, 3000, false);
                if (!downloadModel.isResumable())
                    con.setRequestProperty("User-Agent", userAgent);
                con.addRequestProperty("Range", "bytes=" + fromContinue + "-" + to);
                var out = new FileOutputStream(partFile, partFile.exists());
                var fileChannel = out.getChannel();
                fileChannels.add(fileChannel);
                var byteChannel = Channels.newChannel(con.getInputStream());
                long finalExistingFileSize = existingFileSize;
                var lock = fileChannel.lock();
                while (from + finalExistingFileSize < to) {
                    fileChannel.transferFrom(byteChannel, finalExistingFileSize, bytesToDownloadEachInCycle);
                    finalExistingFileSize += bytesToDownloadEachInCycle;
                    Thread.sleep(ONE_SEC);
                }
                lock.release();
                fileChannel.close();
                con.disconnect();
            } catch (SocketTimeoutException | UnknownHostException s) {
                if (!paused) {
                    retries++;
                    log.warn("Downloading part " + partFile.getName() + " failed. retry count : " + retries);
                    Thread.sleep(2000);
                    var currFileSize = IOUtils.getFileSize(partFile);
                    performLimitedDownload(from + currFileSize, from, to, partFile, currFileSize, rateLimitCount, retries);
                }
            } catch (ClosedChannelException ignore) {
            }
            var currFileSize = IOUtils.getFileSize(partFile);
            if (!paused && currFileSize != (to - from + 1) && downloadRateLimitCount < rateLimitCount) {
                rateLimitCount++;
                performLimitedDownload(from + currFileSize, from, to, partFile, currFileSize, rateLimitCount, retries);
            }
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
        log.info("Paused download: " + downloadModel);
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
        log.info("Failed download: " + downloadModel);
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
                        openFile(download);
                } else
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                            .forEach(DetailsController::onPause);

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
}
