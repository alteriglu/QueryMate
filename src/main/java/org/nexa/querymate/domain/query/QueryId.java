package org.nexa.querymate.domain.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique query identifier.
 */
public record QueryId(UUID value) {

    public QueryId {
        Objects.requireNonNull(value, "Query ID cannot be null");
    }

    public static QueryId generate() {
        return new QueryId(UUID.randomUUID());
    }

    public static QueryId of(String value) {
        return new QueryId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
