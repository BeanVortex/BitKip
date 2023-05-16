package ir.darkdeveloper.bitkip.models;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.utils.Defaults;

import java.util.List;

public enum FileType {
    COMPRESSED(Defaults.compressedEx, AppConfigs.compressedPath),
    VIDEO(Defaults.videoEx, AppConfigs.videosPath),
    PROGRAM(Defaults.programEx, AppConfigs.programsPath),
    MUSIC(Defaults.musicEx, AppConfigs.musicPath),
    DOCUMENT(Defaults.documentEx, AppConfigs.documentPath),
    OTHER(List.of(), AppConfigs.othersPath),
    QUEUES(List.of(), AppConfigs.queuesPath);

    private final List<String> extensions;
    private final String path;

    FileType(List<String> extensions, String path) {
        this.extensions = extensions;
        this.path = path;
    }

    public static FileType determineFileType(String fileName) {
        for (FileType type : FileType.values())
            if (type.extensions.stream().anyMatch(fileName::endsWith))
                return type;

        return OTHER;
    }

    public String getPath() {
        return path;
    }
}