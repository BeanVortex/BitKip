package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.models.Day;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.models.ScheduleModel;
import ir.darkdeveloper.bitkip.models.TurnOffMode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static ir.darkdeveloper.bitkip.models.Day.*;
import static ir.darkdeveloper.bitkip.repo.DatabaseHelper.*;

public class ScheduleRepo {
    static final String COL_ID = "id",
            COL_ENABLED = "enabled",
            COL_START_TIME = "start_time",
            COL_ONCE_DOWNLOAD = "once_download",
            COL_START_DATE = "start_date",
            COL_DAYS = "days",
            COL_STOP_TIME_ENABLED = "stop_time_enabled",
            COL_STOP_TIME = "stop_time",
            COL_TURN_OFF_MODE_ENABLED = "turn_off_mode_enabled",
            COL_TURN_OFF_MODE = "turn_off_mode",
            COL_SPEED_LIMIT = "speed_limit",
            COL_SIMUL_DOWNLOAD = "simultaneously_download";

    public static final Set<Day> DAYS = Set.of(SATURDAY, SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);


    public static void createSchedulesTable() {
        var sql = """
                CREATE TABLE IF NOT EXISTS %s
                (
                    %s INTEGER PRIMARY KEY AUTOINCREMENT,
                    %s INTEGER NOT NULL,
                    %s VARCHAR,
                    %s INTEGER NOT NULL,
                    %s VARCHAR,
                    %s VARCHAR,
                    %s INTEGER,
                    %s VARCHAR,
                    %s INTEGER,
                    %s VARCHAR,
                    %s INTEGER,
                    %s INTEGER,
                    %s INTEGER,
                    FOREIGN KEY (%s) REFERENCES %s(%s)
                    );
                """
                .formatted(SCHEDULE_TABLE_NAME,
                        COL_ID,
                        COL_ENABLED,
                        COL_START_TIME,
                        COL_ONCE_DOWNLOAD,
                        COL_START_DATE,
                        COL_DAYS,
                        COL_STOP_TIME_ENABLED,
                        COL_STOP_TIME,
                        COL_TURN_OFF_MODE_ENABLED,
                        COL_TURN_OFF_MODE,
                        COL_SIMUL_DOWNLOAD,
                        COL_SPEED_LIMIT,
                        COL_QUEUE_ID,
                        COL_QUEUE_ID, QUEUES_TABLE_NAME, COL_ID);
        DatabaseHelper.createTable(sql);
    }

