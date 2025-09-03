package io.beanvortex.bitkip.repo;

import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.models.QueueModel;
import io.beanvortex.bitkip.models.ScheduleModel;
import io.beanvortex.bitkip.utils.Defaults;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.beanvortex.bitkip.config.AppConfigs.log;
import static io.beanvortex.bitkip.repo.DatabaseHelper.*;

public class QueuesRepo {

    public static final String COL_ID = "id",
            COL_NAME = "name",
            COL_EDITABLE = "editable",
            COL_CAN_ADD_DOWN = "can_add_download",
            COL_SCHEDULE_ID = "schedule_id",
            COL_HAS_FOLDER = "has_folder",
            COL_SPEED_LIMIT = "speed_limit",
            COL_DOWN_TOP = "download_from_top",
            COL_SIMUL_DOWNLOAD = "simultaneously_download";


    public static void createTable() {
        createQueuesTable();
        createQueueDownloadTable();
    }

    public static List<QueueModel> createDefaultRecords() {
        return Defaults.staticQueueNames.stream().map(name -> {
            var queue = new QueueModel(name, false);
            if (name.equals("All Downloads"))
                queue.setCanAddDownload(true);
            var schedule = new ScheduleModel();
            ScheduleRepo.insertSchedule(schedule, -1);
            queue.setSchedule(schedule);
            insertQueue(queue);
            ScheduleRepo.updateScheduleQueueId(schedule.getId(), queue.getId());
            return queue;
        }).toList();
    }

    private static void createQueuesTable() {
        var sql = """
                CREATE TABLE IF NOT EXISTS %s
                (
                    %s INTEGER PRIMARY KEY AUTOINCREMENT,
                    %s VARCHAR UNIQUE,
                    %s INTEGER,
                    %s INTEGER,
                    %s INTEGER,
                    %s INTEGER,
                    %s INTEGER,
                    %s VARCHAR,
                    %s INTEGER,
                    FOREIGN KEY (%s) REFERENCES %s(%s) ON DELETE CASCADE
                );
                """
                .formatted(QUEUES_TABLE_NAME,
                        COL_ID,
                        COL_NAME,
                        COL_EDITABLE,
                        COL_CAN_ADD_DOWN,
                        COL_HAS_FOLDER,
                        COL_DOWN_TOP,
                        COL_SIMUL_DOWNLOAD,
                        COL_SPEED_LIMIT,
                        COL_SCHEDULE_ID,
                        COL_SCHEDULE_ID, SCHEDULE_TABLE_NAME, COL_ID);
        DatabaseHelper.runSQL(sql, false);
        alters();
    }

    private static void createQueueDownloadTable() {
        var sql = """
                CREATE TABLE IF NOT EXISTS %s
                (
                    %s INTEGER,
                    %s INTEGER,
                    FOREIGN KEY (%s) REFERENCES %s(%s) ON DELETE CASCADE,
                    FOREIGN KEY (%s) REFERENCES %s(%s) ON DELETE CASCADE
                );
                """
                .formatted(QUEUE_DOWNLOAD_TABLE_NAME,
                        COL_DOWNLOAD_ID, COL_QUEUE_ID,
                        COL_DOWNLOAD_ID, DOWNLOADS_TABLE_NAME, COL_ID,
                        COL_QUEUE_ID, QUEUES_TABLE_NAME, COL_ID);
        DatabaseHelper.runSQL(sql, false);
    }


