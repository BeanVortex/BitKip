package io.beanvortex.bitkip.task;

import io.beanvortex.bitkip.utils.DownloadUtils;
import io.beanvortex.bitkip.utils.IOUtils;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import javafx.concurrent.Task;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static io.beanvortex.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;

public class FileMoveTask extends Task<Long> {

    private final String prevPath;
    private final String nextPath;
    private final long size;
    private final ExecutorService executor;
    private boolean isCompleted, isCanceled;

    public FileMoveTask(String prevPath, String nextPath, long size, ExecutorService executor) {
        this.prevPath = prevPath;
        this.nextPath = nextPath;
        this.size = size;
        this.executor = executor;
    }

    @Override
    protected Long call() {
        calculateSpeedAndProgress();
        IOUtils.moveAndDeletePreviousData(prevPath, nextPath);
        DownloadsRepo.getDownloadsByQueueName(ALL_DOWNLOADS_QUEUE).forEach(dm -> {
            if (dm.getFilePath().contains("BitKip")) {
                var downloadPath = DownloadUtils.determineLocation(dm.getName());
                var id = dm.getId();
                DownloadsRepo.updateDownloadLocation(downloadPath, id);
            }
        });
        isCompleted = true;
        executor.shutdownNow();
        if (size == 0)
            updateProgress(1.0d, 1.0d);
        return size;
    }

    private void calculateSpeedAndProgress() {
        if (size != 0)
            executor.submit(() -> {
                Thread.currentThread().setName("calculator: " + Thread.currentThread().getName());
                try {
                    while (!isCompleted || !isCanceled) {
                        var currentFileSize = IOUtils.getFolderSize(nextPath);
                        updateProgress(currentFileSize, size);
                        updateValue(currentFileSize);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }

    public void pause() {
        isCanceled = true;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public String getPrevPath() {
        return prevPath;
    }

    public String getNextPath() {
        return nextPath;
    }
}
