package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.DownloadStatus;
import ir.darkdeveloper.bitkip.models.QueueModel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ir.darkdeveloper.bitkip.repo.DatabaseHelper.*;

public class DownloadsRepo {
    private static final DatabaseHelper dbHelper = new DatabaseHelper();

    public static void createTable() {
        dbHelper.createDownloadsTable();
    }

    public static void insertDownload(DownloadModel dm) {
        String lastTryDate = "NULL";
        if (dm.getLastTryDate() != null)
            lastTryDate = "\"" + dm.getLastTryDate() + "\"";

        var showDialog = dm.isShowCompleteDialog() ? 1 : 0;
        var openFile = dm.isOpenAfterComplete() ? 1 : 0;

        var downloadSql = """
                INSERT INTO downloads (name, progress, downloaded, size, url, path, chunks, add_date,
                 add_to_queue_date, last_try_date, show_complete_dialog, open_after_complete)
                VALUES ("%s", %f, %d, %d, "%s", "%s", %d, "%s", "%s", %s, %d, %d)
                """.formatted(
                dm.getName(),
                dm.getProgress(),
                dm.getDownloaded(),
                dm.getSize(),
                dm.getUrl(),
                dm.getFilePath(),
                dm.getChunks(),
                dm.getAddDate().toString(),
                dm.getAddToQueueDate(),
                lastTryDate,
                showDialog,
                openFile);

        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement()) {
            dbHelper.insertDownload(downloadSql, dm, stmt);
            dm.getQueue().forEach(queue -> {
                var queueDownloadSql = """
                        INSERT INTO queue_download (download_id, queue_id)
                        VALUES (%d, %d);
                        """.formatted(dm.getId(), queue.getId());
                try {
                    stmt.executeUpdate(queueDownloadSql);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<DownloadModel> getDownloads() {
        var sql = """
                SELECT *, q.name as queue_name
                FROM downloads d
                         INNER JOIN queue_download qd ON d.id = qd.download_id
                         INNER JOIN queues q on q.id = qd.queue_id;
                                """;
        return fetchDownloads(sql);
    }

    public static List<DownloadModel> getDownloadsByQueue(int queueId) {
        var sql = """
                SELECT *, q.name as queue_name
                FROM downloads d
                         INNER JOIN queue_download qd ON d.id = qd.download_id
                         INNER JOIN queues q ON q.id = qd.queue_id
                WHERE qd.queue_id = %d;
                """.formatted(queueId);
        return fetchDownloads(sql);
    }

    private static List<DownloadModel> fetchDownloads(String sql) {
        var list = new ArrayList<DownloadModel>();
        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next())
                list.add(createDownload(rs));
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void updateDownloadQueue(int download_id, int queue_id) {
        var queueCountSql = """
                SELECT count(*) AS queue_count
                FROM downloads
                         INNER JOIN queue_download qd ON downloads.id = qd.download_id
                         WHERE qd.download_id = %d;
                """.formatted(download_id);
        var insertQueueDownloadSql = """
                INSERT INTO queue_download (download_id, queue_id) VALUES (%d,%d);
                """.formatted(download_id, queue_id);
        var idOfNotDefaultQueueSql = """
                SELECT queue_id
                FROM queue_download qd
                         INNER JOIN queues q ON q.id = qd.queue_id
                WHERE q.name != 'All Downloads'
                  AND q.name != 'Compressed'
                  AND q.name != 'Programs'
                  AND q.name != 'Videos'
                  AND q.name != 'Music'
                  AND q.name != 'Docs'
                  AND q.name != 'Others'
                  AND qd.download_id = %d;
                """.formatted(download_id);
        var updateQueueSql = """
                UPDATE queue_download
                SET queue_id = %d
                WHERE queue_id = %d AND download_id = %d;
                """;
        var updateAddToQueueDateSql = """
                UPDATE downloads
                SET add_to_queue_date = "%s"
                WHERE id = %d;
                """.formatted(LocalDateTime.now(), download_id);

        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement()) {
            var countRS = stmt.executeQuery(queueCountSql);
            countRS.next();
            var queueCount = countRS.getInt("queue_count");
            if (queueCount == 3) {
                var prevQueueRS = stmt.executeQuery(idOfNotDefaultQueueSql);
                prevQueueRS.next();
                var prevQueueId = prevQueueRS.getInt("queue_id");
                stmt.executeUpdate(updateQueueSql.formatted(queue_id, prevQueueId, download_id));
            } else if (queueCount == 2)
                stmt.executeUpdate(insertQueueDownloadSql);
            else throw new Exception("queue count for the download is not correct");
            stmt.executeUpdate(updateAddToQueueDateSql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteDownload(DownloadModel download) {
        var sql = "DELETE FROM " + DOWNLOADS_TABLE_NAME + " WHERE " + COL_ID + "=" + download.getId() + ";";
        executeSql(sql);
    }

    public static void deleteDownloadQueue(int downloadId, int queueId) {
        var sql = """
                DELETE FROM queue_download
                WHERE download_id = %d
                    AND queue_id = %d;
                """.formatted(downloadId, queueId);
        executeSql(sql);
    }

    private static DownloadModel createDownload(ResultSet rs) throws SQLException {
        var id = rs.getInt(COL_ID);
        var name = rs.getString(COL_NAME);
        var progress = rs.getFloat(COL_PROGRESS);
        var downloaded = rs.getLong("downloaded");
        var size = rs.getLong(COL_SIZE);
        var url = rs.getString(COL_URL);
        var filePath = rs.getString(COL_PATH);
        var chunks = rs.getInt(COL_CHUNKS);
        var queueId = rs.getInt("queue_id");
        var showCompleteDialog = rs.getBoolean("show_complete_dialog");
        var openAfterComplete = rs.getBoolean("open_after_complete");
        var queueName = rs.getString("queue_name");
        var queueEditable = rs.getBoolean(COL_EDITABLE);
        var queueCanAddDown = rs.getBoolean(COL_CAN_ADD_DOWN);
        var queue = new QueueModel(queueId, queueName, queueEditable, queueCanAddDown);
        var addDate = rs.getString(COL_ADD_DATE);
        var addDateStr = LocalDateTime.parse(addDate);
        var addToQueueDate = rs.getString("add_to_queue_date");
        var addToQueueDateStr = LocalDateTime.parse(addToQueueDate);
        var lastTryDate = rs.getString(COL_LAST_TRY_DATE);
        var lastTryDateStr = lastTryDate == null ? null : LocalDateTime.parse(lastTryDate);
        var completeDate = rs.getString(COL_COMPLETE_DATE);
        var completeDateStr = completeDate == null ? null : LocalDateTime.parse(completeDate);
        var downloadStatus = progress != 100 ? DownloadStatus.Paused : DownloadStatus.Completed;
        return DownloadModel.builder()
                .id(id).name(name).progress(progress).downloaded(downloaded).size(size).url(url).filePath(filePath)
                .chunks(chunks).queue(new ArrayList<>(List.of(queue))).addDate(addDateStr).addToQueueDate(addToQueueDateStr)
                .lastTryDate(lastTryDateStr).completeDate(completeDateStr).openAfterComplete(openAfterComplete)
                .showCompleteDialog(showCompleteDialog).downloadStatus(downloadStatus)
                .build();
    }


    public static void updateDownloadProgress(DownloadModel dm) {
        var sql = """
                UPDATE downloads SET progress = %f, downloaded = %d WHERE id = %d;
                """.formatted(dm.getProgress(), dm.getDownloaded(), dm.getId());
        executeSql(sql);
    }

    public static void updateDownloadCompleteDate(DownloadModel dm) {
        var sql = """
                UPDATE downloads SET complete_date = "%s" WHERE id = %d;
                """.formatted(dm.getCompleteDate(), dm.getId());
        executeSql(sql);
    }

    public static void updateDownloadLastTryDate(DownloadModel dm) {
        var sql = """
                UPDATE downloads SET last_try_date = "%s" WHERE id = %d;
                """.formatted(dm.getLastTryDate(), dm.getId());
        executeSql(sql);
    }

    public static void updateDownloadOpenAfterComplete(DownloadModel dm) {
        var sql = """
                UPDATE downloads SET open_after_complete = %d WHERE id = %d;
                """.formatted(dm.isOpenAfterComplete() ? 1 : 0, dm.getId());
        executeSql(sql);
    }

    public static void updateDownloadShowCompleteDialog(DownloadModel dm) {
        var sql = """
                UPDATE downloads SET show_complete_dialog = %d WHERE id = %d;
                """.formatted(dm.isShowCompleteDialog() ? 1 : 0, dm.getId());
        executeSql(sql);
    }

    public static void updateTableStatus(DownloadModel dm) {
        var lastTryDate = "NULL";
        if (dm.getLastTryDate() != null)
            lastTryDate = "\"" + dm.getLastTryDate() + "\"";
        var completeDate = "NULL";
        if (dm.getCompleteDate() != null)
            completeDate = "\"" + dm.getCompleteDate() + "\"";
        var sql = """
                UPDATE downloads SET progress = %f, downloaded = %d, complete_date = %s,
                    last_try_date = %s WHERE id = %d
                """.formatted(dm.getProgress(), dm.getDownloaded(),
                completeDate, lastTryDate, dm.getId());
        executeSql(sql);
    }

    private static void executeSql(String sql) {
        try (var conn = dbHelper.openConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertDownloads(List<DownloadModel> dms) {
        dms.forEach(DownloadsRepo::insertDownload);
    }
}
