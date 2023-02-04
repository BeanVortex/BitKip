package ir.darkdeveloper.bitkip.utils;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import static ir.darkdeveloper.bitkip.config.AppConfigs.dataPath;
import static ir.darkdeveloper.bitkip.config.AppConfigs.downloadPath;


public class IOUtils {
    private static final Logger log = Logger.getLogger(IOUtils.class.getName());

    private static final String videosPath = downloadPath + File.separator + "Videos";
    private static final String programsPath = downloadPath + File.separator + "Programs";
    private static final String compressedPath = downloadPath + File.separator + "Compressed";
    private static final String musicPath = downloadPath + File.separator + "Music";
    private static final String documentPath = downloadPath + File.separator + "Documents";


    public static void createSaveLocations() {
        List.of(downloadPath, videosPath, programsPath, compressedPath, musicPath, documentPath, dataPath)
                .forEach(IOUtils::mkdir);
    }

    private static void mkdir(String dirPath) {
        var file = new File(dirPath);
        if (file.mkdir())
            log.info("created dir: " + dirPath);
        else
            log.info("not created dir: " + dirPath);
    }


}
