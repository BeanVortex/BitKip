package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.controllers.DetailsController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.utils.DownloadOpUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.DownloadUtils;
import javafx.application.Platform;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.utils.DownloadOpUtils.openFile;


public class DownloadLimitedTask extends DownloadTask {
    private boolean paused;
    private boolean isCalculating;
    private long limit;
    private final boolean isSpeedLimited;
    private File file;
    private ExecutorService executor;
    private FileChannel fileChannel;
    private int retries = 0;
    private int rateLimitCount = 0;
    private boolean blocking;
    private long fileSize;
    private String url;
    private InputStream in;


    /**
     * if not isSpeedLimited, then valueLimit
     **/
    public DownloadLimitedTask(DownloadModel downloadModel, long limit, boolean isSpeedLimited) {
        super(downloadModel);
        this.limit = limit;
        this.isSpeedLimited = isSpeedLimited;
    }


    @Override
    protected Long call() throws IOException {
        try {
            url = downloadModel.getUrl();
            file = new File(downloadModel.getFilePath());
            if (file.exists() && isCompleted(file, mainTableUtils))
                return 0L;
            var parentFolder = Path.of(file.getPath()).getParent().toFile();
            if (!parentFolder.exists())
                parentFolder.mkdir();
            fileSize = downloadModel.getSize();
            if (fileSize == -1 || fileSize == 0 || !downloadModel.isResumable())
                performDownloadInStream();
            else
                performDownload();
        } catch (IOException | InterruptedException e) {
            if (e instanceof IOException)
                log.error(e.getMessage());
            this.pause();
        }
        return IOUtils.getFileSize(file);
    }

    private void performDownloadInStream() throws IOException {
        InputStream i = null;
        ReadableByteChannel rbc = null;
        FileOutputStream fos = null;
        try {
            var con = DownloadUtils.connect(url, true);
            con.setRequestProperty("User-Agent", userAgent);
            i = con.getInputStream();
            rbc = Channels.newChannel(i);

            fos = new FileOutputStream(file, file.exists());
            fileChannel = fos.getChannel();

            updateProgress(0, 1);
            if (fileSize > 0)
                calculateSpeedAndProgress();

            var buffer = ByteBuffer.allocate(8192);
            while (rbc.read(buffer) != -1) {
                buffer.flip();
                fileChannel.write(buffer);
                if (fileSize < 0)
                    updateValue(fileChannel.position());
                buffer.clear();
            }

            var size = fileChannel.size();
            downloadModel.setSize(size);

            DownloadsRepo.updateDownloadProperty(DownloadsRepo.COL_SIZE, String.valueOf(size), downloadModel.getId());


        } catch (IOException e) {
            log.error(e.getMessage());
            Platform.runLater(() -> Notifications.create()
                    .title("Couldn't download file")
                    .text(e.getMessage())
                    .showError());
        } finally {
            if (fileChannel != null)
                fileChannel.close();
            if (fos != null)
                fos.close();
            if (rbc != null)
                rbc.close();
            if (i != null)
                i.close();
        }
    }


    private void performDownload() throws IOException, InterruptedException {

        try {
            var con = DownloadUtils.connect(url, false);
            if (!downloadModel.isResumable())
                con.setRequestProperty("User-Agent", userAgent);
            configureResume(con, file, downloadModel.getSize());
            in = con.getInputStream();

            var out = new FileOutputStream(file, file.exists());
            fileChannel = out.getChannel();

            if (isSpeedLimited)
                downloadSpeedLimited();
            else {
                calculateSpeedAndProgress();
                downloadSizeLimited();
            }
            out.close();
        } catch (SocketTimeoutException | UnknownHostException | SocketException s) {
            retries++;
            if (!paused && (continueOnLostConnectionLost || retries != downloadRetryCount)) {
                Thread.sleep(2000);
                performDownload();
            }
        } catch (ClosedChannelException ignore) {
        }
        var currFileSize = IOUtils.getFileSize(file);
        if (!paused && currFileSize != downloadModel.getSize()
                && (continueOnLostConnectionLost || downloadRateLimitCount < rateLimitCount)) {
            rateLimitCount++;
            performDownload();
        }

        if (blocking && paused) {
            succeeded();
            mainTableUtils.updateDownloadProgress(downloadModel.getProgress(), downloadModel);
        }
        paused = true;
    }

