package ir.darkdeveloper.bitkip.models;

import javafx.beans.property.*;
import lombok.*;

import java.time.LocalDateTime;

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

    public void fillProperties() {
        nameProperty = new SimpleStringProperty(name);
        progressProperty = new SimpleDoubleProperty(progress);
        sizeProperty = new SimpleStringProperty(size);
        remainingTimeProperty = new SimpleIntegerProperty(remainingTime);
        chunksProperty = new SimpleIntegerProperty(chunks);
        addDateProperty = new SimpleStringProperty(addDate.toString());
        lastTryDateProperty = new SimpleStringProperty(lastTryDate.toString());
        completeDateProperty = new SimpleStringProperty(completeDate.toString());

    }
}
