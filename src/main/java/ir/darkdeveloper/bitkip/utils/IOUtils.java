package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.FileType;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.*;
import static ir.darkdeveloper.bitkip.repo.DownloadsRepo.COL_PATH;


public class IOUtils {


    public static void createSaveLocations() {
        mkdir(downloadPath);
        Arrays.stream(FileType.values()).forEach(fileType -> {
            mkdir(fileType.getPath());
            if (fileType != FileType.QUEUES)
                mkdir(fileType.getPath() + ".temp");
        });
    }

    public static void createFoldersForQueue() {
        getQueues().stream().filter(QueueModel::hasFolder)
                .forEach(qm -> {
                    var name = "Queues" + File.separator + qm.getName();
                    createFolderInSaveLocation(name);
                    createFolderInSaveLocation(name + File.separator + ".temp");
                });
    }

    public static void mkdir(String dirPath) {
        var file = new File(dirPath);
        if (file.mkdir())
            log.info("created dir: " + dirPath);
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
            var firstFile = filePaths.get(0).toFile();
            for (int i = 1; i < chunks; i++) {
                var nextFile = filePaths.get(i).toFile();
                try (
                        var in = new FileInputStream(nextFile);
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
                if (nextFile.exists())
                    nextFile.delete();
            }
            var pathToMove = filePaths.get(0).getParent() + File.separator + dm.getName();
            if (filePaths.get(0).toString().contains("BitKip"))
                pathToMove = filePaths.get(0).getParent().getParent() + File.separator + dm.getName();
            return firstFile.renameTo(new File(pathToMove));
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
            log.severe(e.getLocalizedMessage());
        }
    }

    public static boolean createFolderInSaveLocation(String name) {
        var dir = new File(downloadPath + name);
        if (!dir.exists()) {
            dir.mkdir();
            return true;
        }
        return false;
    }

    public static void moveFile(String oldFilePath, String newFilePath) {
        try {
            var file = new File(oldFilePath);
            if (file.exists())
                Files.move(Paths.get(oldFilePath), Paths.get(newFilePath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.severe(e.getLocalizedMessage());
        }
    }

    public static void moveDownloadFilesFiles(DownloadModel dm, String newFilePath) {
        if (dm.getProgress() != 100) {
            if (dm.getChunks() != 0) {
                var oldTempPath = Paths.get(dm.getFilePath()).getParent() + File.separator + ".temp" + File.separator + dm.getName();
                var newTempPath = Paths.get(newFilePath).getParent() + File.separator + ".temp" + File.separator;
                if (!Files.exists(Path.of(newTempPath)))
                    new File(newTempPath).mkdir();
                newTempPath += dm.getName();
                for (int i = 0; i < dm.getChunks(); i++)
                    moveFile(oldTempPath + "#" + i, newTempPath + "#" + i);
            } else
                moveFile(dm.getFilePath(), newFilePath);
        } else
            moveFile(dm.getFilePath(), newFilePath);
        DownloadsRepo.updateDownloadProperty(COL_PATH, "\"" + newFilePath + "\"", dm.getId());
    }

    public static void moveFilesAndDeleteQueueFolder(String queueName) {
        var downloadsByQueueName = DownloadsRepo.getDownloadsByQueueName(queueName);
        downloadsByQueueName.forEach(dm -> {
            var newFilePath = FileType.determineFileType(dm.getName()).getPath() + dm.getName();
            moveDownloadFilesFiles(dm, newFilePath);
        });
        removeFolder("Queues" + File.separator + queueName + File.separator + ".temp");
        removeFolder("Queues" + File.separator + queueName);
    }

    public static void removeFolder(String name) {
        var dir = new File(downloadPath + name);
        if (dir.exists() && dir.isDirectory())
            dir.delete();
    }

    public static void moveChunkFilesToTemp(String path) {
        var f = new File(path);
        if (f.isDirectory()) {
            var listFiles = f.listFiles();
            if (listFiles != null) {
                for (var file : listFiles) {
                    var oldPath = file.getPath();
                    if (oldPath.contains(".temp"))
                        continue;
                    if (oldPath.contains("#")) {
                        var fileName = oldPath.substring(oldPath.lastIndexOf(File.separator) + 1);
                        var newPath = Paths.get(oldPath).getParent() + File.separator + ".temp" + File.separator + fileName;
                        moveFile(oldPath, newPath);
                    } else moveChunkFilesToTemp(file.getPath());
                }
            }
        }
    }

    public static void createOrDeleteFolderForQueue(boolean create, QueueModel queue) {
        if (create) {
            var res = createFolderInSaveLocation("Queues" + File.separator + queue.getName());
            if (res) {
                var downloadsByQueueName = DownloadsRepo.getDownloadsByQueueName(queue.getName());
                if (FxUtils.askToMoveFiles(downloadsByQueueName, queue)) {
                    downloadsByQueueName.forEach(dm -> {
                        var newFilePath = queuesPath + queue.getName() + File.separator + dm.getName();
                        moveDownloadFilesFiles(dm, newFilePath);
                    });
                }
            }
        } else moveFilesAndDeleteQueueFolder(queue.getName());
    }

}
