package org.nexa.querymate.domain.translation.events;

import org.nexa.querymate.domain.translation.NaturalLanguageQuery;
import org.nexa.querymate.domain.translation.SchemaContext;
import org.nexa.querymate.domain.translation.TranslationRequestId;

import java.time.Instant;

/**
 * Domain event raised when a translation request is submitted.
 */
public record TranslationRequested(
        TranslationRequestId requestId,
        NaturalLanguageQuery query,
        SchemaContext schemaContext,
        Instant occurredAt
) implements TranslationEvent {
}
