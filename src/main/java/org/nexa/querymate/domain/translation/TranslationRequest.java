package org.nexa.querymate.domain.translation;

import org.nexa.querymate.domain.query.SqlStatement;
import org.nexa.querymate.domain.translation.events.TranslationCompleted;
import org.nexa.querymate.domain.translation.events.TranslationEvent;
import org.nexa.querymate.domain.translation.events.TranslationFailed;
import org.nexa.querymate.domain.translation.events.TranslationRequested;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root representing a request to translate natural language to SQL.
 */
public final class TranslationRequest {

    private final TranslationRequestId id;
    private final NaturalLanguageQuery naturalLanguageQuery;
    private final SchemaContext schemaContext;
    private TranslationStatus status;
    private SqlStatement translatedSql;
    private String errorMessage;
    private Instant requestedAt;
    private Instant completedAt;
    private final List<TranslationEvent> events;

    private TranslationRequest(
            TranslationRequestId id,
            NaturalLanguageQuery naturalLanguageQuery,
            SchemaContext schemaContext
    ) {
        this.id = Objects.requireNonNull(id, "Translation request ID cannot be null");
        this.naturalLanguageQuery = Objects.requireNonNull(naturalLanguageQuery, "Natural language query cannot be null");
        this.schemaContext = Objects.requireNonNull(schemaContext, "Schema context cannot be null");
        this.status = TranslationStatus.PENDING;
        this.events = new ArrayList<>();
    }

    /**
     * Creates a new translation request.
     */
    public static TranslationRequest create(
            NaturalLanguageQuery query,
            SchemaContext schemaContext
    ) {
        TranslationRequest request = new TranslationRequest(
                TranslationRequestId.generate(),
                query,
                schemaContext
        );
        request.requestedAt = Instant.now();
        request.events.add(new TranslationRequested(
                request.id,
                query,
                schemaContext,
                request.requestedAt
        ));
        return request;
    }

    /**
     * Marks the translation as completed with the resulting SQL.
     */
    public void markCompleted(SqlStatement sql) {
        if (status != TranslationStatus.PENDING) {
            throw new IllegalStateException("Translation must be PENDING to complete");
        }
        this.status = TranslationStatus.TRANSLATED;
        this.translatedSql = Objects.requireNonNull(sql, "Translated SQL cannot be null");
        this.completedAt = Instant.now();
        events.add(new TranslationCompleted(id, sql, completedAt));
    }

    /**
     * Marks the translation as failed.
     */
    public void markFailed(String errorMessage) {
        this.status = TranslationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        events.add(new TranslationFailed(id, errorMessage, completedAt));
    }

    public TranslationRequestId id() {
        return id;
    }

    public NaturalLanguageQuery naturalLanguageQuery() {
        return naturalLanguageQuery;
    }

    public SchemaContext schemaContext() {
        return schemaContext;
    }

    public TranslationStatus status() {
        return status;
    }

    public SqlStatement translatedSql() {
        return translatedSql;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public boolean isTranslated() {
        return status == TranslationStatus.TRANSLATED;
    }

    public boolean isFailed() {
        return status == TranslationStatus.FAILED;
    }

    /**
     * Returns and clears all pending domain events.
     */
    public List<TranslationEvent> drainEvents() {
        List<TranslationEvent> drained = List.copyOf(events);
        events.clear();
        return drained;
    }

    /**
     * Returns all pending domain events without clearing.
     */
    public List<TranslationEvent> events() {
        return Collections.unmodifiableList(events);
    }
}
