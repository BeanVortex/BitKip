package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import javafx.concurrent.Task;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static ir.darkdeveloper.bitkip.utils.IOUtils.getFolderSize;

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
            if (dm.getFilePath().contains("BitKip")){
                var downloadPath = NewDownloadUtils.determineLocation(dm.getName());
                var id = dm.getId();
                DownloadsRepo.updateDownloadLocation(downloadPath, id);
            }
        });
        isCompleted = true;
        executor.shutdownNow();
        return size;
    }

    private void calculateSpeedAndProgress() {
        executor.submit(() -> {
            Thread.currentThread().setName("calculator: " + Thread.currentThread().getName());
            try {
                while (!isCompleted || !isCanceled) {
                    var currentFileSize = getFolderSize(nextPath);
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
