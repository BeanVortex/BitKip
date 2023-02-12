package ir.darkdeveloper.bitkip.service;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.utils.TableUtils;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

public class DownloadProgressService extends ScheduledService<Void> {

    private final DownloadModel downloadModel;
    private final TableUtils tableUtils;

    public DownloadProgressService(DownloadModel downloadModel, TableUtils tableUtils) {
        this.downloadModel = downloadModel;
        this.tableUtils = tableUtils;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                var download = tableUtils.findDownload(downloadModel.getId());
                DownloadsRepo.updateDownloadProgress(download == null ? downloadModel : download);
                return null;
            }
        };
    }
}
