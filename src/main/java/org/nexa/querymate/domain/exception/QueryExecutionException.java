package org.nexa.querymate.domain.exception;

/**
 * Exception thrown when query execution fails.
 */
public final class QueryExecutionException extends QueryMateException {

    public QueryExecutionException(String message) {
        super(message);
    }

    public QueryExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static QueryExecutionException sqlError(String sql, Throwable cause) {
        return new QueryExecutionException(
                "Failed to execute SQL: %s".formatted(truncate(sql, 100)),
                cause
        );
    }

    public static QueryExecutionException invalidSql(String sql) {
        return new QueryExecutionException(
                "Invalid SQL statement: %s".formatted(truncate(sql, 100))
        );
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "null";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }
}
