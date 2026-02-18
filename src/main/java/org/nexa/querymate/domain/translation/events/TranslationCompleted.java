package org.nexa.querymate.domain.translation.events;

import org.nexa.querymate.domain.query.SqlStatement;
import org.nexa.querymate.domain.translation.TranslationRequestId;

import java.time.Instant;

/**
 * Domain event raised when a translation completes successfully.
 */
public record TranslationCompleted(
        TranslationRequestId requestId,
        SqlStatement translatedSql,
        Instant occurredAt
) implements TranslationEvent {
}
