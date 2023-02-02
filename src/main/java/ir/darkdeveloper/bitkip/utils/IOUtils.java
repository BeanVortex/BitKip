package ir.darkdeveloper.bitkip.utils;

import java.io.File;
import java.util.logging.Logger;


public class IOUtils {

    private static final Logger log = Logger.getLogger(IOUtils.class.getName());




    public static void createSaveLocation() {

    }

    private static void mkdir(String dirPath) {
        var file = new File(dirPath);
        if (file.mkdir())
            log.info("created dir: " + dirPath);
        else
            log.info("not created dir: " + dirPath);
    }

//    public static void saveConfigs() {
//        try {
//            var file = new File(getConfigLocation() + "config.cfg");
//            if (!file.exists())
//                file.createNewFile();
//
//            var writer = new FileWriter(file);
//            writer.append("save_location=").append(getSaveLocation())
//                    .append("\n")
//                    .append("theme=").append(getTheme())
//                    .append("\n")
//                    .append("background_download=").append(String.valueOf(isBackgroundDownload()))
//                    .append("\n")
//                    .append("result_count=").append(getResultCount())
//                    .append("\n")
//                    .append("filter_result=").append(getFilterResult());
//            writer.flush();
//            writer.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public static void readConfig() {
//        try {
//            var file = new File(getConfigLocation() + "config.cfg");
//            if (file.exists()) {
//                var reader = new BufferedReader(new FileReader(file));
//                String cfg;
//                while ((cfg = reader.readLine()) != null) {
//                    var key = cfg.split("=")[0];
//                    var value = cfg.split("=")[1];
//                    switch (key) {
//                        case "save_location" -> setSaveLocation(value);
//                        case "theme" -> setTheme(value);
//                        case "background_download" -> setBackgroundDownload(Boolean.parseBoolean(value));
//                        case "result_count" -> setResultCount(value);
//                        case "filter_result" -> setFilterResult(value);
//                    }
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }


}
