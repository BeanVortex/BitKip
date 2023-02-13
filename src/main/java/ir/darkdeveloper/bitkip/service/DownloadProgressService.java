package ir.darkdeveloper.bitkip.service;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.task.DownloadTask;
import ir.darkdeveloper.bitkip.utils.TableUtils;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

import java.util.List;

public class DownloadProgressService extends ScheduledService<Void> {

    private final DownloadModel downloadModel;
    private final List<DownloadModel> currentDownloading = AppConfigs.currentDownloading;

    public DownloadProgressService(DownloadModel downloadModel) {
        this.downloadModel = downloadModel;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                var download = currentDownloading.get(currentDownloading.indexOf(downloadModel));
                DownloadsRepo.updateDownloadProgress(download == null ? downloadModel : download);
                return null;
            }
        };
    }
}
