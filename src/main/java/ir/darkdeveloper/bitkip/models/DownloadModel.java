package ir.darkdeveloper.bitkip.models;

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
    private LocalDateTime addDate;
    private LocalDateTime lastTryDate;
    private LocalDateTime completeDate;
    private int chunks;
}
