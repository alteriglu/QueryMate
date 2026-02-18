package org.nexa.querymate.domain.connection;

import org.nexa.querymate.domain.connection.events.ConnectionClosed;
import org.nexa.querymate.domain.connection.events.ConnectionEstablished;
import org.nexa.querymate.domain.connection.events.ConnectionEvent;
import org.nexa.querymate.domain.connection.events.ConnectionFailed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root representing a database connection.
 */
public final class Connection {

    private final ConnectionId id;
    private final ConnectionConfig config;
    private ConnectionStatus status;
    private String failureReason;
    private Instant connectedAt;
    private final List<ConnectionEvent> events;

    private Connection(ConnectionId id, ConnectionConfig config) {
        this.id = Objects.requireNonNull(id, "Connection ID cannot be null");
        this.config = Objects.requireNonNull(config, "Connection config cannot be null");
        this.status = ConnectionStatus.DISCONNECTED;
        this.events = new ArrayList<>();
    }

    /**
     * Creates a new connection with the given configuration.
     */
    public static Connection create(ConnectionConfig config) {
        return new Connection(ConnectionId.generate(), config);
    }

    /**
     * Marks this connection as successfully established.
     */
    public void markEstablished() {
        this.status = ConnectionStatus.CONNECTED;
        this.connectedAt = Instant.now();
        this.failureReason = null;
        events.add(new ConnectionEstablished(id, config, connectedAt));
    }

    /**
     * Marks this connection as failed.
     */
    public void markFailed(String reason) {
        this.status = ConnectionStatus.FAILED;
        this.failureReason = reason;
        this.connectedAt = null;
        events.add(new ConnectionFailed(id, config, reason, Instant.now()));
    }

    /**
     * Marks this connection as closed.
     */
    public void markClosed() {
        ConnectionStatus previousStatus = this.status;
        this.status = ConnectionStatus.DISCONNECTED;
        this.connectedAt = null;
        events.add(new ConnectionClosed(id, previousStatus, Instant.now()));
    }

    public boolean isConnected() {
        return status == ConnectionStatus.CONNECTED;
    }

    public ConnectionId id() {
        return id;
    }

    public ConnectionConfig config() {
        return config;
    }

    public ConnectionStatus status() {
        return status;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant connectedAt() {
        return connectedAt;
    }

    /**
     * Returns and clears all pending domain events.
     */
    public List<ConnectionEvent> drainEvents() {
        List<ConnectionEvent> drained = List.copyOf(events);
        events.clear();
        return drained;
    }

    /**
     * Returns all pending domain events without clearing.
     */
    public List<ConnectionEvent> events() {
        return Collections.unmodifiableList(events);
    }
}
