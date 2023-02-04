package ir.darkdeveloper.bitkip.config;

import java.io.File;

public class AppConfigs {
    public static String downloadPath = System.getProperty("user.home")
            + File.separator + "Downloads"
            + File.separator + "BitKip";

    public static final String dataPath = System.getProperty("user.home")
            + File.separator + "Documents"
            + File.separator + "BitKip";
}
