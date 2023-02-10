package ir.darkdeveloper.bitkip.models;

import javafx.beans.property.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DownloadModel implements Model {
    private int id;
    private String name;
    private double progress;
    private long size;
    private String url;
    private String filePath;
    private int remainingTime;
    private int chunks;
    //    private boolean downloadedInChunks;
    private StringProperty nameProperty;
    private List<QueueModel> queue;
    private LocalDateTime addDate;
    private LocalDateTime lastTryDate;
    private LocalDateTime completeDate;

    private DoubleProperty progressProperty;
    private LongProperty sizeProperty;
    private IntegerProperty remainingTimeProperty;
    private IntegerProperty chunksProperty;
    private StringProperty addDateProperty;
    private StringProperty lastTryDateProperty;
    private StringProperty completeDateProperty;

    public static final String DATE_FORMAT = "yyyy/MM/dd/-HH:mm:ss";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public void fillProperties() {
        nameProperty = new SimpleStringProperty(name);
        progressProperty = new SimpleDoubleProperty(progress);
        sizeProperty = new SimpleLongProperty(size);
        remainingTimeProperty = new SimpleIntegerProperty(remainingTime);
        chunksProperty = new SimpleIntegerProperty(chunks);
        addDateProperty = new SimpleStringProperty(DATE_FORMATTER.format(addDate));
        lastTryDateProperty = new SimpleStringProperty(DATE_FORMATTER.format(lastTryDate));
        completeDateProperty = new SimpleStringProperty(DATE_FORMATTER.format(completeDate));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (DownloadModel) o;
        // for distinction
        that.getQueue().addAll(queue);
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
