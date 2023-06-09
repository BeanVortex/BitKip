package ir.darkdeveloper.bitkip.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.darkdeveloper.bitkip.controllers.DownloadingController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.utils.MainTableUtils;
import javafx.application.HostServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;


public class AppConfigs {

    public static final String VERSION = "0.3.0";

    public static final String dataPath = System.getProperty("user.home")
            + File.separator + "Documents"
            + File.separator + "BitKip" + File.separator;

    public static final String exportedLinksPath = dataPath + "exported_links" + File.separator;

    public static String downloadPath = System.getProperty("user.home")
            + File.separator + "Downloads"
            + File.separator + "BitKip" + File.separator;

    public static String theme = "light";
    public static boolean serverEnabled = true;
    public static int serverPort = 9563;
    public static boolean showCompleteDialog = true;
    public static int downloadRetryCount = 10;
    public static int downloadRateLimitCount = 20;


    public static final ObjectMapper mapper = new ObjectMapper();

    public static Logger log;

    public static void initLogger() {
        new File(dataPath).mkdir();
        new File(dataPath + "logs").mkdir();
        var dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        var timestamp = dateFormat.format(new Date());
        var pathToLogFile = dataPath + "logs" + File.separator + timestamp + ".log";
        System.setProperty("log.file.path", pathToLogFile);
        log = LoggerFactory.getLogger("BitKip");
    }

    public static String compressedPath;
    public static String programsPath;
    public static String videosPath;
    public static String documentPath;
    public static String musicPath;
    public static String othersPath;
    public static List<String> defaultDownloadPaths;
    public static String queuesPath;

    public static void initPaths() {
        compressedPath = downloadPath + "Compressed" + File.separator;
        programsPath = downloadPath + "Programs" + File.separator;
        videosPath = downloadPath + "Videos" + File.separator;
        documentPath = downloadPath + "Documents" + File.separator;
        musicPath = downloadPath + "Music" + File.separator;
        othersPath = downloadPath + "Others" + File.separator;
        defaultDownloadPaths = List.of(compressedPath, programsPath, videosPath, documentPath, musicPath, othersPath);
        queuesPath = downloadPath + "Queues" + File.separator;
    }


    public static final List<DownloadModel> currentDownloadings = new ArrayList<>();
    public static final List<QueueModel> startedQueues = new ArrayList<>();
    public static final List<DownloadingController> openDownloadings = new ArrayList<>();
    // integer represents scheduleModelId
    public static final Map<Integer, ScheduleModel> currentSchedules = new HashMap<>();


    public static MainTableUtils mainTableUtils;
    public static QueueModel selectedQueue;
    public static String userPassword;
    public static HostServices hostServices;

}
