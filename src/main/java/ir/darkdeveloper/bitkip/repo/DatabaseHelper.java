package ir.darkdeveloper.bitkip.repo;

import ir.darkdeveloper.bitkip.config.AppConfigs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.repo.DownloadsRepo.COL_ID;

public class DatabaseHelper {


    static final String QUEUE_DOWNLOAD_TABLE_NAME = "queue_download";
    public static final String DOWNLOADS_TABLE_NAME = "downloads";
    public static final String QUEUES_TABLE_NAME = "queues";
    static final String SCHEDULE_TABLE_NAME = "schedules";

    static String COL_DOWNLOAD_ID = "download_id",
            COL_QUEUE_ID = "queue_id", COL_QUEUE_NAME = "queue_name";


    static Connection openConnection() throws SQLException {
        var path = AppConfigs.dataPath + "bitkip.db";
        var conn = DriverManager.getConnection("jdbc:sqlite:" + path);
        conn.createStatement().execute("PRAGMA foreign_keys=ON;");
        return conn;
    }


    static void createTable(String sql) {
        try {
            var con = openConnection();
            var stmt = con.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();
            con.close();
        } catch (SQLException e) {
            log.error(e.getLocalizedMessage());
        }
    }

    static void executeUpdateSql(String sql, boolean ignoreStackTrace) {
        try (var conn = openConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            if (!ignoreStackTrace)
                log.error(e.getLocalizedMessage());
        }
    }

    public static void updateCols(String[] columns, String[] values, String table, int id) {
        var length = columns.length;
        if (length != values.length)
            throw new RuntimeException("columns and values do not match by length");

        var builder = new StringBuilder("UPDATE ");
        builder.append(table).append(" SET ");
        for (int i = 0; i < length; i++) {
            boolean isInteger = false;
            try {
                Integer.parseInt(values[i]);
                isInteger = true;
            } catch (Exception ignore) {
            }
            var val = values[i];
            if (!isInteger && !val.equals("NULL"))
                val = "\"" + val + "\"";
            builder.append(columns[i]).append("=").append(val);
            if (i != length - 1)
                builder.append(",");
        }
        builder.append(" WHERE ").append(COL_ID).append("=").append(id).append(";");
        DatabaseHelper.executeUpdateSql(builder.toString(), false);
    }

    public static void updateCol(String column, String value, String table, int id) {

        boolean isInteger = false;
        try {
            Integer.parseInt(value);
            isInteger = true;
        } catch (Exception ignore) {
        }
        if (!isInteger && !value.equals("NULL"))
            value = "\"" + value + "\"";

        var sql = """
                UPDATE %s SET %s=%s WHERE %s=%d;
                """
                .formatted(table, column, value, COL_ID, id);

        DatabaseHelper.executeUpdateSql(sql, false);
    }


}
