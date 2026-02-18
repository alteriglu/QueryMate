package org.nexa.querymate.domain.connection;

import java.util.Objects;

/**
 * Value object encapsulating database connection configuration.
 */
public record ConnectionConfig(
        String host,
        int port,
        String database,
        String username,
        String password
) {

    public ConnectionConfig {
        Objects.requireNonNull(host, "Host cannot be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("Host cannot be blank");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        Objects.requireNonNull(database, "Database cannot be null");
        if (database.isBlank()) {
            throw new IllegalArgumentException("Database cannot be blank");
        }
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
    }

    /**
     * Creates a JDBC URL for PostgreSQL.
     */
    public String toJdbcUrl() {
        return "jdbc:postgresql://%s:%d/%s".formatted(host, port, database);
    }

    /**
     * Returns a display-safe string without credentials.
     */
    public String toDisplayString() {
        return "%s@%s:%d/%s".formatted(username, host, port, database);
    }
}
