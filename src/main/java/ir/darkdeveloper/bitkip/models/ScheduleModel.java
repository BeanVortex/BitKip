package ir.darkdeveloper.bitkip.models;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
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

    private final Set<Day> days = new LinkedHashSet<>();

    private LocalTime stopTime;

    private TurnOffMode turnOffMode;



}
