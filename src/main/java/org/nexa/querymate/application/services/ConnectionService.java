package org.nexa.querymate.application.services;

import org.nexa.querymate.application.ports.in.ConnectionServicePort;
import org.nexa.querymate.application.ports.out.DatabasePort;
import org.nexa.querymate.domain.connection.Connection;
import org.nexa.querymate.domain.connection.ConnectionConfig;
import org.nexa.querymate.domain.connection.ConnectionId;
import org.nexa.querymate.domain.exception.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Application service for managing database connections.
 */
@Service
public class ConnectionService implements ConnectionServicePort {

    private static final Logger log = LoggerFactory.getLogger(ConnectionService.class);

    private final DatabasePort databasePort;
    private final AtomicReference<Connection> activeConnection = new AtomicReference<>();

    public ConnectionService(DatabasePort databasePort) {
        this.databasePort = databasePort;
    }

    @Override
    public Connection connect(ConnectionConfig config) {
        log.info("Connecting to database", kv("target", config.toDisplayString()));

        // Disconnect existing connection if any
        Connection existing = activeConnection.get();
        if (existing != null && existing.isConnected()) {
            log.info("Disconnecting existing connection", kv("connectionId", existing.id()));
            disconnect(existing.id());
        }

        Connection connection = Connection.create(config);

        try {
            databasePort.connect(config);
            connection.markEstablished();
            activeConnection.set(connection);
            log.info("Connection established",
                    kv("connectionId", connection.id()),
                    kv("target", config.toDisplayString()));
            return connection;
        } catch (Exception e) {
            connection.markFailed(e.getMessage());
            log.error("Connection failed",
                    kv("target", config.toDisplayString()),
                    kv("error", e.getMessage()));
            throw ConnectionException.failedToConnect(config.host(), config.port(), e);
        }
    }

    @Override
    public void disconnect(ConnectionId connectionId) {
        Connection connection = activeConnection.get();
        if (connection == null || !connection.id().equals(connectionId)) {
            log.warn("Attempted to disconnect non-active connection",
                    kv("connectionId", connectionId));
            return;
        }

        try {
            databasePort.disconnect();
            connection.markClosed();
            activeConnection.set(null);
            log.info("Disconnected", kv("connectionId", connectionId));
        } catch (Exception e) {
            log.error("Error during disconnect",
                    kv("connectionId", connectionId),
                    kv("error", e.getMessage()));
        }
    }

    @Override
    public Optional<Connection> activeConnection() {
        Connection connection = activeConnection.get();
        if (connection != null && connection.isConnected()) {
            return Optional.of(connection);
        }
        return Optional.empty();
    }

    @Override
    public boolean testConnection(ConnectionConfig config) {
        log.debug("Testing connection", kv("target", config.toDisplayString()));
        return databasePort.testConnection(config);
    }
}
