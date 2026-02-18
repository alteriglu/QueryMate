package org.nexa.querymate.domain.connection.events;

import org.nexa.querymate.domain.connection.ConnectionConfig;
import org.nexa.querymate.domain.connection.ConnectionId;

import java.time.Instant;

/**
 * Domain event raised when a database connection is successfully established.
 */
public record ConnectionEstablished(
        ConnectionId connectionId,
        ConnectionConfig config,
        Instant occurredAt
) implements ConnectionEvent {
}
