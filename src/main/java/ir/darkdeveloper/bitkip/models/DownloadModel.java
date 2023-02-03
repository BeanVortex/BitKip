package ir.darkdeveloper.bitkip.models;

import javafx.beans.property.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Builder
public class DownloadModel {
    private String id;
    private String name;
    private double progress;
    private String size;
    private String url;
    private String filePath;
    private int remainingTime;
    private int chunks;
    private LocalDateTime addDate;
    private LocalDateTime lastTryDate;
    private LocalDateTime completeDate;

    private StringProperty nameProperty;
    private DoubleProperty progressProperty;
    private StringProperty sizeProperty;
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
        sizeProperty = new SimpleStringProperty(size);
        remainingTimeProperty = new SimpleIntegerProperty(remainingTime);
        chunksProperty = new SimpleIntegerProperty(chunks);
        addDateProperty = new SimpleStringProperty(DATE_FORMATTER.format(addDate));
        lastTryDateProperty = new SimpleStringProperty(DATE_FORMATTER.format(lastTryDate));
        completeDateProperty = new SimpleStringProperty(DATE_FORMATTER.format(completeDate));

    }
}