    private void downloadSpeedLimited() throws IOException, InterruptedException {
        var existingFileSize = 0L;
        if (file.exists())
            existingFileSize = IOUtils.getFileSize(file);

        var byteChannel = Channels.newChannel(in);
        log.info("Downloading speed limited: " + downloadModel);
        var lock = fileChannel.lock();
        do {
            var beforeDown = System.currentTimeMillis();
            fileChannel.transferFrom(byteChannel, existingFileSize, limit);
            var afterDown = System.currentTimeMillis();

            var timeToWait = ONE_SEC;
            var downloadTime = afterDown - beforeDown;
            if (timeToWait >= downloadTime)
                timeToWait = ONE_SEC - downloadTime;
            else
                timeToWait = 0;
            Thread.sleep(timeToWait);
            var currentFileSize = IOUtils.getFileSize(file);
            existingFileSize = currentFileSize;
            updateProgress(currentFileSize, fileSize);
            updateValue(existingFileSize);
        } while (existingFileSize < fileSize && !paused);
        lock.release();
    }

    @Override
    protected void setSpeedLimit(long limit) {
        if (isSpeedLimited)
            this.limit = limit;
    }

    private void downloadSizeLimited() throws IOException {
        var existingFileSize = 0L;
        if (file.exists())
            existingFileSize = IOUtils.getFileSize(file);
        log.info("Downloading size limited: " + downloadModel);
        var byteChannel = Channels.newChannel(in);
        fileChannel.transferFrom(byteChannel, existingFileSize, limit);
        fileChannel.close();
    }

    @Override
    protected void succeeded() {
        try {
            if (fileChannel != null)
                fileChannel.close();
            var dmOpt = currentDownloadings.stream()
                    .filter(c -> c.equals(downloadModel))
                    .findFirst();
            if (dmOpt.isPresent()) {
                var download = dmOpt.get();
                download.setDownloadStatus(DownloadStatus.Paused);
                downloadModel.setDownloadStatus(DownloadStatus.Paused);
                if (file.exists() && IOUtils.getFileSize(file) == downloadModel.getSize()) {
                    log.info("File successfully downloaded: " + download);
                    download.setCompleteDate(LocalDateTime.now());
                    download.setDownloadStatus(DownloadStatus.Completed);
                    downloadModel.setDownloadStatus(DownloadStatus.Completed);
                    download.setProgress(100);
                    download.setDownloaded(downloadModel.getSize());
                    updateProgress(1, 1);
                    DownloadsRepo.updateDownloadCompleteDate(download);
                    openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                            .findFirst().ifPresentOrElse(dc -> dc.onComplete(download),
                                    () -> {
                                        if (download.isShowCompleteDialog())
                                            DownloadOpUtils.openDetailsStage(download);
                                    });
                    if (download.isOpenAfterComplete())
                        openFile(downloadModel);

                } else openDownloadings.stream().filter(dc -> dc.getDownloadModel().equals(download))
                        .forEach(DetailsController::onPause);


                download.setDownloaded(IOUtils.getFileSize(file));
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
            whenDone();
        }

    }

    @Override
    protected void failed() {
        log.info("Failed download: " + downloadModel);
        paused = true;
        retries = 0;
        rateLimitCount = 0;
        succeeded();
    }

    private void calculateSpeedAndProgress() {
        if (isCalculating)
            return;
        executor.submit(() -> {
            Thread.currentThread().setName("calculator: " + Thread.currentThread().getName());
            try {
                isCalculating = true;
                while (!paused) {
                    Thread.sleep(ONE_SEC);
                    var currentFileSize = IOUtils.getFileSize(file);
                    updateProgress(currentFileSize, fileSize);
                    updateValue(currentFileSize);
                }
                isCalculating = false;

            } catch (InterruptedException ignore) {
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    private void configureResume(HttpURLConnection con, File file, long fileSize) throws IOException {
        if (file.exists()) {
            var existingFileSize = IOUtils.getFileSize(file);
            con.addRequestProperty("Range", "bytes=" + existingFileSize + "-" + (fileSize - 1));
        }
    }


    @Override
    public void pause() {
        paused = true;
        log.info("Paused download: " + downloadModel);
        retries = 0;
        rateLimitCount = 0;
        succeeded();
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
