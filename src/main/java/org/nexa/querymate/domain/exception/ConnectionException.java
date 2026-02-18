package org.nexa.querymate.domain.exception;

/**
 * Exception thrown when a database connection operation fails.
 */
public final class ConnectionException extends QueryMateException {

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ConnectionException failedToConnect(String host, int port, Throwable cause) {
        return new ConnectionException(
                "Failed to connect to database at %s:%d".formatted(host, port),
                cause
        );
    }

    public static ConnectionException notConnected() {
        return new ConnectionException("Not connected to any database");
    }

    public static ConnectionException connectionClosed() {
        return new ConnectionException("Connection has been closed");
    }
}
