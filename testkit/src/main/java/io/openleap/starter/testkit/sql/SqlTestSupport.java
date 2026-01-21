package io.openleap.starter.testkit.sql;

import io.openleap.starter.testkit.config.SpringConfigurationProperties;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Objects;

public class SqlTestSupport {

    private SqlTestSupport() {

    }

    public static void truncateTables(Environment environment, String... tableNames) throws SQLException {
        String[] truncateQueries = Arrays.stream(tableNames)
                .map(table -> "TRUNCATE TABLE " + table + " RESTART IDENTITY CASCADE")
                .toArray(String[]::new);
        executeSqls(environment, truncateQueries);
    }

    public static long countRows(Environment environment, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection conn = getConnection(environment);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)
        ) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public static void executeSqls(Environment environment, String... sqls) throws SQLException {
        try (Connection conn = getConnection(environment);
             Statement stmt = conn.createStatement()
        ) {
            // Add each SQL string to the batch buffer
            for (String sql : sqls) {
                if (sql != null && !sql.isBlank()) {
                    stmt.addBatch(sql);
                }
            }

            // Execute all statements in a single database call
            stmt.executeBatch();

        }
    }

    private static Connection getConnection(Environment environment) throws SQLException {
        String url = environment.getProperty(SpringConfigurationProperties.DATASOURCE_URL);
        Objects.requireNonNull(url, "Datasource URL must not be null");
        String username = environment.getProperty(SpringConfigurationProperties.DATASOURCE_USERNAME);
        String password = environment.getProperty(SpringConfigurationProperties.DATASOURCE_PASSWORD);
        return DriverManager.getConnection(url, username, password);
    }

}
