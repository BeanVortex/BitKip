package io.beanvortex.bitkip.repo;

import io.beanvortex.bitkip.models.DownloadModel;
import io.beanvortex.bitkip.models.DownloadStatus;
import io.beanvortex.bitkip.models.QueueModel;
import io.beanvortex.bitkip.models.TurnOffMode;
import io.beanvortex.bitkip.utils.Defaults;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.beanvortex.bitkip.config.AppConfigs.log;

public class DownloadsRepo {


    public static final String COL_ID = "id",
            COL_NAME = "name",
            COL_PROGRESS = "progress",
            COL_SIZE = "size",
            COL_DOWNLOADED = "downloaded",
            COL_OPEN_AFTER_COMPLETE = "open_after_complete",
            COL_SHOW_COMPLETE_DIALOG = "show_complete_dialog",
            COL_URL = "url",
            COL_CHUNKS = "chunks",
            COL_ADD_DATE = "add_date",
            COL_ADD_TO_QUEUE_DATE = "add_to_queue_date",
            COL_LAST_TRY_DATE = "last_try_date",
            COL_COMPLETE_DATE = "complete_date",
            COL_RESUMABLE = "resumable",
            COL_TURNOFF_MODE = "turnoff_mode",
            COL_PATH = "path";

    public static void createTable() {
        var sql = "CREATE TABLE IF NOT EXISTS " + DatabaseHelper.DOWNLOADS_TABLE_NAME + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_NAME + " VARCHAR,"
                + COL_PROGRESS + " REAL,"
                + COL_SIZE + " INTEGER,"
                + COL_DOWNLOADED + " INTEGER,"
                + COL_OPEN_AFTER_COMPLETE + " INTEGER,"
                + COL_SHOW_COMPLETE_DIALOG + " INTEGER,"
                + COL_RESUMABLE + " INTEGER,"
                + COL_TURNOFF_MODE + " VARCHAR,"
                + COL_URL + " VARCHAR,"
                + COL_PATH + " VARCHAR,"
                + COL_CHUNKS + " INTEGER,"
                + COL_ADD_DATE + " VARCHAR,"
                + COL_ADD_TO_QUEUE_DATE + " VARCHAR,"
                + COL_LAST_TRY_DATE + " VARCHAR,"
                + COL_COMPLETE_DATE + " VARCHAR"
                + ");";
        DatabaseHelper.runSQL(sql, false);
        alters();

    }


    private static void alters() {
        var addAlters = """
                ALTER TABLE %s ADD COLUMN %s VARCHAR DEFAULT "NOTHING";
                ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 1;
                ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 0;
                ALTER TABLE %s ADD COLUMN %s VARCHAR DEFAULT "%s";
                ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT 1;
                """
                .formatted(
                        DatabaseHelper.DOWNLOADS_TABLE_NAME, COL_TURNOFF_MODE,
                        DatabaseHelper.DOWNLOADS_TABLE_NAME, COL_SHOW_COMPLETE_DIALOG,
                        DatabaseHelper.DOWNLOADS_TABLE_NAME, COL_OPEN_AFTER_COMPLETE,
                        DatabaseHelper.DOWNLOADS_TABLE_NAME, COL_ADD_TO_QUEUE_DATE, LocalDateTime.now().toString(),
                        DatabaseHelper.DOWNLOADS_TABLE_NAME, COL_RESUMABLE);
        DatabaseHelper.runSQL(addAlters, true);
    }


