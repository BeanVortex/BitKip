package io.beanvortex.bitkip.utils;

import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.controllers.BatchDownload;
import io.beanvortex.bitkip.models.*;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import io.beanvortex.bitkip.repo.QueuesRepo;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import org.controlsfx.control.Notifications;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static io.beanvortex.bitkip.config.AppConfigs.*;
import static io.beanvortex.bitkip.config.observers.QueueSubject.getQueues;
import static io.beanvortex.bitkip.repo.DownloadsRepo.COL_PATH;
import static io.beanvortex.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;
import static io.beanvortex.bitkip.utils.Defaults.staticQueueNames;


public class IOUtils {


    public static void createSaveLocations() {
        mkdir(downloadPath);
        mkdir(exportedLinksPath);
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

    public static double getMbOfBytes(long bytes) {
        double scale = Math.pow(10, 3);
        return Math.round(((double) bytes / (1024 * 1024)) * scale) / scale;
    }

    public static long getFreeSpace(Path path) {
        return path.toFile().getUsableSpace();
    }

    public static long getBytesFromString(String mb) {
        if (mb.isBlank())
            return 0;
        var mbVal = Double.parseDouble(mb);
        return (long) (mbVal * 1_048_576);
    }

    public static boolean mergeFiles(DownloadModel dm, int chunks, List<Path> filePaths) throws IOException {
        var currentFileSize = 0L;
        for (int i = 0; i < chunks; i++)
            currentFileSize += Files.size(filePaths.get(i));

        var neededFileSize = currentFileSize + Files.size(filePaths.getLast());
        checkAvailableSpace(filePaths.getFirst(), neededFileSize);

        if (dm.getDownloaded() == 0)
            dm.setDownloaded(currentFileSize);
        if (filePaths.stream().allMatch(path -> path.toFile().exists())
                && currentFileSize == dm.getSize()) {

            var details = openDownloadings.stream()
                    .filter(dc -> dc.getDownloadModel().equals(dm))
                    .findFirst();
            ProgressBar progressBar;
            Label speedLbl, downloadedLbl;
            if (details.isPresent()) {
                progressBar = details.get().getProgressBar();
                Platform.runLater(() -> {
                    details.get().setPauseButtonDisable(true);
                    details.get().updateLabels("Status: " + DownloadStatus.Merging.name(), "Remaining: Merging");
                });
                speedLbl = details.get().getSpeedLbl();
                downloadedLbl = details.get().getDownloadedOfLbl();
            } else {
                downloadedLbl = null;
                speedLbl = null;
                progressBar = null;
            }

            dm.setDownloadStatus(DownloadStatus.Merging);
            mainTableUtils.refreshTable();

            var firstFile = filePaths.getFirst().toFile();
            var bufferSize = currentFileSize > 524_288_000 ?
                    (
                            currentFileSize > 1_048_576_000 ? 2097152 : 1048576
                    )
                    : 8192;
            var buffer = ByteBuffer.allocateDirect(bufferSize);
            for (int i = 1; i < chunks; i++) {
                var nextFile = filePaths.get(i).toFile();
                try (
                        var in = new FileInputStream(nextFile);
                        var out = new FileOutputStream(firstFile, firstFile.exists());
                        var inputChannel = in.getChannel();
                        var outputChannel = out.getChannel()
                ) {
                    while (inputChannel.read(buffer) != -1) {
                        buffer.flip();
                        var bytes = outputChannel.write(buffer);
                        if (details.isPresent()) {
                            var finalCurrentFileSize = currentFileSize;
                            var position = outputChannel.position();
                            Platform.runLater(() -> {
                                progressBar.setProgress((double) position / finalCurrentFileSize);
                                speedLbl.setText(formatBytes(bytes));
                                var downloadOf = "%s / %s"
                                        .formatted(formatBytes(position), formatBytes(finalCurrentFileSize));
                                downloadedLbl.setText(downloadOf);
                            });
                        }
                        buffer.clear();
                    }
                }
                if (nextFile.exists())
                    nextFile.delete();
            }
            var pathToMove = filePaths.getFirst().getParent().getParent() + File.separator + dm.getName();
            return firstFile.renameTo(new File(pathToMove));
        }
        return false;
    }

    public static void deleteDownload(DownloadModel dm) {
        try {
            if (dm.getChunks() == 0)
                Files.deleteIfExists(Path.of(dm.getFilePath()));
            else {
                var parentPath = Path.of(dm.getFilePath()).getParent() + File.separator;
                var tempPath = Path.of(parentPath + ".temp");
                for (int i = 0; i < dm.getChunks(); i++) {
                    if (Files.exists(tempPath ))
                        Files.deleteIfExists(Path.of(tempPath + File.separator + dm.getName() + "#" + i));
                    else Files.deleteIfExists(Path.of(dm.getFilePath() + "#" + i));
                }
                Files.deleteIfExists(Path.of(dm.getFilePath()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            log.error(e.getLocalizedMessage());
        }
    }

    public static void moveDownloadFiles(DownloadModel dm, String newFilePath) {
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
        var downloadsByQueueName = DownloadsRepo.getDownloadsByQueueName(queueName, true);
        downloadsByQueueName.stream()
                .filter(dm -> dm.getFilePath().contains(downloadPath))
                .peek(dm -> {
                    var newFilePath = FileType.determineFileType(dm.getName()).getPath() + dm.getName();
                    moveDownloadFiles(dm, newFilePath);
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

    public static void createOrDeleteFolderForQueue(QueueModel queue) {
        if (staticQueueNames.contains(queue.getName()))
            return;
        if (queue.hasFolder()) {
            var res = createFolderInSaveLocation("Queues" + File.separator + queue.getName());
            if (res) {
                var downloadsByQueueName = DownloadsRepo.getDownloadsByQueueName(queue.getName(), true);
                var newFilePath = queuesPath + queue.getName() + File.separator;
                if (FxUtils.askToMoveFilesForQueues(downloadsByQueueName, queue, newFilePath))
                    downloadsByQueueName.forEach(dm -> moveDownloadFiles(dm, newFilePath + dm.getName()));
            }
        } else moveFilesAndDeleteQueueFolder(queue.getName());
    }

    public static void saveConfigs() {
        try {
            var file = new File(dataPath + "config.cfg");
            if (!file.exists())
                file.createNewFile();

            var writer = new FileWriter(file);
            writer.append("save_location=").append(downloadPath).append("\n")
                    .append("theme=").append(theme).append("\n")
                    .append("startup=").append(String.valueOf(startup)).append("\n")
                    .append("server_enabled=").append(String.valueOf(serverEnabled)).append("\n")
                    .append("port=").append(String.valueOf(serverPort)).append("\n")
                    .append("trigger_turn_off_on_empty_queue=").append(String.valueOf(triggerTurnOffOnEmptyQueue)).append("\n")
                    .append("show_complete_dialog=").append(String.valueOf(showCompleteDialog)).append("\n")
                    .append("show_error_notifications=").append(String.valueOf(showErrorNotifications)).append("\n")
                    .append("start_fast_queue=").append(String.valueOf(startFastQueue)).append("\n")
                    .append("trust_all_servers=").append(String.valueOf(trustAllServers)).append("\n")
                    .append("continue_on_connection_lost=").append(String.valueOf(continueOnLostConnectionLost)).append("\n")
                    .append("retry_count=").append(String.valueOf(downloadRetryCount)).append("\n")
                    .append("rate_limit_count=").append(String.valueOf(downloadRateLimitCount)).append("\n")
                    .append("connection_timeout=").append(String.valueOf(connectionTimeout)).append("\n")
                    .append("read_timeout=").append(String.valueOf(readTimeout)).append("\n")
                    .append("immediate_download=").append(String.valueOf(downloadImmediately)).append("\n")
                    .append("add_same_download=").append(String.valueOf(addSameDownload)).append("\n")
                    .append("less_cpu_intensive=").append(String.valueOf(lessCpuIntensive)).append("\n")
                    .append("last_saved_dir=").append(String.valueOf(lastSavedDir)).append("\n")
                    .append("user_agent_enabled=").append(String.valueOf(userAgentEnabled)).append("\n")
                    .append("user_agent=").append(userAgent);
            writer.flush();
            writer.close();

            log.info("Saved config");

        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    public static void readConfig() {
        try {
            var file = new File(dataPath + "config.cfg");
            if (file.exists()) {
                var reader = new BufferedReader(new FileReader(file));
                String cfg;
                while ((cfg = reader.readLine()) != null) {
                    var key = cfg.split("=")[0];
                    var value = cfg.split("=")[1];
                    switch (key) {
                        case "save_location" -> downloadPath = value;
                        case "theme" -> theme = value;
                        case "startup" -> startup = value.equals("true");
                        case "server_enabled" -> serverEnabled = value.equals("true");
                        case "port" -> serverPort = Integer.parseInt(value);
                        case "trigger_turn_off_on_empty_queue" -> triggerTurnOffOnEmptyQueue = value.equals("true");
                        case "show_complete_dialog" -> showCompleteDialog = value.equals("true");
                        case "show_error_notifications" -> showErrorNotifications = value.equals("true");
                        case "start_fast_queue" -> startFastQueue = value.equals("true");
                        case "trust_all_servers" -> trustAllServers = value.equals("true");
                        case "continue_on_connection_lost" -> continueOnLostConnectionLost = value.equals("true");
                        case "retry_count" -> downloadRetryCount = Integer.parseInt(value);
                        case "rate_limit_count" -> downloadRateLimitCount = Integer.parseInt(value);
                        case "connection_timeout" -> connectionTimeout = Integer.parseInt(value);
                        case "read_timeout" -> readTimeout = Integer.parseInt(value);
                        case "immediate_download" -> downloadImmediately = value.equals("true");
                        case "add_same_download" -> addSameDownload = value.equals("true");
                        case "less_cpu_intensive" -> lessCpuIntensive = value.equals("true");
                        case "last_saved_dir" -> lastSavedDir = Files.exists(Paths.get(value)) ? value : System.getProperty("user.home");
                        case "user_agent" -> userAgent = value;
                        case "user_agent_enabled" -> userAgentEnabled = value.equals("true");
                    }
                }
                log.info("Read config");
            }
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    public static void moveAndDeletePreviousData(String prevPath, String nextPath) {
        var nextDir = new File(nextPath);
        if (!nextDir.exists())
            nextDir.mkdir();
        var prevDir = new File(prevPath);
        var files = prevDir.listFiles();
        if (files != null)
            for (var file : files) {
                if (file.isFile())
                    file.renameTo(new File(nextPath + file.getName()));
                else if (file.isDirectory()) {
                    var innerPrevPath = prevPath + file.getName() + File.separator;
                    var innerNextPath = nextPath + file.getName() + File.separator;
                    moveAndDeletePreviousData(innerPrevPath, innerNextPath);
                }
            }
        prevDir.delete();
    }

    public static void deleteFolderWithContent(String dirPath) {
        var file = new File(dirPath);
        if (file.isFile()) {
            if (file.exists())
                file.delete();
            return;
        }
        var files = file.listFiles();
        if (files != null)
            for (var f : files)
                if (file.isDirectory())
                    deleteFolderWithContent(f.getPath());
        file.delete();
    }

    public static long getFileSize(File file) throws IOException {
        return getFileSize(Path.of(file.getPath()));
    }

    public static long getFileSize(Path path) throws IOException {
        return Files.size(path);
    }

    public static long getFolderSize(String dirPath) throws IOException {
        var file = new File(dirPath);
        var files = file.listFiles();
        long size = 0;
        if (files != null)
            for (var f : files)
                if (f.isFile()) size += Files.size(Path.of(f.getPath()));
                else size += getFolderSize(f.getPath());

        return size;
    }

    public static List<LinkModel> readLinksFromFile(ActionEvent e) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Select txt file containing links");
        fileChooser.setInitialDirectory(new File(AppConfigs.downloadPath));
        var selectedFile = fileChooser.showOpenDialog(FxUtils.getStageFromEvent(e));
        if (selectedFile != null)
            return IOUtils.convertFileToLinks(selectedFile);

        Notifications.create()
                .title("No File")
                .text("Location is wrong!")
                .showError();
        return null;
    }

    private static List<LinkModel> convertFileToLinks(File file) {
        try {
            var reader = new BufferedReader(new FileReader(file));
            var linesStream = reader.lines();
            if (linesStream == null)
                return null;
            var lines = linesStream.toList();
            if (lines.isEmpty())
                return null;

            var chunks = Validations.maxChunks(Long.MAX_VALUE);
            var allDownloadsQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, false);
            var firstUrl = lines.getFirst();
            var connection = DownloadUtils.connect(firstUrl, null);
            var firstFileName = DownloadUtils.extractFileName(firstUrl, connection);
            var secondaryQueue = BatchDownload.getSecondaryQueueByFileName(firstFileName);
            var path = DownloadUtils.determineLocation(firstFileName);

            return lines.stream().map(li -> {
                        if (!Validations.validateUri(li))
                            return null;
                        var lm = new LinkModel(li, chunks);
                        lm.getQueues().add(allDownloadsQueue);
                        lm.getQueues().add(secondaryQueue);
                        lm.setPath(path);
                        return lm;
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage());
        }
        return null;
    }


    public static void writeLinksToFile(List<String> urls, String queue) throws IOException {
        var dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        var timestamp = dateFormat.format(new Date());
        var filename = "exported_links_%s_%s.txt".formatted(timestamp, queue);
        var output = new File(exportedLinksPath + filename);
        output.createNewFile();
        var writer = new BufferedWriter(new FileWriter(output));
        for (var url : urls) {
            writer.write(url);
            writer.write('\n');
        }
        writer.flush();
        writer.close();
    }

    public static void checkAvailableSpace(String filePath, long fileSize) throws IOException {
        checkAvailableSpace(Path.of(filePath), fileSize);
    }

    public static void checkAvailableSpace(Path filePath, long fileSize) throws IOException {
        var freeSpace = getFreeSpace(filePath.getParent());
        // if after saving, the space left should be above 100MB
        if (freeSpace - fileSize <= Math.pow(2, 20) * 100) {
            var msg = "The location you chose, has not enough space to save the download file: " + filePath;
            Platform.runLater(() -> Notifications.create()
                    .title("No Free space")
                    .text(msg)
                    .showError());
            throw new IOException(msg);
        }

    }

    public static void writeUpdateDescription(UpdateModel.Description description) {
        try {
            var file = new File(dataPath + "patch_note.txt");
            if (!file.exists())
                file.createNewFile();

            var writer = new FileWriter(file);
            writer.append(description.header()).append("\n");
            for (var s : description.features())
                writer.append(s).append("\n");
            writer.flush();
            writer.close();
            log.info("Saved patch note");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static String readUpdateDescription() {
        try {
            var file = new File(dataPath + "patch_note.txt");
            var reader = new BufferedReader(new FileReader(file));
            String line;
            var str = new StringBuilder();
            if ((line = reader.readLine()) != null) {
                var pattern = Pattern.compile("\\d+(\\.\\d+)*");
                var matcher = pattern.matcher(line);

                String version = "";
                while (matcher.find())
                    version = matcher.group();

                if (!VERSION.equals(version))
                    return "No recent patch notes";
            }
            while ((line = reader.readLine()) != null)
                str.append(line).append("\n");
            reader.close();
            log.info("Read patch note");
            return str.toString();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return "No recent patch notes";
    }
}
