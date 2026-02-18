package org.nexa.querymate.domain.connection.events;

import org.nexa.querymate.domain.connection.ConnectionId;

import java.time.Instant;

/**
 * Base interface for all connection-related domain events.
 */
public sealed interface ConnectionEvent
        permits ConnectionEstablished, ConnectionFailed, ConnectionClosed {

    ConnectionId connectionId();

    Instant occurredAt();
}
