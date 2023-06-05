package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static ir.darkdeveloper.bitkip.config.AppConfigs.downloadPath;
import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static org.junit.jupiter.api.Assertions.*;

class DownloadsRepoTest {

    @Test
    void updateDownloadLocation() {
        var name = "name";
        AppConfigs.log = LoggerFactory.getLogger("BitKip");
        var dm = DownloadModel.builder()
                .name(name).progress(0).downloaded(0).size(1254631)
                .url(UUID.randomUUID().toString()).filePath(downloadPath + name)
                .chunks(8).addDate(LocalDateTime.now()).addToQueueDate(LocalDateTime.now())
                .lastTryDate(LocalDateTime.now()).completeDate(LocalDateTime.now())
                .queues(new CopyOnWriteArrayList<>(List.of(QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, false))))
                .openAfterComplete(false).showCompleteDialog(false).downloadStatus(DownloadStatus.Paused)
                .resumable(true)
                .build();
        DownloadsRepo.insertDownload(dm);
        var dmId = dm.getId();
        var newPath = System.getProperty("user.home")
                + File.separator + "Desktop"
                + File.separator + "BitKip-test-folder-new" + File.separator;
        DownloadsRepo.updateDownloadLocation(newPath, dmId);
        var byId = DownloadsRepo.findById(dmId);

        assert byId.size() == 1;
        var fetchedDm = byId.get(0);

        assertEquals(newPath + name, fetchedDm.getFilePath());

        DownloadsRepo.deleteDownload(dm);
    }
}