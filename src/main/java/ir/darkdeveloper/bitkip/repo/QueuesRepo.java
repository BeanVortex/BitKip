package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.models.QueueModel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static ir.darkdeveloper.bitkip.repo.DatabaseHelper.*;

public class QueuesRepo {
    private static final DatabaseHelper dbHelper = new DatabaseHelper();

    public static void createTable() {
        dbHelper.createQueuesTable();
    }

    public static void insertQueue(QueueModel queue) {
        var sql = "INSERT INTO " + QUEUES_TABLE_NAME + " (" +
                COL_NAME +
                ") VALUES(\"" +
                queue.getName() +
                "\");";
        dbHelper.insert(sql, queue);
    }

    public static QueueModel findById(int id) {
        var sql = "SELECT * FROM " + QUEUES_TABLE_NAME + " WHERE " + COL_ID + "=" + id + ";";
        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return createQueueModel(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static List<QueueModel> getQueues() {
        var sql = "SELECT * FROM " + QUEUES_TABLE_NAME + ";";
        var list = new ArrayList<QueueModel>();
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
        return new QueueModel(id, name);
    }
}
