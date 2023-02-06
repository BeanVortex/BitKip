package ir.darkdeveloper.bitkip.models;

import javafx.beans.property.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    @Builder.Default
    private String queue = "All Downloads";
    private LocalDateTime addDate;
    private LocalDateTime lastTryDate;
    private LocalDateTime completeDate;

    private StringProperty nameProperty;
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

}
