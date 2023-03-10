package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.config.AppConfigs;
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

public class DownloadInChunksTask extends DownloadTask {
    private final int chunks;
    private final MainTableUtils mainTableUtils;
    private final List<DownloadModel> currentDownloading = AppConfigs.currentDownloading;
    private final List<FileChannel> fileChannels = new ArrayList<>();
    private final List<Path> filePaths = new ArrayList<>();
    private volatile boolean paused;
    private volatile boolean isCalculating = false;
    private ExecutorService executor;
    private ExecutorService partsExecutor;
    private ExecutorService statusExecutor;

    public DownloadInChunksTask(DownloadModel downloadModel, MainTableUtils mainTableUtils) {
        super(downloadModel);
        if (downloadModel.getChunks() == 0)
            throw new IllegalArgumentException("To download file in chunks, chunks must not be 0");
        this.chunks = downloadModel.getChunks();
        this.mainTableUtils = mainTableUtils;
    }


    @Override
    protected Long call() throws Exception {
        var url = new URL(downloadModel.getUrl());
        var file = new File(downloadModel.getFilePath());
        var fileSize = downloadModel.getSize();
        if (file.exists() && isCompleted(downloadModel, file))
            return 0L;
        downloadInChunks(url, fileSize);
        return 0L;
    }

    private boolean isCompleted(DownloadModel download, File file) {
        try {
            var fs = download.getSize();
            var existingFileSize = getCurrentFileSize(file);
            if (fs != 0 && existingFileSize == fs) {
                download.setDownloadStatus(DownloadStatus.Completed);
                mainTableUtils.refreshTable();
                currentDownloading.remove(downloadModel);
                System.out.println("already downloaded");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void downloadInChunks(URL url, long fileSize) throws IOException, InterruptedException, ExecutionException {
        partsExecutor = Executors.newCachedThreadPool();
        statusExecutor = calculateSpeedAndProgressChunks(fileSize);
        var futures = prepareParts(url, fileSize, partsExecutor);
        if (!futures.isEmpty()) {
            isCalculating = true;
            var futureArr = new CompletableFuture[futures.size()];
            futures.toArray(futureArr);
            CompletableFuture.allOf(futureArr).get();
        }
        if (paused)
            paused = false;
        statusExecutor.shutdown();
    }

    private List<CompletableFuture<Void>> prepareParts(URL url, long fileSize, ExecutorService partsExecutor) throws IOException {
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
            long finalExistingFileSize = existingFileSize;
            long finalTo = to - 1;
            if (from != 0 && i == 0)
                from--;
            long finalFrom = from;

            var c = CompletableFuture.runAsync(() -> {
                try {
                    var con = (HttpURLConnection) url.openConnection();
                    con.setReadTimeout(3000);
                    con.setConnectTimeout(3000);
                    con.addRequestProperty("Range", "bytes=" + finalFrom + "-" + finalTo);
                    var out = new FileOutputStream(partFile, partFile.exists());
                    var fileChannel = out.getChannel();
                    fileChannels.add(fileChannel);
                    var byteChannel = Channels.newChannel(con.getInputStream());
                    fileChannel.transferFrom(byteChannel, finalExistingFileSize, Long.MAX_VALUE);
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
        return futures;
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
        try {
            paused = true;
            for (var channel : fileChannels)
                channel.close();
            var index = currentDownloading.indexOf(downloadModel);
            if (index != -1) {
                var download = currentDownloading.get(index);
                download.setDownloadStatus(DownloadStatus.Paused);
                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadLastTryDate(download);
                currentDownloading.remove(index);
                mainTableUtils.refreshTable();
            }
            partsExecutor.shutdown();
            statusExecutor.shutdown();
            executor.shutdown();
            System.gc();
            cancel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void succeeded() {
        try {
            var index = currentDownloading.indexOf(downloadModel);
            if (index != -1) {
                paused = true;
                statusExecutor.shutdown();
                var download = currentDownloading.get(index);
                download.setDownloadStatus(DownloadStatus.Paused);
                if (IOUtils.mergeFiles(download, chunks, filePaths)) {
                    download.setCompleteDate(LocalDateTime.now());
                    download.setDownloadStatus(DownloadStatus.Completed);
                    download.setDownloaded(download.getSize());
                    download.setProgress(100);
                    mainTableUtils.refreshTable();
                    updateProgress(1, 1);
                    DownloadsRepo.updateDownloadCompleteDate(download);
                }
                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadLastTryDate(download);
                currentDownloading.remove(index);
                mainTableUtils.refreshTable();
            }
            if (partsExecutor != null)
                partsExecutor.shutdown();
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
}
