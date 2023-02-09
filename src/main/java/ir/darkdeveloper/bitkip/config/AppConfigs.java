package ir.darkdeveloper.bitkip.config;

import javafx.application.HostServices;

import java.io.File;

public class AppConfigs {
    public static String downloadPath = System.getProperty("user.home")
            + File.separator + "Downloads"
            + File.separator + "BitKip";

    public static final String dataPath = System.getProperty("user.home")
            + File.separator + "Documents"
            + File.separator + "BitKip";

    public static final String videosPath = downloadPath + File.separator + "Videos";
    public static final String programsPath = downloadPath + File.separator + "Programs";
    public static final String compressedPath = downloadPath + File.separator + "Compressed";
    public static final String musicPath = downloadPath + File.separator + "Music";
    public static final String documentPath = downloadPath + File.separator + "Documents";
    public static final String othersPath = downloadPath + File.separator + "Others";

    public static HostServices hostServices;

    public static void setHostServices(HostServices hostServices) {
        AppConfigs.hostServices = hostServices;
    }
}
