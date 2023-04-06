package ir.darkdeveloper.bitkip.config;

import ir.darkdeveloper.bitkip.controllers.DownloadingController;
import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import javafx.application.HostServices;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppConfigs {

    public static final String VERSION = "0.1.0";

    public static String downloadPath = System.getProperty("user.home")
            + File.separator + "Downloads"
            + File.separator + "BitKip";

    public static final String dataPath = System.getProperty("user.home")
            + File.separator + "Documents"
            + File.separator + "BitKip";

    public static final String videosPath = downloadPath + File.separator + "Videos" + File.separator;
    public static final String programsPath = downloadPath + File.separator + "Programs" + File.separator;
    public static final String compressedPath = downloadPath + File.separator + "Compressed" + File.separator;
    public static final String musicPath = downloadPath + File.separator + "Music" + File.separator;
    public static final String documentPath = downloadPath + File.separator + "Documents" + File.separator;
    public static final String othersPath = downloadPath + File.separator + "Others" + File.separator;

    public static final double mainMinWidth = 883;
    public static final double mainPrefWidth = 1000;
    public static final double mainMinHeight = 515;
    public static final double mainPrefHeight = 600;
    public static final double newDownloadMinWidth = 600;
    public static final double newDownloadMinHeight = 550;

    public static final double downloadingMinWidth = 600;
    public static final double downloadingMinHeight = 350;

    public static int downloadRetryCount = 2;
    public static HostServices hostServices;

    public static final List<DownloadModel> currentDownloadings = new ArrayList<>();
    public static final List<QueueModel> startedQueues = new ArrayList<>();
    public static final List<DownloadingController> openDownloadings = new ArrayList<>();

    public static boolean showCompleteDialog = true;

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
