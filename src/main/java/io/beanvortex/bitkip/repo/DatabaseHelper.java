package io.beanvortex.bitkip.repo;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.beanvortex.bitkip.config.AppConfigs;

import java.sql.Connection;
import java.sql.SQLException;

import static io.beanvortex.bitkip.config.AppConfigs.log;
import static io.beanvortex.bitkip.repo.DownloadsRepo.COL_ID;

public class DatabaseHelper {


    static final String QUEUE_DOWNLOAD_TABLE_NAME = "queue_download";
    public static final String DOWNLOADS_TABLE_NAME = "downloads";
    public static final String QUEUES_TABLE_NAME = "queues";
    static final String SCHEDULE_TABLE_NAME = "schedules";

    static String COL_DOWNLOAD_ID = "download_id",
            COL_QUEUE_ID = "queue_id", COL_QUEUE_NAME = "queue_name";

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + AppConfigs.dataPath + "bitkip.db"); // JDBC URL
        config.setMaximumPoolSize(10); // Maximum number of connections in the pool
        config.setMinimumIdle(2); // Minimum number of idle connections
        config.setIdleTimeout(30000); // Timeout for idle connections (in milliseconds)
        config.setMaxLifetime(1800000); // Maximum lifetime of a connection (in milliseconds)
        config.setConnectionTimeout(30000); // Timeout for acquiring a connection (in milliseconds)
        config.setPoolName("BitKipDBPool"); // Name of the connection pool
        config.setConnectionInitSql("PRAGMA foreign_keys=ON;");
        dataSource = new HikariDataSource(config);
    }



    synchronized static Connection openConnection() throws SQLException {
        return dataSource.getConnection();
    }


    synchronized static void runSQL(String sql, boolean ignoreStackTrace) {
        try (var conn = openConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            if (!ignoreStackTrace)
                log.error(e.getLocalizedMessage());
        }
    }

    public synchronized static void updateCols(String[] columns, String[] values, String table, int id) {
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
        DatabaseHelper.runSQL(builder.toString(), false);
    }

    public synchronized static void updateCol(String column, String value, String table, int id) {

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

        DatabaseHelper.runSQL(sql, false);
    }


}
