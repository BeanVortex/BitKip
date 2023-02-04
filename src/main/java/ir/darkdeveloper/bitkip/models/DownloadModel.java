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
public class DownloadModel {
    private String id;
    private String name;
    private double progress;
    private int size;
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
    private IntegerProperty sizeProperty;
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
        sizeProperty = new SimpleIntegerProperty(size);
        remainingTimeProperty = new SimpleIntegerProperty(remainingTime);
        chunksProperty = new SimpleIntegerProperty(chunks);
        addDateProperty = new SimpleStringProperty(DATE_FORMATTER.format(addDate));
        lastTryDateProperty = new SimpleStringProperty(DATE_FORMATTER.format(lastTryDate));
        completeDateProperty = new SimpleStringProperty(DATE_FORMATTER.format(completeDate));

    }

    public String[] getDataInArr() {
        return new String[]{
                this.id,
                this.name,
                this.progress + "",
                this.size + "",
                this.url,
                this.filePath,
                this.remainingTime + "",
                this.chunks + "",
                this.queue,
                this.addDate.toString(),
                this.lastTryDate.toString(),
                this.completeDate.toString()
        };
    }

    public void mapDataFromArr(String[] arr) {
        this.id = arr[0];
        this.name = arr[1];
        this.progress = Double.parseDouble(arr[2]);
        this.size = Integer.parseInt(arr[3]);
        this.url = arr[4];
        this.filePath = arr[5];
        this.remainingTime = Integer.parseInt(arr[6]);
        this.chunks = Integer.parseInt(arr[7]);
        this.queue = arr[8];
        this.addDate = LocalDateTime.parse(arr[9]);
        this.lastTryDate = LocalDateTime.parse(arr[10]);
        this.completeDate = LocalDateTime.parse(arr[11]);

    }
}
