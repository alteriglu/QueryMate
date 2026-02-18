package org.nexa.querymate.domain.query;

/**
 * Indicates how the query was originated.
 */
public enum QueryOrigin {
    /**
     * Query was written directly as SQL by the user.
     */
    DIRECT_SQL,

    /**
     * Query was translated from natural language.
     */
    NATURAL_LANGUAGE
}
