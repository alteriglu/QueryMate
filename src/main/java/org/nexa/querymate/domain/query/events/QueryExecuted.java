package org.nexa.querymate.domain.query.events;

import org.nexa.querymate.domain.query.QueryId;
import org.nexa.querymate.domain.query.QueryResult;

import java.time.Instant;

/**
 * Domain event raised when a query completes successfully.
 */
public record QueryExecuted(
        QueryId queryId,
        QueryResult result,
        Instant occurredAt
) implements QueryEvent {
}
