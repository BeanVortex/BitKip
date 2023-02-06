package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.models.DownloadModel;

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

    public static void insertDownload(DownloadModel download) {
        var sql = "INSERT INTO " + DOWNLOADS_TABLE_NAME + " (" +
                COL_NAME + "," +
                COL_PROGRESS + "," +
                COL_SIZE + "," +
                COL_URL + "," +
                COL_PATH + "," +
                COL_CHUNKS + "," +
                COL_QUEUE + "," +
                COL_ADD_DATE + "," +
                COL_LAST_TRY_DATE + "," +
                COL_COMPLETE_DATE + ")" +
                " VALUES(\"" +
                download.getName() + "\"," +
                download.getProgress() + "," +
                download.getSize() + ",\"" +
                download.getUrl() + "\",\"" +
                download.getFilePath() + "\"," +
                download.getChunks() + ",\"" +
                download.getQueue() + "\",\"" +
                download.getAddDate().toString() + "\",\"" +
                download.getLastTryDate().toString() + "\",\"" +
                download.getCompleteDate().toString() + "\"" +
                ");";
        dbHelper.insert(sql, download);
    }

    public static DownloadModel findById(int id) {
        var sql = "SELECT * FROM " + DOWNLOADS_TABLE_NAME + " WHERE " + COL_ID + "=" + id + ";";
        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement();
             var rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return createDownload(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<DownloadModel> getDownloads() {
        var sql = "SELECT * FROM " + DOWNLOADS_TABLE_NAME + ";";
        return fetchDownloads(sql);
    }

    public static List<DownloadModel> getDownloadsByQueue(String queue) {
        var sql = "SELECT * FROM " + DOWNLOADS_TABLE_NAME + " WHERE " + COL_QUEUE + "=" + queue + ";";
        return fetchDownloads(sql);
    }

    private static ArrayList<DownloadModel> fetchDownloads(String sql) {
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

    public static void updateDownloadQueue(int id, String queue) {
        var sql = "UPDATE " + DOWNLOADS_TABLE_NAME + " SET " + COL_QUEUE + "=\"" + queue + "\""
                + " WHERE " + COL_ID + "=" + id + ";";
        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteDownload(DownloadModel download) {
        var sql = "DELETE FROM " + DOWNLOADS_TABLE_NAME + " WHERE " + COL_ID + "=" + download.getId() + ";";
        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static DownloadModel createDownload(ResultSet rs) throws SQLException {
        var id = rs.getInt(COL_ID);
        var name = rs.getString(COL_NAME);
        var progress = rs.getInt(COL_PROGRESS);
        var size = rs.getInt(COL_SIZE);
        var url = rs.getString(COL_URL);
        var filePath = rs.getString(COL_PATH);
        var chunks = rs.getInt(COL_CHUNKS);
        var queue = rs.getString(COL_QUEUE);
        var addDate = rs.getString(COL_ADD_DATE);
        var lastTryDate = rs.getString(COL_LAST_TRY_DATE);
        var completeDate = rs.getString(COL_COMPLETE_DATE);
        var dow = DownloadModel.builder()
                .id(id).name(name).progress(progress).size(size).url(url).filePath(filePath)
                .chunks(chunks).queue(queue).remainingTime(0).addDate(LocalDateTime.parse(addDate))
                .lastTryDate(LocalDateTime.parse(lastTryDate)).completeDate(LocalDateTime.parse(completeDate))
                .build();
        dow.fillProperties();
        return dow;
    }

    public static void updateDownloadSize(DownloadModel download) {
        var sql = "UPDATE " + DOWNLOADS_TABLE_NAME + " SET " + COL_SIZE + "=\"" + download.getSize() + "\""
                + " WHERE " + COL_ID + "=" + download.getId() + ";";
        try (var con = dbHelper.openConnection();
             var stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
