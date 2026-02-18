package org.nexa.querymate.domain.query.events;

import org.nexa.querymate.domain.connection.ConnectionId;
import org.nexa.querymate.domain.query.QueryId;
import org.nexa.querymate.domain.query.QueryOrigin;
import org.nexa.querymate.domain.query.SqlStatement;

import java.time.Instant;

/**
 * Domain event raised when a query is submitted for execution.
 */
public record QuerySubmitted(
        QueryId queryId,
        ConnectionId connectionId,
        SqlStatement statement,
        QueryOrigin origin,
        Instant occurredAt
) implements QueryEvent {
}
