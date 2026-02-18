package org.nexa.querymate.domain.query.events;

import org.nexa.querymate.domain.query.QueryId;

import java.time.Instant;

/**
 * Domain event raised when a query execution fails.
 */
public record QueryFailed(
        QueryId queryId,
        String errorMessage,
        Instant occurredAt
) implements QueryEvent {
}
