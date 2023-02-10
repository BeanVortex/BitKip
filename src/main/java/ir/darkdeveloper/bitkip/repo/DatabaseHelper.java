package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.models.Model;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseHelper {


    static final String DOWNLOADS_TABLE_NAME = "downloads";
    static final String QUEUES_TABLE_NAME = "queues";

    static final String COL_ID = "id";
    static final String COL_NAME = "name";
    static final String COL_PROGRESS = "progress";
    static final String COL_SIZE = "size";
    static final String COL_URL = "url";
    static final String COL_PATH = "path";
    static final String COL_CHUNKS = "chunks";
    static final String COL_ADD_DATE = "add_Date";
    static final String COL_LAST_TRY_DATE = "last_try_date";
    static final String COL_COMPLETE_DATE = "complete_date";
    static final String COL_EDITABLE = "editable";
    static final String COL_CAN_ADD_DOWN = "can_add_download";

    private static final Logger log = Logger.getLogger(DatabaseHelper.class.getName());

    Connection openConnection() throws SQLException {
        var path = AppConfigs.dataPath + File.separator + "bitkip.db";
        var conn = DriverManager.getConnection("jdbc:sqlite:" + path);
        conn.createStatement().execute("PRAGMA foreign_keys=ON;");
        return conn;
    }

    void createDownloadsTable() {
        var sql = "CREATE TABLE IF NOT EXISTS " + DOWNLOADS_TABLE_NAME + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_NAME + " VARCHAR,"
                + COL_PROGRESS + " REAL,"
                + COL_SIZE + " INTEGER,"
                + COL_URL + " VARCHAR,"
                + COL_PATH + " VARCHAR,"
                + COL_CHUNKS + " INTEGER,"
                + COL_ADD_DATE + " VARCHAR,"
                + COL_LAST_TRY_DATE + " VARCHAR,"
                + COL_COMPLETE_DATE + " VARCHAR"
                + ");";
        createTable(sql);
    }

    void createQueuesTable() {
        var sql = "CREATE TABLE IF NOT EXISTS " + QUEUES_TABLE_NAME + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_NAME + " VARCHAR UNIQUE,"
                + COL_EDITABLE + " INTEGER,"
                + COL_CAN_ADD_DOWN + " INTEGER" +
                ");";
        createTable(sql);
    }

    void createQueueDownloadTable() {
        var sql = """
                CREATE TABLE IF NOT EXISTS queue_download
                (
                    download_id INTEGER,
                    queue_id    INTEGER,
                    FOREIGN KEY (download_id) REFERENCES downloads(id) ON DELETE CASCADE,
                    FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCADE
                );
                """;
        createTable(sql);
    }

    private void createTable(String sql) {
        try {
            var con = openConnection();
            var stmt = con.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();
            con.close();
            log.info("created db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insert(String sql, Model queue) {
        try (var con = openConnection();
             var stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
            var genKeys = stmt.getGeneratedKeys();
            genKeys.next();
            queue.setId(genKeys.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
