package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.utils.TableUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadInChunksTask extends DownloadTask {
    private final int chunks;
    private final TableUtils tableUtils;
    private File file;
    private final List<DownloadModel> currentDownloading = AppConfigs.currentDownloading;
    private final List<FileChannel> fileChannels = new ArrayList<>();
    private final List<Path> filePaths = new ArrayList<>();
    private ExecutorService executor;
    private volatile boolean paused;
    private volatile boolean isCalculating = false;
    private ExecutorService partsExecutor;
    private ExecutorService statusExecutor;

    public DownloadInChunksTask(DownloadModel downloadModel, TableUtils tableUtils) {
        super(downloadModel);
        if (downloadModel.getChunks() == 0)
            throw new IllegalArgumentException("To download file in chunks, chunks must not be 0");
        this.chunks = downloadModel.getChunks();
        this.tableUtils = tableUtils;
    }


    @Override
    protected Long call() throws Exception {
        var url = new URL(downloadModel.getUrl());
        file = new File(downloadModel.getFilePath());
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
                tableUtils.refreshTable();
                currentDownloading.remove(downloadModel);
                System.out.println("already downloaded");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void downloadInChunks(URL url, long fileSize) throws IOException, InterruptedException {
        partsExecutor = Executors.newFixedThreadPool(chunks);
        var bytesForEach = fileSize / chunks;
        statusExecutor = calculateSpeedAndProgressChunks(fileSize);
        var callables = prepareParts(url, fileSize, bytesForEach);
        partsExecutor.invokeAll(callables);
        statusExecutor.shutdown();
        if (paused)
            paused = false;
    }

    private List<Callable<String>> prepareParts(URL url, long fileSize, long bytesForEach) throws IOException {
        var callables = new ArrayList<Callable<String>>();
        var to = bytesForEach;
        var from = 0L;
        var fromContinue = 0L;
        var filePath = downloadModel.getFilePath();
        for (int i = 0; i < chunks; i++) {
            var name = filePath + "#" + i;
            filePaths.add(Paths.get(name));
            var partFile = new File(name);
            var existingFileSize = 0L;
            if (!partFile.exists()) {
                partFile.createNewFile();
                from = fromContinue;
            } else {
                existingFileSize = getCurrentFileSize(partFile);
                if (existingFileSize == bytesForEach)
                    continue;
                if (from == 0)
                    from = existingFileSize;
                else
                    from = fromContinue + existingFileSize;
            }
            isCalculating = true;
            var out = new FileOutputStream(partFile, partFile.exists());
            var fileChannel = out.getChannel();
            fileChannels.add(fileChannel);
            var con = (HttpURLConnection) url.openConnection();
            con.setReadTimeout(3000);
            if (i + 1 == chunks && to != fileSize)
                to = fileSize;
            con.addRequestProperty("Range", "bytes=" + from + "-" + to);
            fromContinue = to + 1;
            to += bytesForEach;
            // todo: check if a part is done
            var finalExistingFileSize = existingFileSize;
            var c = (Callable<String>) () -> {
                try {
                    var byteChannel = Channels.newChannel(con.getInputStream());
                    fileChannel.transferFrom(byteChannel, finalExistingFileSize, Long.MAX_VALUE);
                    fileChannel.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return "Success";
            };
            callables.add(c);
        }
        return callables;
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
                DownloadsRepo.updateDownloadCompleteDate(download);
                currentDownloading.remove(index);
                tableUtils.refreshTable();
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
                var currentFileSize = 0L;
                for (int i = 0; i < chunks; i++)
                    currentFileSize += Files.size(filePaths.get(i));
                if (filePaths.stream().allMatch(path -> path.toFile().exists())
                        && currentFileSize == downloadModel.getSize()) {
                    if (!file.exists())
                        file.createNewFile();
                    for (int i = 0; i < chunks; i++) {
                        var name = file.getPath() + "#" + i;
                        try (
                                var in = new FileInputStream(name);
                                var out = new FileOutputStream(file.getPath(), file.exists());
                                var inputChannel = in.getChannel();
                                var outputChannel = out.getChannel()
                        ) {
                            var buffer = ByteBuffer.allocateDirect(1048576);
                            while (inputChannel.read(buffer) != -1) {
                                buffer.flip();
                                outputChannel.write(buffer);
                                buffer.clear();
                            }
                            updateProgress(1, 1);
                        }
                    }
                    download.setCompleteDate(LocalDateTime.now());
                    download.setDownloadStatus(DownloadStatus.Completed);
                    download.setDownloaded(download.getSize());
                    download.setProgress(100);
                    for (var f : filePaths)
                        Files.deleteIfExists(f);
                }
                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadCompleteDate(download);
                currentDownloading.remove(index);
                tableUtils.refreshTable();
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
