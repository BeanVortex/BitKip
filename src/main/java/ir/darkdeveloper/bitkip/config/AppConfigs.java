package ir.darkdeveloper.bitkip.config;

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

    public static final String VERSION = "0.2.4";

    public static final String dataPath = System.getProperty("user.home")
            + File.separator + "Documents"
            + File.separator + "BitKip" + File.separator;

    public static String downloadPath = System.getProperty("user.home")
            + File.separator + "Downloads"
            + File.separator + "BitKip" + File.separator;


    public static Logger log;


    public static void initLogger() {
        new File(dataPath).mkdir();
        new File(dataPath + "logs").mkdir();
        var dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        var timestamp = dateFormat.format(new Date());
        var pathToLogFile = dataPath + "logs" + File.separator + timestamp + ".log";
        System.setProperty("log.file.path", pathToLogFile);
        log = LoggerFactory.getLogger("LOG");
    }

    public static final String compressedPath = downloadPath + "Compressed" + File.separator;
    public static final String programsPath = downloadPath + "Programs" + File.separator;
    public static final String videosPath = downloadPath + "Videos" + File.separator;
    public static final String documentPath = downloadPath + "Documents" + File.separator;
    public static final String musicPath = downloadPath + "Music" + File.separator;
    public static final String othersPath = downloadPath + "Others" + File.separator;
    public static final List<String> defaultDownloadPaths = List.of(compressedPath, programsPath, videosPath,
            documentPath, musicPath, othersPath);
    public static final String queuesPath = downloadPath + "Queues" + File.separator;

    public static int downloadRetryCount = 10;
    public static HostServices hostServices;

    public static final List<DownloadModel> currentDownloadings = new ArrayList<>();
    public static final List<QueueModel> startedQueues = new ArrayList<>();
    public static final List<DownloadingController> openDownloadings = new ArrayList<>();
    // integer represents scheduleModelId
    public static final Map<Integer, ScheduleModel> currentSchedules = new HashMap<>();
    public static MainTableUtils mainTableUtils;
    public static QueueModel selectedQueue;
    public static boolean showCompleteDialog = true;

    public static String userPassword;


    public static void setHostServices(HostServices hostServices) {
        AppConfigs.hostServices = hostServices;
    }

    private static final QueueSubject queueSubject = new QueueSubject();
    private static final List<QueueModel> queues = new ArrayList<>();

    public static void addQueue(QueueModel queue) {
        queues.add(queue);
        queueSubject.notifyAllObservers();
    }

    public static void deleteQueue(String name) {
        getQueues().removeIf(qm -> qm.getName().equals(name));
        queueSubject.notifyAllObservers();
    }

    public static List<QueueModel> getQueues() {
        return queues;
    }

    public static void addAllQueues(List<QueueModel> queues) {
        AppConfigs.queues.clear();
        AppConfigs.queues.addAll(queues);
        queueSubject.notifyAllObservers();
    }

    public static QueueSubject getQueueSubject() {
        return queueSubject;
    }
}
