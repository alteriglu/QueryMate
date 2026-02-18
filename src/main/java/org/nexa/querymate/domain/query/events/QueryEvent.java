package org.nexa.querymate.domain.query.events;

import org.nexa.querymate.domain.query.QueryId;

import java.time.Instant;

/**
 * Base interface for all query-related domain events.
 */
public sealed interface QueryEvent
        permits QuerySubmitted, QueryExecuted, QueryFailed {

    QueryId queryId();

    Instant occurredAt();
}
