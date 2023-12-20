package io.beanvortex.bitkip.utils;

import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import io.beanvortex.bitkip.repo.QueuesRepo;
import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.models.DownloadStatus;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.beanvortex.bitkip.config.AppConfigs.downloadPath;
import static org.junit.jupiter.api.Assertions.*;

class DownloadUtilsTest {


    @Test
    void getNewFileNameIfExists_ONE_EXISTS() {
        var name = "name.cc";
        var dm = createDownload(name);
        var newFileNameIfExists = getNewFileNameIfExists(name, dm);
        assertEquals("name(1).cc", newFileNameIfExists);
        deleteDownload(dm);
    }

    @Test
    void getNewFileNameIfExists_MULTIPLE_EXISTS_WITH_NUMBERED() {
        var name = "BitKip-main.part1.zip";
        var dm = createDownload(name);
        var newFileNameIfExists = getNewFileNameIfExists(name, dm);
        assertEquals("BitKip-main.part1(1).zip", newFileNameIfExists);

        var dm2 = createDownload(newFileNameIfExists);
        newFileNameIfExists = getNewFileNameIfExists(name, dm);
        assertEquals("BitKip-main.part1(2).zip", newFileNameIfExists);

        var dm3 = createDownload(newFileNameIfExists);
        newFileNameIfExists = getNewFileNameIfExists(name, dm);
        assertEquals("BitKip-main.part1(3).zip", newFileNameIfExists);


        deleteDownload(dm);
        deleteDownload(dm2);
        deleteDownload(dm3);
    }

    @Test
    void getNewFileNameIfExists_MULTIPLE_EXISTS_WITH_NUMBERED_NO_DOTS() {
        var name = "BitKip-main";
        var dm = createDownload(name);
        var newFileNameIfExists = getNewFileNameIfExists(name, dm);
        assertEquals("BitKip-main(1)", newFileNameIfExists);

        var dm2 = createDownload(newFileNameIfExists);
        newFileNameIfExists = getNewFileNameIfExists(name, dm);
        assertEquals("BitKip-main(2)", newFileNameIfExists);

        var dm3 = createDownload(newFileNameIfExists);
        newFileNameIfExists = getNewFileNameIfExists(name, dm);
        assertEquals("BitKip-main(3)", newFileNameIfExists);


        deleteDownload(dm);
        deleteDownload(dm2);
        deleteDownload(dm3);
    }



    private static String getNewFileNameIfExists(String name, DownloadModel dm) {
        var path = Path.of(dm.getFilePath()).getParent().toString();
        if (!path.endsWith(File.separator))
            path += File.separator;
        return DownloadUtils.getNewFileNameIfExists(name, path);
    }

    DownloadModel createDownload(String name) {
        AppConfigs.log = LoggerFactory.getLogger("BitKip");
        var dm = DownloadModel.builder()
                .name(name).progress(0).downloaded(0).size(1254631)
                .uri(UUID.randomUUID().toString()).filePath(downloadPath + name)
                .chunks(8).addDate(LocalDateTime.now()).addToQueueDate(LocalDateTime.now())
                .lastTryDate(LocalDateTime.now()).completeDate(LocalDateTime.now())
                .queues(new CopyOnWriteArrayList<>(List.of(QueuesRepo.findByName(Defaults.ALL_DOWNLOADS_QUEUE, false))))
                .openAfterComplete(false).showCompleteDialog(false).downloadStatus(DownloadStatus.Paused)
                .resumable(true)
                .build();
        DownloadsRepo.insertDownload(dm);
        return dm;
    }

    void deleteDownload(DownloadModel dm) {
        DownloadsRepo.deleteDownload(dm);
    }
}