package org.nexa.querymate.domain.connection.events;

import org.nexa.querymate.domain.connection.ConnectionId;
import org.nexa.querymate.domain.connection.ConnectionStatus;

import java.time.Instant;

/**
 * Domain event raised when a database connection is closed.
 */
public record ConnectionClosed(
        ConnectionId connectionId,
        ConnectionStatus previousStatus,
        Instant occurredAt
) implements ConnectionEvent {
}
