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

    public static HostServices hostServices;

    public static void setHostServices(HostServices hostServices) {
        AppConfigs.hostServices = hostServices;
    }
}
