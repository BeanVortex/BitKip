package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
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
        if (bytes <= 0) return "0";
        final var units = new String[]{"B", "kB", "MB", "GB", "TB"};
        var digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static long getBytesFromString(String mb) {
        var mbVal = Double.parseDouble(mb);
        return (long) (mbVal * Math.pow(2, 20));
    }

    public static boolean mergeFiles(DownloadModel dm, int chunks, List<Path> filePaths) throws IOException {
        var currentFileSize = 0L;
        for (int i = 0; i < chunks; i++)
            currentFileSize += Files.size(filePaths.get(i));
        if (dm.getDownloaded() == 0)
            dm.setDownloaded(currentFileSize);
        if (filePaths.stream().allMatch(path -> path.toFile().exists())
                && currentFileSize == dm.getSize()) {
            var firstFile = new File(dm.getFilePath() + "#0");
            for (int i = 1; i < chunks; i++) {
                var name = dm.getFilePath() + "#" + i;
                try (
                        var in = new FileInputStream(name);
                        var out = new FileOutputStream(firstFile, firstFile.exists());
                        var inputChannel = in.getChannel();
                        var outputChannel = out.getChannel()
                ) {
                    var buffer = ByteBuffer.allocateDirect(1048576);
                    while (inputChannel.read(buffer) != -1) {
                        buffer.flip();
                        outputChannel.write(buffer);
                        buffer.clear();
                    }
                }
                Files.deleteIfExists(Path.of(name));
            }
            var firstFilePath = firstFile.getPath();
            return firstFile.renameTo(new File(firstFilePath.substring(0, firstFilePath.lastIndexOf('#'))));
        }
        return false;
    }

    public static void deleteDownload(DownloadModel download) {
        try {
            if (download.getChunks() == 0)
                Files.deleteIfExists(Path.of(download.getFilePath()));
            else {
                for (int i = 0; i < download.getChunks(); i++)
                    Files.deleteIfExists(Path.of(download.getFilePath() + "#" + i));
                Files.deleteIfExists(Path.of(download.getFilePath()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
