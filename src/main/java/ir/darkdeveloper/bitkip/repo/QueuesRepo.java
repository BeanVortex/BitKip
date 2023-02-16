package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.models.QueueModel;
import ir.darkdeveloper.bitkip.utils.FileExtensions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static ir.darkdeveloper.bitkip.repo.DatabaseHelper.*;

public class QueuesRepo {
    private static final DatabaseHelper dbHelper = new DatabaseHelper();

    public static void createTableAndDefaultRecords() {
        dbHelper.createQueuesTable();
        dbHelper.createQueueDownloadTable();
        FileExtensions.staticQueueNames.forEach(name -> {
            var queue = new QueueModel(name, false, false);
            if (name.equals("All Downloads"))
                queue.setCanAddDownload(true);
            insertQueue(queue);
        });
    }

    public static void insertQueue(QueueModel queue) {
        var sql = """
                INSERT OR IGNORE INTO queues (name,editable,can_add_download) VALUES("%s",%d,%d);
                """.formatted(queue.getName(), queue.isEditable() ? 1 : 0, queue.isCanAddDownload() ? 1 : 0);
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

    public static void deleteQueue(String name) {
        var sql = """
                DELETE FROM queues WHERE name = "%s";
                """.formatted(name);
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
