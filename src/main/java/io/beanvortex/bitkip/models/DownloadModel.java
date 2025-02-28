package io.beanvortex.bitkip.models;

import io.beanvortex.bitkip.repo.DatabaseHelper;
import io.beanvortex.bitkip.repo.QueuesRepo;
import io.beanvortex.bitkip.repo.ScheduleRepo;
import io.beanvortex.bitkip.task.DownloadTask;
import io.beanvortex.bitkip.utils.IOUtils;
import io.beanvortex.bitkip.utils.MainTableUtils;
import lombok.*;

import static io.beanvortex.bitkip.repo.DownloadsRepo.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private String uri;
    private String filePath;
    private String remainingTime;
    @Builder.Default
    private CopyOnWriteArrayList<QueueModel> queues = new CopyOnWriteArrayList<>();
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
    private TurnOffMode turnOffMode;
    private Credential credential;

    private long speedLimit;
    private long byteLimit;

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
        return "%s (%s)".formatted(downloadStatus, MainTableUtils.dFormat.format(progress) + " %");
    }

    @Override
    public String toString() {
        return "DownloadModel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", progress=" + progress +
                ", size=" + size +
                ", downloaded=" + downloaded +
                ", uri='" + uri + '\'' +
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

    public static DownloadModel createDownload(ResultSet rs, boolean fetchQueue) throws SQLException {
        var id = rs.getInt(COL_ID);
        var name = rs.getString(COL_NAME);
        var progress = rs.getFloat(COL_PROGRESS);
        var downloaded = rs.getLong(COL_DOWNLOADED);
        var size = rs.getLong(COL_SIZE);
        var url = rs.getString(COL_URL);
        var filePath = rs.getString(COL_PATH);
        var chunks = rs.getInt(COL_CHUNKS);
        var showCompleteDialog = rs.getBoolean(COL_SHOW_COMPLETE_DIALOG);
        var openAfterComplete = rs.getBoolean(COL_OPEN_AFTER_COMPLETE);
        var resumable = rs.getBoolean(COL_RESUMABLE);
        var turnOffMode = TurnOffMode.valueOf(rs.getString(COL_TURNOFF_MODE));
        var addDate = rs.getString(COL_ADD_DATE);
        var addDateStr = LocalDateTime.parse(addDate);
        var addToQueueDate = rs.getString(COL_ADD_TO_QUEUE_DATE);
        var addToQueueDateStr = LocalDateTime.parse(addToQueueDate);
        var lastTryDate = rs.getString(COL_LAST_TRY_DATE);
        var lastTryDateStr = lastTryDate == null ? null : LocalDateTime.parse(lastTryDate);
        var completeDate = rs.getString(COL_COMPLETE_DATE);
        var completeDateStr = completeDate == null ? null : LocalDateTime.parse(completeDate);
        var downloadStatus = progress != 100 ? DownloadStatus.Paused : DownloadStatus.Completed;
        var credential = Credential.decrypt(rs.getString(COL_CREDENTIAL));

        var build = DownloadModel.builder()
                .id(id).name(name).progress(progress).downloaded(downloaded).size(size).uri(url).filePath(filePath)
                .chunks(chunks).addDate(addDateStr).addToQueueDate(addToQueueDateStr).turnOffMode(turnOffMode)
                .lastTryDate(lastTryDateStr).completeDate(completeDateStr).openAfterComplete(openAfterComplete)
                .showCompleteDialog(showCompleteDialog).downloadStatus(downloadStatus).resumable(resumable)
                .credential(credential)
                .build();

        if (fetchQueue) {
            var queueId = rs.getInt(DatabaseHelper.COL_QUEUE_ID);
            var queueName = rs.getString(DatabaseHelper.COL_QUEUE_NAME);
            var scheduleId = rs.getInt(QueuesRepo.COL_SCHEDULE_ID);
            var schedule = ScheduleRepo.createScheduleModel(rs, scheduleId);
            var queue = QueueModel.createQueueModel(rs, queueId, queueName, schedule);
            var queues = new CopyOnWriteArrayList<>(Collections.singletonList(queue));
            build.setQueues(queues);
        }
        return build;
    }
}
