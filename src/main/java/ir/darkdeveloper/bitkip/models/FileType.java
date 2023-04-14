package ir.darkdeveloper.bitkip.models;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.utils.FileExtensions;

import java.util.List;

public enum FileType {
    COMPRESSED(FileExtensions.compressedEx, AppConfigs.compressedPath),
    VIDEO(FileExtensions.videoEx, AppConfigs.videosPath),
    PROGRAM(FileExtensions.programEx, AppConfigs.programsPath),
    MUSIC(FileExtensions.musicEx, AppConfigs.musicPath),
    DOCUMENT(FileExtensions.documentEx, AppConfigs.documentPath),
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