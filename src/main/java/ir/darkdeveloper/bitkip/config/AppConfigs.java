package ir.darkdeveloper.bitkip.config;

import ir.darkdeveloper.bitkip.task.DownloadTask;
import javafx.application.HostServices;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppConfigs {
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

    public static HostServices hostServices;
    public static final List<DownloadTask> downloadTaskList = new ArrayList<>();

    public static void setHostServices(HostServices hostServices) {
        AppConfigs.hostServices = hostServices;
    }
}
