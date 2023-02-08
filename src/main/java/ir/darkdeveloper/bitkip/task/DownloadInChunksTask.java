package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.models.DownloadModel;
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
import java.util.ArrayList;
import java.util.List;

import static ir.darkdeveloper.bitkip.task.DownloadTask.*;

public class DownloadInChunksTask extends Task<Long> implements DownloadTask {
    private final int chunks;
    private final List<FileChannel> fileChannels = new ArrayList<>();
    private final DownloadModel downloadModel;

    private boolean paused;
    private boolean completed;
    private boolean isCalculating;

    public DownloadInChunksTask(DownloadModel downloadModel) {
        if (downloadModel.getChunks() == 0)
            throw new IllegalArgumentException("To download file in chunks, chunks must not be 0");
        this.chunks = downloadModel.getChunks();
        this.downloadModel = downloadModel;
    }


    @Override
    protected Long call() throws Exception {
        var url = new URL(downloadModel.getUrl());
        var connection = (HttpURLConnection) url.openConnection();
        var file = new File(downloadModel.getFilePath());
        var fileSize = saveOrGetFileSize(connection, file, downloadModel);
        downloadInChunks(file, url, fileSize);
        return 0L;
    }


    private void downloadInChunks(File file, URL url, long fileSize) throws IOException, InterruptedException {

        var bytesForEach = fileSize / chunks;
        var threadList = new ArrayList<Thread>();
        var filePaths = new ArrayList<Path>();
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
            countingThread = calculateSpeedAndProgressChunks(fileChannels, filePaths, fileSize);
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

        if (paused) {
            paused = false;
            return;
        }


        if (!completed) {
            completed = true;
            if (!file.exists())
                file.createNewFile();
            for (int i = 0; i < chunks; i++) {
                var name = filePath + "#" + i;
                try (
                        var in = new FileInputStream(name);
                        var out = new FileOutputStream(filePath, file.exists());
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
            for (var f : filePaths)
                Files.deleteIfExists(f);
        }
    }

    private Thread calculateSpeedAndProgressChunks(List<FileChannel> channels, List<Path> filePaths, long fileSize) {
        if (filePaths.size() == channels.size() && channels.size() != chunks && !isCalculating)
            return null;
        var t = new Thread(() -> {
            try {
                while (!paused) {
                    isCalculating = true;
                    var currentFileSize = 0L;

                    Thread.sleep(500);
                    for (int i = 0; i < chunks; i++)
                        currentFileSize += Files.size(filePaths.get(i));

                    updateProgress(currentFileSize, fileSize);
                    updateValue(currentFileSize);
                }
                isCalculating = false;
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
            cancelled();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
