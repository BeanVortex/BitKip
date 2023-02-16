package ir.darkdeveloper.bitkip.utils;

import java.util.List;

public class FileExtensions {

    public static final List<String> staticQueueNames = List.of("All Downloads", "Compressed",
            "Music", "Videos", "Programs", "Docs", "Others");
    public static final List<String> musicEx = List.of("mp3", "ogg", "wav", "mpa",
            "cda", "aif", "mid", "midi", "wma", "wpl");
    public static final List<String> compressedEx = List.of("rar", "zip", "7z", "deb",
            "pkg", "rpm", "tar", "tar.gz", "z");

    public static final List<String> videoEx = List.of("mp4", "mov", "mpg", "mpeg",
            "mkv", "avi", "flv", "m4v", "h264", "swf", "vob", "webm", "wmv", "3gp");
    public static final List<String> programEx = List.of("bin", "dmg", "iso", "msi",
            "exe", "sh", "cmd", "apk", "bat", "cgi", "jar", "py", "wsf");

    public static final List<String> documentEx = List.of("csv", "dat", "db", "sql",
            "xml", "html", "htm", "ppt", "odp", "pptx", "pps", "ods", "xls", "xlsm", "xlsx",
            "doc", "docx", "odt", "pdf", "rtf", "txt", "tex", "wpd");

}
