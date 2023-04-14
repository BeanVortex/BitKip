package ir.darkdeveloper.bitkip.models;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ScheduleModel {

    private int id;
    private boolean enabled;
    private LocalTime startTime;
    private boolean onceDownload;
    private LocalDate startDate;
    private Set<DayOfWeek> days;
    private boolean stopTimeEnabled;
    private LocalTime stopTime;
    private boolean turnOffEnabled;
    private TurnOffMode turnOffMode;
    private int queueId;

    private ScheduledExecutorService startScheduler;
    private ScheduledExecutorService stopScheduler;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleModel that = (ScheduleModel) o;
        return id == that.id && enabled == that.enabled && onceDownload == that.onceDownload &&
                stopTimeEnabled == that.stopTimeEnabled && turnOffEnabled == that.turnOffEnabled &&
                queueId == that.queueId && Objects.equals(startTime, that.startTime) &&
                Objects.equals(startDate, that.startDate) && days.equals(that.days) &&
                Objects.equals(stopTime, that.stopTime) && turnOffMode == that.turnOffMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, enabled, startTime, onceDownload, startDate, days,
                stopTimeEnabled, stopTime, turnOffEnabled, turnOffMode, queueId);
    }
}
