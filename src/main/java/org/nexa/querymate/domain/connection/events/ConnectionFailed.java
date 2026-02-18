package org.nexa.querymate.domain.connection.events;

import org.nexa.querymate.domain.connection.ConnectionConfig;
import org.nexa.querymate.domain.connection.ConnectionId;

import java.time.Instant;

/**
 * Domain event raised when a database connection attempt fails.
 */
public record ConnectionFailed(
        ConnectionId connectionId,
        ConnectionConfig config,
        String reason,
        Instant occurredAt
) implements ConnectionEvent {
}
