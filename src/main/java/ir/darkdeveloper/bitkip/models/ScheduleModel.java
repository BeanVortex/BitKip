package ir.darkdeveloper.bitkip.models;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ScheduleModel {

    private int id;
    private boolean enabled;
    private LocalTime startTime;

    // if true, it means startDate should not null
    private boolean onceDownload;
    private LocalDate startDate;

    private Set<DayOfWeek> days;

    private long speed;
    private int simultaneouslyDownload;

    private boolean stopTimeEnabled;
    private LocalTime stopTime;

    private boolean turnOffEnabled;
    private TurnOffMode turnOffMode;

    private int queueId;

}
