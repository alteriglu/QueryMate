package org.nexa.querymate.domain.connection;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique connection identifier.
 */
public record ConnectionId(UUID value) {

    public ConnectionId {
        Objects.requireNonNull(value, "Connection ID cannot be null");
    }

    public static ConnectionId generate() {
        return new ConnectionId(UUID.randomUUID());
    }

    public static ConnectionId of(String value) {
        return new ConnectionId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