    public static void insertQueue(QueueModel queue) {
        var sql = """
                INSERT OR IGNORE INTO %s (%s,%s,%s,%s,%s,%s,%s,%s) VALUES("%s",%d,%d,%d,%d,%d,%s,%d);
                """
                .formatted(QUEUES_TABLE_NAME,
                        COL_NAME, COL_EDITABLE, COL_CAN_ADD_DOWN, COL_HAS_FOLDER, COL_SCHEDULE_ID,
                        COL_SIMUL_DOWNLOAD, COL_SPEED_LIMIT, COL_DOWN_TOP,
                        queue.getName(),
                        queue.isEditable() ? 1 : 0,
                        queue.isCanAddDownload() ? 1 : 0,
                        queue.hasFolder() ? 1 : 0,
                        queue.getSchedule().getId(),
                        queue.getSimultaneouslyDownload(),
                        queue.getSpeed(),
                        queue.isDownloadFromTop() ? 1 : 0);
        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
            var genKeys = stmt.getGeneratedKeys();
            genKeys.next();
            queue.setId(genKeys.getInt(1));
        } catch (SQLException e) {
            log.error(e.toString());
        }
    }

    public static QueueModel findByName(String name, boolean fetchDownloads) {
        var sql = """
                SELECT * FROM %s WHERE %s="%s";
                """
                .formatted(QUEUES_TABLE_NAME, COL_NAME, name);
        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return createQueueModel(rs, fetchDownloads, true);
        } catch (SQLException e) {
            log.error(e.toString());
        }
        throw new IllegalArgumentException("Queue does not exist");
    }


    public static List<QueueModel> getAllQueues(boolean fetchDownloads, boolean fetchSchedule) {
        var sql = "SELECT * FROM " + QUEUES_TABLE_NAME + ";";
        return getQueues(fetchDownloads, fetchSchedule, sql);
    }

    private static ArrayList<QueueModel> getQueues(boolean fetchDownloads, boolean fetchSchedule, String sql) {
        var list = new ArrayList<QueueModel>();
        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next())
                list.add(createQueueModel(rs, fetchDownloads, fetchSchedule));
            return list;
        } catch (SQLException e) {
            log.error(e.toString());
        }
        return list;
    }

    public static void deleteQueue(String name) {
        var sql = """
                DELETE FROM %s WHERE %s = "%s";
                """
                .formatted(QUEUES_TABLE_NAME, COL_NAME, name);
        DatabaseHelper.runSQL(sql, false);
    }

    public static List<QueueModel> findQueuesOfADownload(int downloadId, boolean fetchDownloads, boolean fetchSchedule) {
        var sql = """
                SELECT q.*
                FROM %s q
                         INNER JOIN %s qd ON q.%s = qd.%s
                         INNER JOIN %s d ON d.%s = qd.%s
                WHERE d.%s = %d;
                """
                .formatted(QUEUES_TABLE_NAME,
                        QUEUE_DOWNLOAD_TABLE_NAME, COL_ID, COL_QUEUE_ID,
                        DOWNLOADS_TABLE_NAME, COL_ID, COL_DOWNLOAD_ID,
                        COL_ID, downloadId);
        return getQueues(fetchDownloads, fetchSchedule, sql);
    }

    private static void alters() {
        // NEW ALTERS SHOULD ADD ON TOP
        var addAlters = """
                ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 1;
                ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 0;
                ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 1;
                ALTER TABLE %s ADD COLUMN %s VARCHAR DEFAULT "0";
                ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT NULL REFERENCES %s(%s) ON DELETE CASCADE;
                """
                .formatted(
                        QUEUES_TABLE_NAME, COL_DOWN_TOP,
                        QUEUES_TABLE_NAME, COL_HAS_FOLDER,
                        QUEUES_TABLE_NAME, COL_SIMUL_DOWNLOAD,
                        QUEUES_TABLE_NAME, COL_SPEED_LIMIT,
                        QUEUES_TABLE_NAME, COL_SCHEDULE_ID, SCHEDULE_TABLE_NAME, COL_ID
                );
        DatabaseHelper.runSQL(addAlters, true);
    }

    static QueueModel createQueueModel(ResultSet rs, boolean fetchDownloads, boolean fetchSchedule) throws SQLException {
        var id = rs.getInt(COL_ID);
        var name = rs.getString(COL_NAME);
        var editable = rs.getBoolean(COL_EDITABLE);
        var canAddDownload = rs.getBoolean(COL_CAN_ADD_DOWN);
        var hasFolder = rs.getBoolean(COL_HAS_FOLDER);
        var downloadFromTop = rs.getBoolean(COL_DOWN_TOP);
        var speedLimit = rs.getString(COL_SPEED_LIMIT);
        var simulDownloads = rs.getInt(COL_SIMUL_DOWNLOAD);
        CopyOnWriteArrayList<DownloadModel> downloads = null;
        if (fetchDownloads)
            downloads = new CopyOnWriteArrayList<>(DownloadsRepo.getDownloadsByQueueName(name, false));
        ScheduleModel schedule = null;
        if (fetchSchedule)
            schedule = ScheduleRepo.getSchedule(id);
        return new QueueModel(id, name, editable, canAddDownload, hasFolder, downloadFromTop,
                speedLimit, simulDownloads, schedule, downloads);
    }
}
