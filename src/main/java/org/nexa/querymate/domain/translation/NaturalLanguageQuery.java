package org.nexa.querymate.domain.translation;

import java.util.Objects;

/**
 * Value object representing a natural language query to be translated to SQL.
 */
public record NaturalLanguageQuery(String value) {

    public NaturalLanguageQuery {
        Objects.requireNonNull(value, "Natural language query cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Natural language query cannot be blank");
        }
    }

    /**
     * Creates a NaturalLanguageQuery from a string.
     */
    public static NaturalLanguageQuery of(String query) {
        return new NaturalLanguageQuery(query);
    }

    /**
     * Returns the query normalized (trimmed).
     */
    public String normalized() {
        return value.strip();
    }

    @Override
    public String toString() {
        return value;
    }
}
