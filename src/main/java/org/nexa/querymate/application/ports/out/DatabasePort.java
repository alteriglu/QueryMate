package org.nexa.querymate.application.ports.out;

import org.nexa.querymate.domain.connection.ConnectionConfig;
import org.nexa.querymate.domain.query.QueryResult;
import org.nexa.querymate.domain.query.SqlStatement;

/**
 * Driven port for database operations.
 * Implementations provide database-specific adapters (Postgres, MySQL, etc.).
 */
public interface DatabasePort {

    /**
     * Establishes a connection to the database.
     *
     * @param config the connection configuration
     * @throws org.nexa.querymate.domain.exception.ConnectionException if connection fails
     */
    void connect(ConnectionConfig config);

    /**
     * Closes the current database connection.
     */
    void disconnect();

    /**
     * Returns true if currently connected to a database.
     */
    boolean isConnected();

    /**
     * Executes a SQL statement and returns the result.
     *
     * @param statement the SQL to execute
     * @return the query result
     * @throws org.nexa.querymate.domain.exception.QueryExecutionException if execution fails
     */
    QueryResult execute(SqlStatement statement);

    /**
     * Tests if a connection can be established with the given configuration.
     *
     * @param config the connection configuration to test
     * @return true if connection succeeds
     */
    boolean testConnection(ConnectionConfig config);
}
