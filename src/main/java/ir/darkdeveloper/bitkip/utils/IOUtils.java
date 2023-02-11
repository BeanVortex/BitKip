package ir.darkdeveloper.bitkip.utils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Logger;

import static ir.darkdeveloper.bitkip.config.AppConfigs.dataPath;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;


public class IOUtils {
    private static final Logger log = Logger.getLogger(IOUtils.class.getName());


    public static void createSaveLocations() {
        List.of(downloadPath, videosPath, programsPath, compressedPath, musicPath, othersPath, documentPath, dataPath)
                .forEach(IOUtils::mkdir);
    }

    private static void mkdir(String dirPath) {
        var file = new File(dirPath);
        if (file.mkdir())
            log.info("created dir: " + dirPath);
        else
            log.info("not created dir: " + dirPath);
    }


    public static String formatBytes(long bytes) {
        if(bytes <= 0) return "0";
        final var units = new String[] { "B", "kB", "MB", "GB", "TB" };
        var digitGroups = (int) (Math.log10(bytes)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