    public static void insertSchedule(ScheduleModel schedule, int queueId) {
        var m = validScheduleProperties(schedule);
        var insertToScheduleSql = """
                INSERT INTO %s (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) VALUES(%d,%s,%d,%s,%s,%d,%s,%d,%s,%d,%d,%s);
                """
                .formatted(SCHEDULE_TABLE_NAME, COL_ENABLED, COL_START_TIME,
                        COL_ONCE_DOWNLOAD, COL_START_DATE, COL_DAYS,
                        COL_STOP_TIME_ENABLED, COL_STOP_TIME, COL_TURN_OFF_MODE_ENABLED,
                        COL_TURN_OFF_MODE, COL_SIMUL_DOWNLOAD, COL_SPEED_LIMIT, COL_QUEUE_ID,
                        schedule.isEnabled() ? 1 : 0,
                        m.get(COL_START_TIME),
                        schedule.isOnceDownload() ? 1 : 0,
                        m.get(COL_START_DATE), m.get(COL_DAYS),
                        schedule.isStopTimeEnabled() ? 1 : 0,
                        m.get(COL_STOP_TIME),
                        schedule.isTurnOffEnabled() ? 1 : 0,
                        m.get(COL_TURN_OFF_MODE),
                        schedule.getSimultaneouslyDownload(),
                        schedule.getSpeed(),
                        queueId == -1 ? "NULL" : queueId);

        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement()) {
            stmt.executeUpdate(insertToScheduleSql);
            var genKeys = stmt.getGeneratedKeys();
            genKeys.next();
            var scheduleId = genKeys.getInt(1);
            schedule.setId(scheduleId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ScheduleModel getSchedule(int queueId) {
        var sql = """
                SELECT * FROM %s WHERE %s=%d;
                """
                .formatted(SCHEDULE_TABLE_NAME, COL_QUEUE_ID, queueId);
        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return createScheduleModel(rs, -1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    static ScheduleModel createScheduleModel(ResultSet rs, int id) throws SQLException {
        if (id == -1)
            id = rs.getInt(COL_ID);
        var startTimeString = rs.getString(COL_START_TIME);
        var enabled = rs.getBoolean(COL_ENABLED);
        var onceDownload = rs.getBoolean(COL_ONCE_DOWNLOAD);
        var startDateString = rs.getString(COL_START_DATE);
        var speed = rs.getLong(COL_SPEED_LIMIT);
        var simulDownload = rs.getInt(COL_SIMUL_DOWNLOAD);
        var stopTimeEnabled = rs.getBoolean(COL_STOP_TIME_ENABLED);
        var stopTimeString = rs.getString(COL_STOP_TIME);
        var daysAsString = rs.getString(COL_DAYS);
        var turnOffEnabled = rs.getBoolean(COL_TURN_OFF_MODE_ENABLED);
        var turnOffModeString = rs.getString(COL_TURN_OFF_MODE);
        var queueId = rs.getInt(COL_QUEUE_ID);

        var vars = daysAsString.split(",");
        var days = Arrays.stream(vars)
                .map(Day::valueOf)
                .collect(Collectors.toSet());
        var startTime = startTimeString == null ? null : LocalTime.parse(startTimeString);
        var startDate = startDateString == null ? null : LocalDate.parse(startDateString);
        var stopTime = stopTimeString == null ? null : LocalTime.parse(stopTimeString);
        var turnOffMode = turnOffModeString == null ? null : TurnOffMode.valueOf(turnOffModeString);
        return new ScheduleModel(id, enabled, startTime, onceDownload, startDate, days, speed, simulDownload,
                stopTimeEnabled, stopTime, turnOffEnabled, turnOffMode, queueId);
    }

    public static void updateSchedule(ScheduleModel schedule) {
        var m = validScheduleProperties(schedule);

        var sql = """
                UPDATE %s SET %s=%s,%s=%d,%s=%s,%s=%s,%s=%s,%s=%s,%s=%d,%s=%d,%s=%d,%s=%d,%s=%d
                WHERE %s=%d;
                """
                .formatted(SCHEDULE_TABLE_NAME,
                        COL_START_TIME, m.get(COL_START_TIME),
                        COL_ONCE_DOWNLOAD, schedule.isOnceDownload() ? 1 : 0,
                        COL_START_DATE, m.get(COL_START_DATE),
                        COL_DAYS, m.get(COL_DAYS),
                        COL_STOP_TIME, m.get(COL_STOP_TIME),
                        COL_TURN_OFF_MODE, m.get(COL_TURN_OFF_MODE),
                        COL_ENABLED, schedule.isEnabled() ? 1 : 0,
                        COL_TURN_OFF_MODE_ENABLED, schedule.isTurnOffEnabled() ? 1 : 0,
                        COL_STOP_TIME_ENABLED, schedule.isStopTimeEnabled() ? 1 : 0,
                        COL_SIMUL_DOWNLOAD, schedule.getSimultaneouslyDownload(),
                        COL_SPEED_LIMIT, schedule.getSpeed(),
                        COL_ID, schedule.getId()
                );
        DatabaseHelper.executeUpdateSql(sql, false);
    }

    public static void updateScheduleQueueId(int scheduleId, int queueId) {
        var sql = """
                UPDATE %s SET %s=%d WHERE %s=%d;
                """
                .formatted(SCHEDULE_TABLE_NAME, COL_QUEUE_ID, queueId, COL_ID, scheduleId);
        DatabaseHelper.executeUpdateSql(sql, false);
    }

    // for removal in future
    public static List<QueueModel> createDefaultSchedulesForQueues(List<QueueModel> queues) {
        return queues.stream().peek(queue -> {
            if (queue.getSchedule() != null)
                return;
            var defaultSchedule = new ScheduleModel();
            insertSchedule(defaultSchedule, queue.getId());
            QueuesRepo.updateQueueScheduleId(queue.getId(), defaultSchedule.getId());
            queue.setSchedule(defaultSchedule);
        }).toList();
    }

    static Map<String, String> validScheduleProperties(ScheduleModel schedule) {
        var startTime = "NULL";
        if (schedule.getStartTime() != null)
            startTime = "\"" + schedule.getStartTime() + "\"";
        var startDate = "NULL";
        if (schedule.getStartDate() != null)
            startDate = "\"" + schedule.getStartDate() + "\"";
        var stopTime = "NULL";
        if (schedule.getStopTime() != null)
            stopTime = "\"" + schedule.getStopTime() + "\"";
        var turnOffMode = "NULL";
        if (schedule.getTurnOffMode() != null)
            turnOffMode = "\"" + schedule.getTurnOffMode() + "\"";
        var days = "NULL";
        if (schedule.getDays() == null)
            schedule.setDays(DAYS);
        var daysAsString = schedule.getDays().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
        days = "\"" + daysAsString + "\"";

        var m = new HashMap<String, String>();
        m.put(COL_START_TIME, startTime);
        m.put(COL_START_DATE, startDate);
        m.put(COL_STOP_TIME, stopTime);
        m.put(COL_TURN_OFF_MODE, turnOffMode);
        m.put(COL_DAYS, days);
        return m;
    }

}