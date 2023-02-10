package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.models.QueueModel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static ir.darkdeveloper.bitkip.repo.DatabaseHelper.*;

public class QueuesRepo {
    private static final DatabaseHelper dbHelper = new DatabaseHelper();

    private static final QueueModel allDownloadQueue = new QueueModel("All Downloads", false, true);
    private static final QueueModel compressedQueue = new QueueModel("Compressed", false, false);
    private static final QueueModel programsQueue = new QueueModel("Programs", false, false);
    private static final QueueModel videosQueue = new QueueModel("Videos", false, false);
    private static final QueueModel musicQueue = new QueueModel("Music", false, false);
    private static final QueueModel docsQueue = new QueueModel("Docs", false, false);
    private static final QueueModel othersQueue = new QueueModel("Others", false, false);

    public static void createTableAndDefaultRecords() {
        dbHelper.createQueuesTable();
        dbHelper.createQueueDownloadTable();
        insertDefaultQueues(allDownloadQueue);
        insertDefaultQueues(compressedQueue);
        insertDefaultQueues(programsQueue);
        insertDefaultQueues(videosQueue);
        insertDefaultQueues(musicQueue);
        insertDefaultQueues(docsQueue);
        insertDefaultQueues(othersQueue);
    }

    public static void insertQueue(QueueModel queue) {
        var sql = "INSERT INTO " + QUEUES_TABLE_NAME + " (" +
                COL_NAME +
                ") VALUES(\"" +
                queue.getName() +
                "\");";
        dbHelper.insertQueue(sql, queue);
    }

    private static void insertDefaultQueues(QueueModel queue) {
        var sql = "INSERT OR IGNORE INTO " + QUEUES_TABLE_NAME + " (" +
                COL_NAME + "," +
                COL_EDITABLE + "," +
                COL_CAN_ADD_DOWN +
                ") VALUES(\"" +
                queue.getName() + "\"," +
                queue.isEditable() + "," +
                queue.isCanAddDownload() +
                ");";
        dbHelper.insertQueue(sql, queue);
    }

    public static QueueModel findByName(String name) {
        var sql = "SELECT * FROM queues WHERE name=\"" + name + "\";";
        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return createQueueModel(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Queue does not exist");
    }


    public static List<QueueModel> getQueues() {
        var list = new ArrayList<QueueModel>();
        var sql = "SELECT * FROM " + QUEUES_TABLE_NAME + ";";
        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next())
                list.add(createQueueModel(rs));
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    public static void deleteQueue(int id) {
        var sql = "DELETE FROM " + QUEUES_TABLE_NAME + " WHERE " + COL_ID + "=" + id + ";";
        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static QueueModel createQueueModel(ResultSet rs) throws SQLException {
        var id = rs.getInt(COL_ID);
        var name = rs.getString(COL_NAME);
        var editable = rs.getBoolean(COL_EDITABLE);
        var canAddDownload = rs.getBoolean(COL_CAN_ADD_DOWN);
        return new QueueModel(id, name, editable, canAddDownload);
    }
}
