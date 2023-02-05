package ir.darkdeveloper.bitkip.task;


import javafx.concurrent.Task;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class DownloadTask extends Task<Long> {

    private static final String uri = "https://dl2.soft98.ir/soft/n/Notepad.8.4.9.x64.zip?1675187285";
    //    private static final String uri = "https://dl2.soft98.ir/soft/m/Mozilla.Firefox.109.0.1.EN.x64.zip?1675245764";
    //        private static final String uri = "https://www.uplooder.net/f/tl/43/5f7fe4354236a7f7c52c9fbdfae24b33/a.txt";
//    private static final String fileName = "a.txt";
    private static final String fileName = "Mozilla.Firefox.109.0.1.EN.x64.zip";
    private static final int ONE_SEC = 1000;

    private final TextField field;
    private final TextField field2;
    private FileChannel fileChannel;
    private File file;
    private boolean paused = false;
    private boolean completed = false;
    private boolean isCalculating = false;

    public DownloadTask(TextField field, TextField field2) {
        this.field = field;
        this.field2 = field2;
    }

    private int chunks = 8;
    private List<FileChannel> fileChannels = new ArrayList<>();

    @Override
    protected Long call() throws Exception {

        var url = new URL(uri);
        var connection = (HttpURLConnection) url.openConnection();
        file = new File(fileName);
        // for resume
        if (resume(connection)) return null;

        var in = connection.getInputStream();
        long fileSize = saveFileSize(connection);

//        var out = new FileOutputStream(file, file.exists());
//        fileChannel = out.getChannel();
        var existingFileSize = 0L;
        if (file.exists()) {
            existingFileSize = Files.size(Path.of(file.getPath()));

            if (fileSize <= existingFileSize) {
                fileChannel.close();
                return existingFileSize;
            }
        }

        var byteChannel = Channels.newChannel(in);
//
//        var maxBuff = field.getText();
//        if (maxBuff != null && !maxBuff.isBlank()) {
//            downloadUntil(existingFileSize, Long.parseLong(maxBuff));
//        }
//
//        existingFileSize = downloadLimited(in, out, existingFileSize, byteChannel);
//
//

        downloadInChunks(url, fileSize);

        return existingFileSize;
    }

    private void downloadInChunks(URL url, long fileSize) throws IOException, InterruptedException {
        var bytesForEach = readConfig() / chunks;
        var threadList = new ArrayList<Thread>();
        var filePaths = new ArrayList<Path>();
        var to = bytesForEach;
        var from = 0L;
        var fromContinue = 0L;
        for (int i = 0; i < chunks; i++) {
            var name = fileName + "#" + i;
            filePaths.add(Paths.get(name));
            var file = new File(name);
            if (!file.exists()) {
                file.createNewFile();
                from = fromContinue + Files.size(Path.of(file.getPath()));
            } else {
                if (from == 0)
                    from = Files.size(Path.of(file.getPath()));
                else
                    from = fromContinue + Files.size(Path.of(file.getPath()));
            }
            var outp = new FileOutputStream(file, file.exists());
            var fileChan = outp.getChannel();
            fileChannels.add(fileChan);
            var con = (HttpURLConnection) url.openConnection();
            if (i + 1 == chunks && to != fileSize)
                to = fileSize;
            con.addRequestProperty("Range", "bytes=" + from + "-" + to);
            fromContinue = to + 1;
            to += bytesForEach;
            long existingFileSize = Files.size(Path.of(file.getPath()));
            calculateSpeedAndProgressChunks(fileChannels, filePaths);
            var t = new Thread(() -> {
                try {
                    downloadUntil(existingFileSize, fileChan, con.getInputStream(), Long.MAX_VALUE);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            threadList.add(t);
            t.start();
        }

        for (var thread : threadList) {
            thread.join();
        }

        if (paused) {
            paused = false;
            return;
        }
        if (!completed) {
            completed = true;
            if (!file.exists())
                file.createNewFile();
            for (int i = 0; i < chunks; i++) {
                var name = fileName + "#" + i;
                Files.write(Paths.get(fileName), Files.readAllBytes(Paths.get(name)), StandardOpenOption.APPEND);
                updateProgress(1,1);
            }
            for (var filePath : filePaths)
                Files.deleteIfExists(filePath);
        }
    }

    private long downloadLimited(InputStream in, FileOutputStream out, long existingFileSize, ReadableByteChannel byteChannel) throws IOException, InterruptedException {
        var limit = field2.getText();
        if (limit != null && !limit.isBlank()) {
            var fs = readConfig();
            var start = System.currentTimeMillis();
            long readBytes;
            do {
                readBytes = fileChannel.transferFrom(byteChannel, existingFileSize, Long.parseLong(limit));
                var currentFileSize = Files.size(Path.of(file.getPath()));
                existingFileSize = currentFileSize;
                updateProgress(currentFileSize, fs);
                updateValue(readBytes);
                Thread.sleep(ONE_SEC);
            } while (existingFileSize < fs && fileChannel.isOpen());
            in.close();
            out.close();
            System.out.println("Lasted: " + (System.currentTimeMillis() - start));
        }
        return existingFileSize;
    }

    private void downloadUntil(long existingFileSize, FileChannel fileChannel,
                               InputStream i, long count) throws IOException {
        var byteChannel = Channels.newChannel(i);
        var s = System.currentTimeMillis();
        fileChannel.transferFrom(byteChannel, existingFileSize, count);
        var e = System.currentTimeMillis() - s;
        System.out.println("Lasted: " + e);
        fileChannel.close();
    }

    private long saveFileSize(HttpURLConnection connection) {
        long fileSize = connection.getContentLengthLong();
        if (!file.exists())
            saveConfigs(fileSize);
        else
            fileSize = readConfig();
        return fileSize;
    }

    private boolean resume(HttpURLConnection connection) throws IOException {
        if (file.exists()) {
            var existingFileSize = Files.size(Path.of(file.getPath()));
            connection.addRequestProperty("Range", "bytes=" + existingFileSize + "-");
            var fs = readConfig();
            if (fs != 0 && existingFileSize == fs) {
                System.out.println("already downloaded");
                return true;
            }
        }
        return false;
    }

    private void calculateSpeedAndProgress(FileChannel channel, String filePath) {
        new Thread(() -> {
            try {
                var readFileSize = readConfig();
                while (channel.isOpen()) {
                    var beforeCurrentFileSize = Files.size(Path.of(filePath));
                    Thread.sleep(500);
                    var currentFileSize = Files.size(Path.of(filePath));
                    updateProgress(currentFileSize, readFileSize);
                    var speed = (currentFileSize - beforeCurrentFileSize) * 2;
                    updateValue(speed);
                }
                if (file.exists()) {
                    var currentFileSize = Files.size(Path.of(file.getPath()));
                    if (currentFileSize == readFileSize)
                        updateProgress(currentFileSize, readFileSize);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void calculateSpeedAndProgressChunks(List<FileChannel> channels, List<Path> filePaths) {
        if (filePaths.size() == channels.size() && channels.size() != chunks && !isCalculating)
            return;
        new Thread(() -> {
            try {
                var readFileSize = readConfig();
                while (!paused) {
                    isCalculating = true;
                    var currentFileSize = 0L;

                    Thread.sleep(500);
                    for (int i = 0; i < chunks; i++)
                        currentFileSize += Files.size(filePaths.get(i));

                    updateProgress(currentFileSize, readFileSize);
                    updateValue(currentFileSize);
                }
                isCalculating = false;
            } catch (IOException | InterruptedException ignore) {
                // it may reach here
//                e.printStackTrace();
            }
        }).start();
    }

    // 473638

    @Override
    protected void succeeded() {
        System.out.println("DownloadTask.succeeded");
        try {
            if (fileChannel != null && fileChannel.isOpen())
                fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void failed() {
        super.failed();
    }

    public void pause() {
        try {
            paused = true;
            if (fileChannel != null)
                fileChannel.close();
            for (var channel : fileChannels)
                channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void cancelled() {
        try {
            if (fileChannel != null)
                fileChannel.close();
            Files.deleteIfExists(Path.of(file.getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveConfigs(Long size) {
        try {
            var file = new File("download_detail.txt");
            if (!file.exists())
                file.createNewFile();

            var writer = new FileWriter(file);
            writer.append("size=").append(String.valueOf(size));
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static long readConfig() {
        var file = new File("download_detail.txt");
        if (file.exists()) {
            try (var reader = new BufferedReader(new FileReader(file))) {
                String cfg;
                while ((cfg = reader.readLine()) != null) {
                    var key = cfg.split("=")[0];
                    var value = cfg.split("=")[1];
                    if ("size".equals(key))
                        return Long.parseLong(value);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }
}

//    public void onStart() throws Exception {
//        downloadTask = new DownloadTask(field, field2);
//        downloadTask.valueProperty().addListener((observable, oldValue, newValue) -> {
//            if (oldValue == null)
//                oldValue = 0L;
//            var speed = (newValue - oldValue) * 2;
////            long fileSize = DownloadTask.readConfig();
//            // todo :remaining time
//            if (newValue == 0)
//                speed = 0;
//
//            speed /= 1000;
//            remainingLabel.setText("%d kB/s%n%n".formatted(speed));
//        });
//        progress.progressProperty().bind(downloadTask.progressProperty());
//        downloadTask.progressProperty().addListener((o, old, newV) -> {
//            progressLabel.setText("" + ((int) (newV.floatValue() * 100)));
//
//        });
//        var t = new Thread(downloadTask);
//        t.setDaemon(true);
//        t.start();
//    }
//
//    public void onPause() {
//        if (downloadTask != null)
//            downloadTask.pause();
//    }