package ir.darkdeveloper.bitkip.utils;

import ir.darkdeveloper.bitkip.models.DownloadModel;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ir.darkdeveloper.bitkip.config.AppConfigs.downloadPath;


public class IOUtils {
    private static final Logger log = Logger.getLogger(IOUtils.class.getName());

    private static final String videosPath = downloadPath + File.separator + "Videos";
    private static final String programsPath = downloadPath + File.separator + "Programs";
    private static final String compressedPath = downloadPath + File.separator + "Compressed";
    private static final String musicPath = downloadPath + File.separator + "Music";
    private static final String documentPath = downloadPath + File.separator + "Documents";

    private static final String dataPath = System.getProperty("user.home")
            + File.separator + "Documents"
            + File.separator + "BitKip";

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

    public static void writeNewDownloadData(DownloadModel download) {
        try {
            var file = new File(dataPath, "downloads.csv");
            FileWriter writer = null;
            if (!file.exists()) {
                file.createNewFile();
                writer = new FileWriter(file);
                var columns = convertToCSV(new String[]{
                        "id", "name", "progress", "size", "url",
                        "filePath", "remainingTime", "chunks",
                        "queue", "addDate", "lastTryDate", "completeDate"});
                writer.append(columns);
            }
            if (writer == null)
                writer = new FileWriter(file, true);
            var newRow = convertToCSV(download.getDataInArr());
            writer.append("\n").append(newRow);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<DownloadModel> readDownloadData() {
        var downloadData = new ArrayList<DownloadModel>();
        try {
            var file = new File(dataPath, "downloads.csv");
            if (file.exists()) {
                var reader = new BufferedReader(new FileReader(file));
                String downloadLine;
                var skipOne = 0;
                while ((downloadLine = reader.readLine()) != null) {
                    if (skipOne == 0) {
                        skipOne++;
                        continue;
                    }
                    var downloadArr = downloadLine.split(",");
                    var dow = new DownloadModel();
                    dow.mapDataFromArr(downloadArr);
                    dow.fillProperties();
                    downloadData.add(dow);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return downloadData;
    }


    public static void writeNewQueue(String name) {
        try {
            var file = new File(dataPath, "queue.csv");
            FileWriter writer = null;
            if (!file.exists()) {
                file.createNewFile();
                writer = new FileWriter(file);
                var columns = convertToCSV(new String[]{"queue"});
                writer.append(columns);
            }
            if (writer == null)
                writer = new FileWriter(file, true);
            var newRow = convertToCSV(new String[]{name});
            writer.append("\n").append(newRow);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readQueues() {
        var queues = new ArrayList<String>();
        try {
            var file = new File(dataPath, "queue.csv");
            if (file.exists()) {
                var reader = new BufferedReader(new FileReader(file));
                String queueName;
                var skipOne = 0;
                while ((queueName = reader.readLine()) != null) {
                    if (skipOne == 0) {
                        skipOne++;
                        continue;
                    }
                    queues.add(queueName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return queues;
    }


    private static String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(IOUtils::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    private static String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }


}
