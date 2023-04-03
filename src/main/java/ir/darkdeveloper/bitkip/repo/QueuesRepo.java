package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.models.DownloadModel;
import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.utils.FileExtensions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static ir.darkdeveloper.bitkip.repo.DatabaseHelper.*;

public class QueuesRepo {

    private static final String COL_ID = "id",
            COL_NAME = "name",
            COL_EDITABLE = "editable",
            COL_CAN_ADD_DOWN = "can_add_download";
    static final String COL_SCHEDULE_ID = "schedule_id";


    public static void createTableAndDefaultRecords() {
        createQueuesTable();
        createQueueDownloadTable();
        FileExtensions.staticQueueNames.forEach(name -> {
            var queue = new QueueModel(name, false, false);
            if (name.equals("All Downloads"))
                queue.setCanAddDownload(true);
            insertQueue(queue);
        });
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
                    FOREIGN KEY (%s) REFERENCES %s(%s)
                );
                """
                .formatted(QUEUES_TABLE_NAME,
                        COL_ID,
                        COL_NAME,
                        COL_EDITABLE,
                        COL_CAN_ADD_DOWN,
                        COL_SCHEDULE_ID,
                        COL_SCHEDULE_ID, SCHEDULE_TABLE_NAME, COL_ID);
        DatabaseHelper.createTable(sql);
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
        DatabaseHelper.createTable(sql);
    }


    public static void insertQueue(QueueModel queue) {
        var sql = """
                INSERT OR IGNORE INTO %s (%s,%s,%s) VALUES("%s",%d,%d);
                """
                .formatted(QUEUES_TABLE_NAME, COL_NAME, COL_EDITABLE, COL_CAN_ADD_DOWN,
                        queue.getName(), queue.isEditable() ? 1 : 0, queue.isCanAddDownload() ? 1 : 0);
        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
            var genKeys = stmt.getGeneratedKeys();
            genKeys.next();
            queue.setId(genKeys.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
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
                return createQueueModel(rs, fetchDownloads);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Queue does not exist");
    }


    public static List<QueueModel> getQueues(boolean fetchDownloads) {
        var list = new ArrayList<QueueModel>();
        var sql = "SELECT * FROM " + QUEUES_TABLE_NAME + ";";
        try (var con = DatabaseHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next())
                list.add(createQueueModel(rs, fetchDownloads));
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    public static void deleteQueue(String name) {
        var sql = """
                DELETE FROM %s WHERE %s = "%s";
                """
                .formatted(QUEUES_TABLE_NAME, COL_NAME, name);
        DatabaseHelper.executeUpdateSql(sql);
    }

    private static QueueModel createQueueModel(ResultSet rs, boolean fetchDownloads) throws SQLException {
        var id = rs.getInt(COL_ID);
        var name = rs.getString(COL_NAME);
        var editable = rs.getBoolean(COL_EDITABLE);
        var canAddDownload = rs.getBoolean(COL_CAN_ADD_DOWN);
        CopyOnWriteArrayList<DownloadModel> downloads = null;
        if (fetchDownloads)
            downloads = new CopyOnWriteArrayList<>(DownloadsRepo.getDownloadsByQueueName(name));
        // TODO fetch schedule
        return new QueueModel(id, name, editable, canAddDownload, null, downloads);
    }
}
