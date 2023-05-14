package ir.darkdeveloper.bitkip.models;

import ir.darkdeveloper.bitkip.task.DownloadTask;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static ir.darkdeveloper.bitkip.utils.MainTableUtils.dFormat;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DownloadModel {
    private int id;
    private String name;
    private float progress;
    private long size;
    private long downloaded;
    private String url;
    private String filePath;
    private String remainingTime;
    private CopyOnWriteArrayList<QueueModel> queues;
    private int chunks;
    private long speed;
    private DownloadStatus downloadStatus;
    private DownloadTask downloadTask;
    private LocalDateTime addDate;
    private LocalDateTime addToQueueDate;
    private LocalDateTime lastTryDate;
    private LocalDateTime completeDate;
    private boolean openAfterComplete;
    private boolean showCompleteDialog;
    private boolean resumable;

    private String sizeString;
    private String downloadedString;
    private String downloadStatusString;
    private String speedString;
    private String addDateString;
    private String addToQueueDateString;
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

    public String getAddToQueueDateString() {
        return DATE_FORMATTER.format(addToQueueDate);
    }

    public String getLastTryDateString() {
        if (lastTryDate == null)
            return "";
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

    public String getDownloadedString() {
        return IOUtils.formatBytes(downloaded);
    }

    public String getSpeedString() {
        return IOUtils.formatBytes(speed);
    }

    public String getDownloadStatusString() {
        return "%s (%s)".formatted(downloadStatus, dFormat.format(progress) + " %");
    }

    @Override
    public String toString() {
        return "DownloadModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", progress=" + progress +
                ", size=" + size +
                ", downloaded=" + downloaded +
                ", url='" + url + '\'' +
                ", filePath='" + filePath + '\'' +
                ", remainingTime='" + remainingTime + '\'' +
                ", queues=" + queues.stream().map(QueueModel::toStringModel).toList() +
                ", chunks=" + chunks +
                ", speed=" + speed +
                ", downloadStatus=" + downloadStatus +
                ", downloadTask=" + downloadTask +
                ", addDate=" + addDate +
                ", addToQueueDate=" + addToQueueDate +
                ", lastTryDate=" + lastTryDate +
                ", completeDate=" + completeDate +
                ", openAfterComplete=" + openAfterComplete +
                ", showCompleteDialog=" + showCompleteDialog +
                ", resumable=" + resumable +
                '}';
    }
}
