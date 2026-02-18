package org.nexa.querymate.domain.query;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing the result of a query execution.
 */
public record QueryResult(
        List<String> columns,
        List<List<Object>> rows,
        QueryMetadata metadata
) {

    public QueryResult {
        columns = List.copyOf(Objects.requireNonNull(columns, "Columns cannot be null"));
        rows = rows.stream()
                .map(List::copyOf)
                .toList();
        Objects.requireNonNull(metadata, "Metadata cannot be null");
    }

    /**
     * Creates an empty result (for statements that don't return rows).
     */
    public static QueryResult empty(Duration executionTime) {
        return new QueryResult(
                List.of(),
                List.of(),
                new QueryMetadata(0, executionTime)
        );
    }

    /**
     * Creates a result with affected row count (for INSERT/UPDATE/DELETE).
     */
    public static QueryResult affected(int affectedRows, Duration executionTime) {
        return new QueryResult(
                List.of(),
                List.of(),
                new QueryMetadata(affectedRows, executionTime)
        );
    }

    public int rowCount() {
        return rows.size();
    }

    public int columnCount() {
        return columns.size();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * Metadata about query execution.
     */
    public record QueryMetadata(
            int affectedRows,
            Duration executionTime
    ) {
        public QueryMetadata {
            Objects.requireNonNull(executionTime, "Execution time cannot be null");
        }
    }
}