    public static void insertDownload(DownloadModel dm) {
        String lastTryDate = "NULL";
        if (dm.getLastTryDate() != null)
            lastTryDate = "\"" + dm.getLastTryDate() + "\"";

        var showDialog = dm.isShowCompleteDialog() ? 1 : 0;
        var openFile = dm.isOpenAfterComplete() ? 1 : 0;
        var resumable = dm.isResumable() ? 1 : 0;

        var downloadSql = """
                INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                VALUES ("%s", %f, %d, %d, "%s", "%s", %d, "%s", "%s", "%s", %s, %d, %d, %d)
                """.formatted(
                DatabaseHelper.DOWNLOADS_TABLE_NAME,
                COL_NAME, COL_PROGRESS, COL_DOWNLOADED, COL_SIZE, COL_URL, COL_PATH, COL_CHUNKS, COL_ADD_DATE, COL_TURNOFF_MODE,
                COL_ADD_TO_QUEUE_DATE, COL_LAST_TRY_DATE, COL_SHOW_COMPLETE_DIALOG, COL_OPEN_AFTER_COMPLETE, COL_RESUMABLE,
                dm.getName(),
                dm.getProgress(),
                dm.getDownloaded(),
                dm.getSize(),
                dm.getUri(),
                dm.getFilePath(),
                dm.getChunks(),
                dm.getAddDate().toString(),
                dm.getTurnOffMode(),
                dm.getAddToQueueDate(),
                lastTryDate,
                showDialog,
                openFile,
                resumable);

        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement()) {
            stmt.executeUpdate(downloadSql);
            var genKeys = stmt.getGeneratedKeys();
            genKeys.next();
            dm.setId(genKeys.getInt(1));

            var queueDownloadSql = new StringBuilder("INSERT INTO ");
            queueDownloadSql.append(DatabaseHelper.QUEUE_DOWNLOAD_TABLE_NAME)
                    .append(" (").append(DatabaseHelper.COL_DOWNLOAD_ID).append(", ").append(DatabaseHelper.COL_QUEUE_ID).append(") VALUES");
            for (var q : dm.getQueues())
                queueDownloadSql.append("(").append(dm.getId()).append(",").append(q.getId()).append("),");
            queueDownloadSql.deleteCharAt(queueDownloadSql.length() - 1);
            stmt.executeUpdate(queueDownloadSql.toString());
        } catch (SQLException e) {
            log.error(e.getLocalizedMessage());
        }
    }


    public static List<DownloadModel> getDownloadsByQueueName(String queueName) {
        var sql = """
                SELECT d.*,
                       qd.%s,
                       q.%s as %s, q.*,
                       sc.%s,sc.%s,sc.%s,sc.%s,sc.%s,sc.%s,sc.%s,sc.%s,sc.%s
                FROM %s d
                         INNER JOIN %s qd ON d.%s = qd.%s
                         INNER JOIN %s q ON q.%s = qd.%s
                         LEFT JOIN %s sc on q.%s = sc.%s
                WHERE q.%s = "%s"
                """
                .formatted(DatabaseHelper.COL_QUEUE_ID,
                        COL_NAME, DatabaseHelper.COL_QUEUE_NAME,
                        ScheduleRepo.COL_ENABLED, ScheduleRepo.COL_DAYS, ScheduleRepo.COL_ONCE_DOWNLOAD, ScheduleRepo.COL_START_TIME, ScheduleRepo.COL_START_DATE,
                        ScheduleRepo.COL_STOP_TIME_ENABLED, ScheduleRepo.COL_STOP_TIME, ScheduleRepo.COL_TURN_OFF_MODE_ENABLED, ScheduleRepo.COL_TURN_OFF_MODE,
                        DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        DatabaseHelper.QUEUE_DOWNLOAD_TABLE_NAME, COL_ID, DatabaseHelper.COL_DOWNLOAD_ID,
                        DatabaseHelper.QUEUES_TABLE_NAME, COL_ID, DatabaseHelper.COL_QUEUE_ID,
                        DatabaseHelper.SCHEDULE_TABLE_NAME, QueuesRepo.COL_SCHEDULE_ID, COL_ID,
                        COL_NAME, queueName);
        return fetchDownloads(sql, true);
    }

    private static List<DownloadModel> fetchDownloads(String sql, boolean fetchQueue) {
        var list = new ArrayList<DownloadModel>();
        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next())
                list.add(createDownload(rs, fetchQueue));
            return list;
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return list;
    }

    public static List<DownloadModel> findByURL(String url) {
        var sql = """
                SELECT * FROM %s WHERE %s LIKE "%s%%";
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME, COL_URL, url);
        return fetchDownloads(sql, false);
    }

    public static List<DownloadModel> findById(int id) {
        var sql = """
                SELECT * FROM %s WHERE %s="%s";
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME, COL_ID, id);
        return fetchDownloads(sql, false);
    }

    public static void updateDownloadQueue(int download_id, int queue_id, String addToQueue) {
        var colQueueCount = "queue_count";
        var queueCountSql = """
                SELECT count(*) AS %s FROM %s d
                         INNER JOIN %s qd ON d.%s = qd.%s
                         WHERE qd.%s = %d;
                """
                .formatted(colQueueCount, DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        DatabaseHelper.QUEUE_DOWNLOAD_TABLE_NAME,
                        COL_ID, DatabaseHelper.COL_DOWNLOAD_ID,
                        DatabaseHelper.COL_DOWNLOAD_ID, download_id);
        var insertQueueDownloadSql = """
                INSERT INTO %s (%s, %s) VALUES (%d, %d);
                """
                .formatted(DatabaseHelper.QUEUE_DOWNLOAD_TABLE_NAME,
                        DatabaseHelper.COL_DOWNLOAD_ID,
                        DatabaseHelper.COL_QUEUE_ID,
                        download_id,
                        queue_id);
        var idOfNotDefaultQueueSql = """
                SELECT %s FROM %s qd INNER JOIN %s q ON q.%s = qd.%s
                WHERE q.name != 'All Downloads'
                  AND q.name != 'Compressed'
                  AND q.name != 'Programs'
                  AND q.name != 'Videos'
                  AND q.name != 'Music'
                  AND q.name != 'Docs'
                  AND q.name != 'Others'
                  AND qd.%s = %d;
                """
                .formatted(DatabaseHelper.COL_QUEUE_ID,
                        DatabaseHelper.QUEUE_DOWNLOAD_TABLE_NAME,
                        DatabaseHelper.QUEUES_TABLE_NAME,
                        COL_ID,
                        DatabaseHelper.COL_QUEUE_ID,
                        DatabaseHelper.COL_DOWNLOAD_ID,
                        download_id);
        var updateAddToQueueDateSql = """
                UPDATE %s SET %s = "%s" WHERE %s = %d;
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        COL_ADD_TO_QUEUE_DATE, addToQueue,
                        COL_ID, download_id);

        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement()) {
            var countRS = stmt.executeQuery(queueCountSql);
            countRS.next();
            var queueCount = countRS.getInt(colQueueCount);
            if (queueCount == 3) {
                var prevQueueRS = stmt.executeQuery(idOfNotDefaultQueueSql);
                prevQueueRS.next();
                var prevQueueId = prevQueueRS.getInt(DatabaseHelper.COL_QUEUE_ID);
                var updateQueueDownloadSql = "UPDATE %s SET %s = %d WHERE %s = %d AND %s = %d;"
                        .formatted(DatabaseHelper.QUEUE_DOWNLOAD_TABLE_NAME,
                                DatabaseHelper.COL_QUEUE_ID, queue_id,
                                DatabaseHelper.COL_QUEUE_ID, prevQueueId,
                                DatabaseHelper.COL_DOWNLOAD_ID, download_id);
                stmt.executeUpdate(updateQueueDownloadSql);
            } else if (queueCount == 2)
                stmt.executeUpdate(insertQueueDownloadSql);
            else throw new Exception("queue count for the download is not correct");
            stmt.executeUpdate(updateAddToQueueDateSql);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static void deleteDownload(DownloadModel download) {
        var sql = """
                DELETE FROM %s WHERE %s=%d;
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME, COL_ID, download.getId());
        DatabaseHelper.runSQL(sql, false);
    }

    public static void deleteDownloadQueue(int downloadId, int queueId) {
        var sql = """
                DELETE FROM %s WHERE %s = %d AND %s = %d;
                """
                .formatted(DatabaseHelper.QUEUE_DOWNLOAD_TABLE_NAME, DatabaseHelper.COL_DOWNLOAD_ID, downloadId, DatabaseHelper.COL_QUEUE_ID, queueId);
        DatabaseHelper.runSQL(sql, false);
    }

    private static DownloadModel createDownload(ResultSet rs, boolean fetchQueue) throws SQLException {
        var id = rs.getInt(COL_ID);
        var name = rs.getString(COL_NAME);
        var progress = rs.getFloat(COL_PROGRESS);
        var downloaded = rs.getLong(COL_DOWNLOADED);
        var size = rs.getLong(COL_SIZE);
        var url = rs.getString(COL_URL);
        var filePath = rs.getString(COL_PATH);
        var chunks = rs.getInt(COL_CHUNKS);
        var showCompleteDialog = rs.getBoolean(COL_SHOW_COMPLETE_DIALOG);
        var openAfterComplete = rs.getBoolean(COL_OPEN_AFTER_COMPLETE);
        var resumable = rs.getBoolean(COL_RESUMABLE);
        var turnOffMode = TurnOffMode.valueOf(rs.getString(COL_TURNOFF_MODE));
        var addDate = rs.getString(COL_ADD_DATE);
        var addDateStr = LocalDateTime.parse(addDate);
        var addToQueueDate = rs.getString(COL_ADD_TO_QUEUE_DATE);
        var addToQueueDateStr = LocalDateTime.parse(addToQueueDate);
        var lastTryDate = rs.getString(COL_LAST_TRY_DATE);
        var lastTryDateStr = lastTryDate == null ? null : LocalDateTime.parse(lastTryDate);
        var completeDate = rs.getString(COL_COMPLETE_DATE);
        var completeDateStr = completeDate == null ? null : LocalDateTime.parse(completeDate);
        var downloadStatus = progress != 100 ? DownloadStatus.Paused : DownloadStatus.Completed;

        var build = DownloadModel.builder()
                .id(id).name(name).progress(progress).downloaded(downloaded).size(size).uri(url).filePath(filePath)
                .chunks(chunks).addDate(addDateStr).addToQueueDate(addToQueueDateStr).turnOffMode(turnOffMode)
                .lastTryDate(lastTryDateStr).completeDate(completeDateStr).openAfterComplete(openAfterComplete)
                .showCompleteDialog(showCompleteDialog).downloadStatus(downloadStatus).resumable(resumable)
                .build();

        if (fetchQueue) {
            var queueId = rs.getInt(DatabaseHelper.COL_QUEUE_ID);
            var queueName = rs.getString(DatabaseHelper.COL_QUEUE_NAME);
            var scheduleId = rs.getInt(QueuesRepo.COL_SCHEDULE_ID);
            var schedule = ScheduleRepo.createScheduleModel(rs, scheduleId);
            var queue = QueuesRepo.createQueueModel(rs, queueId, queueName, schedule);
            var queues = new CopyOnWriteArrayList<>(Collections.singletonList(queue));
            build.setQueues(queues);
        }
        return build;
    }


    public static void updateDownloadProgress(DownloadModel dm) {
        var sql = """
                UPDATE %s SET %s = %f, %s = %d WHERE %s = %d;
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        COL_PROGRESS, dm.getProgress(),
                        COL_DOWNLOADED, dm.getDownloaded(),
                        COL_ID, dm.getId());
        DatabaseHelper.runSQL(sql, false);
    }

    public static void updateDownloadCompleteDate(DownloadModel dm) {
        var sql = """
                UPDATE %s SET %s = "%s" WHERE %s = %d;
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        COL_COMPLETE_DATE, dm.getCompleteDate(),
                        COL_ID, dm.getId());
        DatabaseHelper.runSQL(sql, false);
    }

    public static void updateDownloadLastTryDate(DownloadModel dm) {
        var sql = """
                UPDATE %s SET %s = "%s" WHERE %s = %d;
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        COL_LAST_TRY_DATE, dm.getLastTryDate(),
                        COL_ID, dm.getId());
        DatabaseHelper.runSQL(sql, false);
    }

    public static void updateDownloadOpenAfterComplete(DownloadModel dm) {
        var sql = """
                UPDATE %s SET %s = %d WHERE %s = %d;
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        COL_OPEN_AFTER_COMPLETE, dm.isOpenAfterComplete() ? 1 : 0,
                        COL_ID, dm.getId());
        DatabaseHelper.runSQL(sql, false);
    }

    public static void updateDownloadShowCompleteDialog(DownloadModel dm) {
        var sql = """
                UPDATE %s SET %s = %d WHERE %s = %d;
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        COL_SHOW_COMPLETE_DIALOG, dm.isShowCompleteDialog() ? 1 : 0,
                        COL_ID, dm.getId());
        DatabaseHelper.runSQL(sql, false);
    }

    public static void updateTableStatus(DownloadModel dm) {
        var lastTryDate = "NULL";
        if (dm.getLastTryDate() != null)
            lastTryDate = "\"" + dm.getLastTryDate() + "\"";
        var completeDate = "NULL";
        if (dm.getCompleteDate() != null)
            completeDate = "\"" + dm.getCompleteDate() + "\"";
        var sql = """
                UPDATE %s SET %s = %f, %s = %d, %s = %s,
                    %s = %s WHERE %s = %d
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        COL_PROGRESS, dm.getProgress(),
                        COL_DOWNLOADED, dm.getDownloaded(),
                        COL_COMPLETE_DATE, completeDate,
                        COL_LAST_TRY_DATE, lastTryDate,
                        COL_ID, dm.getId());
        DatabaseHelper.runSQL(sql, false);
    }

    public static void insertDownloads(List<DownloadModel> dms) {
        dms.forEach(DownloadsRepo::insertDownload);
    }

    public static void updateDownloadProperty(String column, String value, int downloadId) {
        var sql = """
                UPDATE %s SET %s = %s WHERE %s = %s;
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        column, value,
                        COL_ID, downloadId);
        DatabaseHelper.runSQL(sql, false);
    }

    public static void updateDownloadLocation(String downloadPath, int id) {
        var sql = """
                UPDATE %s SET %s="%s" || %s WHERE %s = %d;
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        COL_PATH, downloadPath, COL_NAME,
                        COL_ID, id);
        DatabaseHelper.runSQL(sql, false);
    }

    public static int getNextNumberOfExistedDownload(String path) {
        var sql = """
                SELECT COUNT(*) AS count FROM %s WHERE %s LIKE "%s%%";
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME,
                        COL_PATH, path);
        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement()) {
            var resultSet = stmt.executeQuery(sql);
            return resultSet.getInt("count");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<DownloadModel> searchLike(String value, QueueModel selectedQueue) {
        var tokens = value.split("\\s+");

        var whereClause = new StringBuilder();
        for (var token : tokens) {
            if (whereClause.length() > 0)
                whereClause.append(" AND ");
            whereClause.append(COL_NAME).append(" LIKE '%").append(token).append("%'");
        }
        if (!selectedQueue.getName().equals(Defaults.ALL_DOWNLOADS_QUEUE))
            whereClause.append(" AND id IN (")
                    .append("SELECT download_id FROM queue_download WHERE queue_id=")
                    .append(selectedQueue.getId()).append(")");
        var sql = """
                SELECT * FROM %s WHERE %s;
                """
                .formatted(DatabaseHelper.DOWNLOADS_TABLE_NAME, whereClause);
        var list = new LinkedList<DownloadModel>();
        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                var download = createDownload(rs, false);
                download.getQueues().add(selectedQueue);
                list.add(download);
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return list;
    }

}
