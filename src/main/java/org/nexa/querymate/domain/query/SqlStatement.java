package org.nexa.querymate.domain.query;

import java.util.Objects;

/**
 * Value object representing a SQL statement to be executed.
 */
public record SqlStatement(String value) {

    public SqlStatement {
        Objects.requireNonNull(value, "SQL statement cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("SQL statement cannot be blank");
        }
    }

    /**
     * Creates a SqlStatement from a string.
     */
    public static SqlStatement of(String sql) {
        return new SqlStatement(sql);
    }

    /**
     * Returns the SQL normalized (trimmed).
     */
    public String normalized() {
        return value.strip();
    }

    @Override
    public String toString() {
        return value;
    }
}
