package ir.darkdeveloper.bitkip.models;

import lombok.*;

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
    private LocalTime startTime;

    // if true, it means startDate should not null
    private boolean onceDownload;
    private LocalDate startDate;

    private Set<Day> days;

    private LocalTime stopTime;

    private TurnOffMode turnOffMode;

    private int queueId;


}
