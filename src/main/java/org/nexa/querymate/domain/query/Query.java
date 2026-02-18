package org.nexa.querymate.domain.query;

import org.nexa.querymate.domain.connection.ConnectionId;
import org.nexa.querymate.domain.query.events.QueryEvent;
import org.nexa.querymate.domain.query.events.QueryExecuted;
import org.nexa.querymate.domain.query.events.QueryFailed;
import org.nexa.querymate.domain.query.events.QuerySubmitted;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root representing a query to be executed against a database.
 */
public final class Query {

    private final QueryId id;
    private final ConnectionId connectionId;
    private final SqlStatement statement;
    private final QueryOrigin origin;
    private final String originalNaturalLanguage;
    private QueryStatus status;
    private QueryResult result;
    private String errorMessage;
    private Instant submittedAt;
    private Instant completedAt;
    private final List<QueryEvent> events;

    private Query(
            QueryId id,
            ConnectionId connectionId,
            SqlStatement statement,
            QueryOrigin origin,
            String originalNaturalLanguage
    ) {
        this.id = Objects.requireNonNull(id, "Query ID cannot be null");
        this.connectionId = Objects.requireNonNull(connectionId, "Connection ID cannot be null");
        this.statement = Objects.requireNonNull(statement, "SQL statement cannot be null");
        this.origin = Objects.requireNonNull(origin, "Query origin cannot be null");
        this.originalNaturalLanguage = originalNaturalLanguage;
        this.status = QueryStatus.PENDING;
        this.events = new ArrayList<>();
    }

    /**
     * Creates a new query from direct SQL input.
     */
    public static Query fromSql(ConnectionId connectionId, SqlStatement statement) {
        Query query = new Query(
                QueryId.generate(),
                connectionId,
                statement,
                QueryOrigin.DIRECT_SQL,
                null
        );
        query.submit();
        return query;
    }

    /**
     * Creates a new query from natural language translation.
     */
    public static Query fromNaturalLanguage(
            ConnectionId connectionId,
            SqlStatement translatedStatement,
            String originalQuery
    ) {
        Query query = new Query(
                QueryId.generate(),
                connectionId,
                translatedStatement,
                QueryOrigin.NATURAL_LANGUAGE,
                originalQuery
        );
        query.submit();
        return query;
    }

    private void submit() {
        this.submittedAt = Instant.now();
        events.add(new QuerySubmitted(id, connectionId, statement, origin, submittedAt));
    }

    /**
     * Marks the query as currently executing.
     */
    public void markExecuting() {
        if (status != QueryStatus.PENDING) {
            throw new IllegalStateException("Query must be PENDING to start executing");
        }
        this.status = QueryStatus.EXECUTING;
    }

    /**
     * Marks the query as completed with results.
     */
    public void markCompleted(QueryResult result) {
        if (status != QueryStatus.EXECUTING) {
            throw new IllegalStateException("Query must be EXECUTING to complete");
        }
        this.status = QueryStatus.COMPLETED;
        this.result = Objects.requireNonNull(result, "Result cannot be null");
        this.completedAt = Instant.now();
        events.add(new QueryExecuted(id, result, completedAt));
    }

    /**
     * Marks the query as failed.
     */
    public void markFailed(String errorMessage) {
        this.status = QueryStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        events.add(new QueryFailed(id, errorMessage, completedAt));
    }

    public QueryId id() {
        return id;
    }

    public ConnectionId connectionId() {
        return connectionId;
    }

    public SqlStatement statement() {
        return statement;
    }

    public QueryOrigin origin() {
        return origin;
    }

    public String originalNaturalLanguage() {
        return originalNaturalLanguage;
    }

    public QueryStatus status() {
        return status;
    }

    public QueryResult result() {
        return result;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public Instant submittedAt() {
        return submittedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public boolean isCompleted() {
        return status == QueryStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == QueryStatus.FAILED;
    }

    /**
     * Returns and clears all pending domain events.
     */
    public List<QueryEvent> drainEvents() {
        List<QueryEvent> drained = List.copyOf(events);
        events.clear();
        return drained;
    }

    /**
     * Returns all pending domain events without clearing.
     */
    public List<QueryEvent> events() {
        return Collections.unmodifiableList(events);
    }
}
