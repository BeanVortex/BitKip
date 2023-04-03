package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.models.ScheduleModel;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static ir.darkdeveloper.bitkip.repo.DatabaseHelper.QUEUES_TABLE_NAME;
import static ir.darkdeveloper.bitkip.repo.DatabaseHelper.SCHEDULE_TABLE_NAME;
import static ir.darkdeveloper.bitkip.repo.QueuesRepo.COL_SCHEDULE_ID;

public class ScheduleRepo {
    private static final String COL_ID = "id",
            COL_START_TIME = "start_time",
            COL_ONCE_DOWNLOAD = "once_download",
            COL_START_DATE = "start_date",
            COL_DAYS = "days",
            COL_STOP_TIME = "stop_time",
            COL_TURN_OFF_MODE = "turn_off_mode";

    public static void createSchedulesTable() {
        var sql = """
                CREATE TABLE IF NOT EXISTS %s
                (
                    %s INTEGER PRIMARY KEY AUTOINCREMENT,
                    %s VARCHAR NOT NULL,
                    %s INTEGER NOT NULL,
                    %s VARCHAR,
                    %s VARCHAR,
                    %s VARCHAR,
                    %s VARCHAR;
                    );
                """
                .formatted(SCHEDULE_TABLE_NAME,
                        COL_ID,
                        COL_START_TIME,
                        COL_ONCE_DOWNLOAD,
                        COL_START_DATE,
                        COL_DAYS,
                        COL_STOP_TIME,
                        COL_TURN_OFF_MODE);
        DatabaseHelper.createTable(sql);
    }

    public static void insertSchedule(ScheduleModel schedule, int queueId) {
        var m = validScheduleProperties(schedule);
        var insertToScheduleSql = """
                INSERT OR IGNORE INTO %s (%s,%s,%s,%s,%s,%s) VALUES("%s",%d,%s,%s,%s,%s);
                """
                .formatted(SCHEDULE_TABLE_NAME, COL_START_TIME,
                        COL_ONCE_DOWNLOAD, COL_START_DATE, COL_DAYS,
                        COL_STOP_TIME, COL_TURN_OFF_MODE,
                        schedule.getStartTime(),
                        schedule.isOnceDownload() ? 1 : 0,
                        m.get(COL_START_DATE), m.get(COL_DAYS),
                        m.get(COL_STOP_TIME), m.get(COL_TURN_OFF_MODE));

        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement()) {
            stmt.executeUpdate(insertToScheduleSql);
            var genKeys = stmt.getGeneratedKeys();
            genKeys.next();
            var scheduleId = genKeys.getInt(1);
            schedule.setId(scheduleId);
            var updateQueueSql = """
                    UPDATE %s SET %s=%d WHERE %s=%d;
                    """
                    .formatted(QUEUES_TABLE_NAME,
                            COL_SCHEDULE_ID, scheduleId,
                            COL_ID, queueId);
            stmt.executeUpdate(updateQueueSql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void updateSchedule(ScheduleModel schedule) {
        var m = validScheduleProperties(schedule);

        var sql = """
                UPDATE %s SET %s="%s",%s=%d,%s=%s,%s=%s,%s=%s,%s=%s
                WHERE %s=%d;
                """
                .formatted(SCHEDULE_TABLE_NAME,
                        COL_START_TIME, schedule.getStartTime(),
                        COL_ONCE_DOWNLOAD, schedule.isOnceDownload() ? 1 : 0,
                        COL_START_DATE, m.get(COL_START_DATE),
                        COL_DAYS, m.get(COL_DAYS),
                        COL_STOP_TIME, m.get(COL_STOP_TIME),
                        COL_TURN_OFF_MODE, m.get(COL_TURN_OFF_MODE),
                        COL_ID, schedule.getId()
                );
        DatabaseHelper.executeUpdateSql(sql);
    }

    public static void deleteSchedule(int id) {
        var sql = """
                DELETE FROM %s WHERE %s = %d;
                """
                .formatted(SCHEDULE_TABLE_NAME, COL_ID, id);
        DatabaseHelper.executeUpdateSql(sql);
    }

    private static Map<String, String> validScheduleProperties(ScheduleModel schedule) {
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
        if (schedule.getDays() != null) {
            var daysAsString = schedule.getDays().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
            days = "\"" + daysAsString + "\"";
        }
        var m = new HashMap<String, String>();
        m.put(COL_START_DATE, startDate);
        m.put(COL_STOP_TIME, stopTime);
        m.put(COL_TURN_OFF_MODE, turnOffMode);
        m.put(COL_DAYS, days);
        return m;
    }

}