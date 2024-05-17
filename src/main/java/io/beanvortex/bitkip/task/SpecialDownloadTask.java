package io.beanvortex.bitkip.task;

import io.beanvortex.bitkip.utils.DownloadOpUtils;
import io.beanvortex.bitkip.utils.DownloadUtils;
import io.beanvortex.bitkip.utils.IOUtils;
import io.beanvortex.bitkip.controllers.DetailsController;
import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.models.DownloadStatus;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import javafx.application.Platform;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

import static io.beanvortex.bitkip.config.AppConfigs.*;


public class SpecialDownloadTask extends DownloadTask {
    private boolean paused;
    private File file;
    private ExecutorService executor;
    private FileChannel fileChannel;
    private boolean blocking;
    private long fileSize;
    private String url;
    private boolean isCalculating;
    private long lastModified;


    /**
     * if not isSpeedLimited, then valueLimit
     **/
    public SpecialDownloadTask(DownloadModel downloadModel) {
        super(downloadModel);
    }


    @Override
    protected Long call() throws IOException {
        try {
            url = downloadModel.getUri();
            file = new File(downloadModel.getFilePath());
            if (file.exists() && isCompleted(file, mainTableUtils))
                return 0L;
            var parentFolder = Path.of(file.getPath()).getParent().toFile();
            if (!parentFolder.exists())
                parentFolder.mkdir();
            fileSize = downloadModel.getSize();
            performDownloadInStream();
        } catch (IOException e) {
            log.error(e.getMessage());
            this.pause();
        }
        return IOUtils.getFileSize(file);
    }

    private void performDownloadInStream() throws IOException {
        InputStream i = null;
        ReadableByteChannel rbc = null;
        FileOutputStream fos = null;
        var notResumableOnly = fileSize > 0;
        try {
            var con = DownloadUtils.connect(url);
            var con2 = DownloadUtils.connect(url);
            lastModified = con2.getLastModified();
            con.setRequestProperty("User-Agent", userAgent);
            i = con.getInputStream();
            rbc = Channels.newChannel(i);

            fos = new FileOutputStream(file, file.exists());
            fileChannel = fos.getChannel();
            if (!notResumableOnly)
                updateProgress(0, 1);
            else
                calculateSpeedAndProgress();

            var buffer = ByteBuffer.allocate(8192);
            while (rbc.read(buffer) != -1) {
                buffer.flip();
                fileChannel.write(buffer);
                if (!notResumableOnly)
                    updateValue(fileChannel.position());
                buffer.clear();
            }

            var size = fileChannel.size();
            downloadModel.setSize(size);

            if (!notResumableOnly)
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
                        DownloadOpUtils.openFile(downloadModel);
                    var fileTime = FileTime.fromMillis(lastModified);
                    Files.setLastModifiedTime(Path.of(download.getFilePath()), fileTime);
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
        succeeded();
    }

    @Override
    public void pause() {
        paused = true;
        log.info("Paused download: " + downloadModel);
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
