package org.nexa.querymate.infrastructure.adapters.out.postgres;

import org.nexa.querymate.application.ports.out.DatabasePort;
import org.nexa.querymate.domain.connection.ConnectionConfig;
import org.nexa.querymate.domain.exception.ConnectionException;
import org.nexa.querymate.domain.exception.QueryExecutionException;
import org.nexa.querymate.domain.query.QueryResult;
import org.nexa.querymate.domain.query.SqlStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PostgreSQL implementation of DatabasePort.
 */
@Component
public class PostgresDatabaseAdapter implements DatabasePort {

    private static final Logger log = LoggerFactory.getLogger(PostgresDatabaseAdapter.class);

    private final AtomicReference<java.sql.Connection> connectionRef = new AtomicReference<>();

    @Override
    public void connect(ConnectionConfig config) {
        try {
            java.sql.Connection connection = DriverManager.getConnection(
                    config.toJdbcUrl(),
                    config.username(),
                    config.password()
            );
            connectionRef.set(connection);
            log.debug("PostgreSQL connection established");
        } catch (SQLException e) {
            throw ConnectionException.failedToConnect(config.host(), config.port(), e);
        }
    }

    @Override
    public void disconnect() {
        java.sql.Connection connection = connectionRef.getAndSet(null);
        if (connection != null) {
            try {
                connection.close();
                log.debug("PostgreSQL connection closed");
            } catch (SQLException e) {
                log.warn("Error closing PostgreSQL connection", e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        java.sql.Connection connection = connectionRef.get();
        if (connection == null) {
            return false;
        }
        try {
            return !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public QueryResult execute(SqlStatement statement) {
        java.sql.Connection connection = connectionRef.get();
        if (connection == null) {
            throw ConnectionException.notConnected();
        }

        Instant start = Instant.now();
        String sql = statement.normalized();

        try (Statement stmt = connection.createStatement()) {
            boolean hasResultSet = stmt.execute(sql);
            Duration executionTime = Duration.between(start, Instant.now());

            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    return mapResultSet(rs, executionTime);
                }
            } else {
                int affectedRows = stmt.getUpdateCount();
                return QueryResult.affected(affectedRows, executionTime);
            }
        } catch (SQLException e) {
            throw QueryExecutionException.sqlError(sql, e);
        }
    }

    @Override
    public boolean testConnection(ConnectionConfig config) {
        try (java.sql.Connection connection = DriverManager.getConnection(
                config.toJdbcUrl(),
                config.username(),
                config.password()
        )) {
            return connection.isValid(5);
        } catch (SQLException e) {
            log.debug("Connection test failed", e);
            return false;
        }
    }

    private QueryResult mapResultSet(ResultSet rs, Duration executionTime) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }

        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
        }

        return new QueryResult(
                columns,
                rows,
                new QueryResult.QueryMetadata(rows.size(), executionTime)
        );
    }
}
