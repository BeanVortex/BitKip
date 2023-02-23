package ir.darkdeveloper.bitkip.models;

import ir.darkdeveloper.bitkip.task.DownloadTask;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import lombok.*;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DownloadModel {
    private int id;
    private String name;
    private float progress;
    private long size;
    private String url;
    private String filePath;
    private String remainingTime;
    private List<QueueModel> queue;
    private int chunks;
    private long speed;
    private DownloadStatus downloadStatus;
    private DownloadTask downloadTask;
    private LocalDateTime addDate;
    private LocalDateTime lastTryDate;
    private LocalDateTime completeDate;

    private String sizeString;
    private String progressString;
    private String speedString;
    private String addDateString;
    private String lastTryDateString;
    private String completeDateString;


    public static final String DATE_FORMAT = "yyyy/MM/dd - HH:mm:ss";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (DownloadModel) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    public String getAddDateString() {
        return DATE_FORMATTER.format(addDate);
    }

    public String getLastTryDateString() {
        return DATE_FORMATTER.format(lastTryDate);
    }

    public String getCompleteDateString() {
        if (completeDate == null)
            return "";
        return DATE_FORMATTER.format(completeDate);
    }

    public String getSizeString() {
        return IOUtils.formatBytes(size);
    }

    public String getProgressString() {
        return new DecimalFormat("##.#").format(progress) + " %";
    }

    public String getSpeedString() {
        return IOUtils.formatBytes(speed);
    }

}
