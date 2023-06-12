package ir.darkdeveloper.bitkip.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Defaults {

    public static final String ALL_DOWNLOADS_QUEUE = "All Downloads";
    public static final String COMPRESSED_QUEUE = "Compressed";
    public static final String PROGRAMS_QUEUE = "Programs";
    public static final String VIDEOS_QUEUE = "Videos";
    public static final String DOCS_QUEUE = "Docs";
    public static final String MUSIC_QUEUE = "Music";
    public static final String OTHERS_QUEUE = "Others";

    public static final List<String> staticQueueNames = List.of(ALL_DOWNLOADS_QUEUE, COMPRESSED_QUEUE,
            MUSIC_QUEUE, VIDEOS_QUEUE, PROGRAMS_QUEUE, DOCS_QUEUE, OTHERS_QUEUE);

    public static final List<String> compressedEx = List.of("rar", "zip", "7z", "deb", "compressed",
            "pkg", "rpm", "tar", "tar.gz", "z");
    public static final List<String> programEx = List.of("bin", "dmg", "iso", "msi",
            "exe", "sh", "cmd", "apk", "bat", "cgi", "jar", "py", "wsf");
    public static final List<String> videoEx = List.of("mp4", "mov", "mpg", "mpeg",
            "mkv", "avi", "flv", "m4v", "h264", "swf", "vob", "webm", "wmv", "3gp");
    public static final List<String> documentEx = List.of("csv", "dat", "db", "sql",
            "xml", "html", "htm", "ppt", "odp", "pptx", "pps", "ods", "xls", "xlsm", "xlsx",
            "doc", "docx", "odt", "pdf", "rtf", "txt", "tex", "wpd");
    public static final List<String> musicEx = List.of("mp3", "ogg", "wav", "mpa",
            "cda", "aif", "mid", "midi", "wma", "wpl");


    public static final LinkedHashMap<String, List<String>> extensions = new LinkedHashMap<>();

    static {
        extensions.put(COMPRESSED_QUEUE, compressedEx);
        extensions.put(PROGRAMS_QUEUE, programEx);
        extensions.put(VIDEOS_QUEUE, videoEx);
        extensions.put(DOCS_QUEUE, documentEx);
        extensions.put(MUSIC_QUEUE, musicEx);
        extensions.put(OTHERS_QUEUE, new ArrayList<>());
    }
}
