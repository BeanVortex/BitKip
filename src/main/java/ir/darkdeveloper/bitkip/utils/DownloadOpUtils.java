package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;

import java.time.LocalDateTime;

public class DownloadOpUtils {

    public static void pauseDownload(DownloadModel dm) {
        var download = AppConfigs.currentDownloading.get(AppConfigs.currentDownloading.indexOf(dm));
        download.getDownloadTask().pause();
    }

    public static void resumeDownload(DownloadModel dm, Runnable tableRefresh, MainTableUtils mainTableUtils) {
        dm.setLastTryDate(LocalDateTime.now());
        dm.setDownloadStatus(DownloadStatus.Trying);
        DownloadsRepo.updateDownloadLastTryDate(dm);
        if (tableRefresh != null)
            tableRefresh.run();
        NewDownloadUtils.startDownload(dm, mainTableUtils, null, null, true);
    }

    public static void deleteDownloadRecord(DownloadModel dm) {
        var index = AppConfigs.currentDownloading.indexOf(dm);
        if (index != -1)
            dm = AppConfigs.currentDownloading.get(index);
        if (dm.getDownloadTask() != null && dm.getDownloadTask().isRunning())
            dm.getDownloadTask().pause();
        DownloadsRepo.deleteDownload(dm);
    }
}
