package io.beanvortex.bitkip.config;

import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.models.QueueModel;
import io.beanvortex.bitkip.models.ScheduleModel;
import io.beanvortex.bitkip.models.StartedQueue;
import io.beanvortex.bitkip.utils.MainTableUtils;
import io.beanvortex.bitkip.controllers.DetailsController;
import javafx.application.HostServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;


public class AppConfigs {

    public static final String VERSION = "1.5.2";

    public static final String dataPath = System.getProperty("user.home")
            + File.separator + "Documents"
            + File.separator + "BitKip" + File.separator;

    public static final String exportedLinksPath = dataPath + "exported_links" + File.separator;

    public static String downloadPath = System.getProperty("user.home")
            + File.separator + "Downloads"
            + File.separator + "BitKip" + File.separator;

    public static final String defaultTheme = "light";
    public static String theme = defaultTheme;
    public static final boolean defaultStartup = true;
    public static boolean startup = defaultStartup;
    public static final boolean defaultServerEnabled = true;
    public static boolean serverEnabled = defaultServerEnabled;
    public static final int defaultServerPort = 9563;
    public static int serverPort = defaultServerPort;
    public static final boolean defaultShowCompleteDialog = true;
    public static boolean showCompleteDialog = defaultShowCompleteDialog;
    public static final boolean defaultContinueOnLostConnectionLost = true;
    public static boolean continueOnLostConnectionLost = defaultContinueOnLostConnectionLost;
    public static final int defaultDownloadRetryCount = 10;
    public static int downloadRetryCount = defaultDownloadRetryCount;
    public static final int defaultDownloadRateLimitCount = 20;
    public static int downloadRateLimitCount = defaultDownloadRateLimitCount;
    public static final String defaultUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36";
    public static String userAgent = defaultUserAgent;
    public static final boolean defaultUserAgentEnabled = false;
    public static boolean userAgentEnabled = defaultUserAgentEnabled;
    public static final int defaultConnectionTimeout = 3000;
    public static int connectionTimeout = defaultConnectionTimeout;
    public static final int defaultReadTimeout = 3000;
    public static int readTimeout = defaultReadTimeout;
    public static final boolean defaultTriggerTurnOffOnEmptyQueue = true;
    public static boolean triggerTurnOffOnEmptyQueue = defaultTriggerTurnOffOnEmptyQueue;
    public static final boolean defaultDownloadImmediately = false;
    public static boolean downloadImmediately = defaultDownloadImmediately;
    public static final boolean defaultAddSameDownload = true;
    public static boolean addSameDownload  = defaultAddSameDownload;
    public static final boolean defaultLessCpuIntensive = false;
    public static String lastSavedDir = null;
    public static boolean lessCpuIntensive = defaultLessCpuIntensive;
    public static final boolean defaultShowErrorNotifications = true;
    public static boolean showErrorNotifications = defaultShowErrorNotifications;



    public static Logger log;
    public static String pathToLogFile;

    public static void initLogger() {
        new File(dataPath).mkdir();
        new File(dataPath + "logs").mkdir();
        var dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        var timestamp = dateFormat.format(new Date());
        pathToLogFile = dataPath + "logs" + File.separator + timestamp + ".log";
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
    public static final List<StartedQueue> startedQueues = new ArrayList<>();
    public static final List<DetailsController> openDownloadings = new ArrayList<>();
    // integer represents scheduleModelId
    public static final Map<Integer, ScheduleModel> currentSchedules = new HashMap<>();


    public static MainTableUtils mainTableUtils;
    public static QueueModel selectedQueue;
    public static String userPassword;
    public static HostServices hostServices;

}
