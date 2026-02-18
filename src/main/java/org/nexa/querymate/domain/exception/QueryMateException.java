package org.nexa.querymate.domain.exception;

/**
 * Base exception for all QueryMate domain exceptions.
 */
public sealed class QueryMateException extends RuntimeException
        permits ConnectionException, QueryExecutionException, TranslationException {

    public QueryMateException(String message) {
        super(message);
    }

    public QueryMateException(String message, Throwable cause) {
        super(message, cause);
    }
}
