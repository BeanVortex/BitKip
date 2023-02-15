package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import javafx.concurrent.Task;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ir.darkdeveloper.bitkip.task.DownloadTask.*;

public class DownloadInChunksTask extends DownloadTask {
    private final int chunks;
    private boolean paused;
    private File file;
    private final List<DownloadModel> currentDownloading = AppConfigs.currentDownloading;
    private final List<FileChannel> fileChannels = new ArrayList<>();
    private final List<Path> filePaths = new ArrayList<>();

    public DownloadInChunksTask(DownloadModel downloadModel) {
        super(downloadModel);
        if (downloadModel.getChunks() == 0)
            throw new IllegalArgumentException("To download file in chunks, chunks must not be 0");
        this.chunks = downloadModel.getChunks();
    }


    @Override
    protected Long call() throws Exception {
        var url = new URL(downloadModel.getUrl());
        file = new File(downloadModel.getFilePath());
        var fileSize = downloadModel.getSize();
        downloadInChunks(url, fileSize);
        return 0L;
    }


    private void downloadInChunks(URL url, long fileSize) throws IOException, InterruptedException {

        var bytesForEach = fileSize / chunks;
        var threadList = new ArrayList<Thread>();
        var to = bytesForEach;
        var from = 0L;
        var fromContinue = 0L;
        var filePath = downloadModel.getFilePath();
        Thread countingThread = null;
        for (int i = 0; i < chunks; i++) {
            var name = filePath + "#" + i;
            filePaths.add(Paths.get(name));
            var partFile = new File(name);
            if (!partFile.exists()) {
                partFile.createNewFile();
                from = fromContinue + getCurrentFileSize(partFile);
            } else {
                if (from == 0)
                    from = getCurrentFileSize(partFile);
                else
                    from = fromContinue + getCurrentFileSize(partFile);
            }
            var out = new FileOutputStream(partFile, partFile.exists());
            var fileChannel = out.getChannel();
            fileChannels.add(fileChannel);
            var con = (HttpURLConnection) url.openConnection();
            if (i + 1 == chunks && to != fileSize)
                to = fileSize;
            con.addRequestProperty("Range", "bytes=" + from + "-" + to);
            fromContinue = to + 1;
            to += bytesForEach;
            var existingFileSize = getCurrentFileSize(partFile);
            countingThread = calculateSpeedAndProgressChunks(fileSize);
            // todo: check if a part is done
            var t = new Thread(() -> {
                try {
                    var byteChannel = Channels.newChannel(con.getInputStream());
                    fileChannel.transferFrom(byteChannel, existingFileSize, Long.MAX_VALUE);
                    fileChannel.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            threadList.add(t);
            t.start();
        }

        for (var thread : threadList)
            thread.join();
        if (countingThread != null)
            countingThread.interrupt();

        if (paused)
            paused = false;
    }

    private Thread calculateSpeedAndProgressChunks(long fileSize) {
        var t = new Thread(() -> {
            try {
                while (!paused) {
                    var currentFileSize = 0L;

                    Thread.sleep(500);
                    for (int i = 0; i < chunks; i++)
                        currentFileSize += Files.size(filePaths.get(i));

                    updateProgress(currentFileSize, fileSize);
                    updateValue(currentFileSize);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.start();
        return t;
    }

    @Override
    public void pause() {
        try {
            paused = true;
            for (var channel : fileChannels)
                channel.close();
            var index = currentDownloading.indexOf(downloadModel);
            var download = currentDownloading.get(index);
            if (download != null) {
                download.setDownloadStatus(DownloadStatus.Paused);
                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadCompleteDate(download);
                currentDownloading.remove(index);
            }
            cancel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void succeeded() {
        try {
            var index = currentDownloading.indexOf(downloadModel);
            var download = currentDownloading.get(index);
            if (download != null) {
                var currentFileSize = 0L;
                for (int i = 0; i < chunks; i++)
                    currentFileSize += Files.size(filePaths.get(i));
                if (filePaths.stream().allMatch(path -> path.toFile().exists())
                        && currentFileSize == downloadModel.getSize() && !paused) {
                    if (!file.exists())
                        file.createNewFile();
                    for (int i = 0; i < chunks; i++) {
                        var name = file.getPath() + "#" + i;
                        try (
                                var in = new FileInputStream(name);
                                var out = new FileOutputStream(file.getPath(), file.exists());
                                var inputChannel = in.getChannel();
                                var outputChannel = out.getChannel();
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
                    download.setProgress(100);
                    for (var f : filePaths)
                        Files.deleteIfExists(f);
                }
                DownloadsRepo.updateDownloadProgress(download);
                DownloadsRepo.updateDownloadCompleteDate(download);
                currentDownloading.remove(index);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
