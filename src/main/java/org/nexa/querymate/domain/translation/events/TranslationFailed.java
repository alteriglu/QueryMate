package org.nexa.querymate.domain.translation.events;

import org.nexa.querymate.domain.translation.TranslationRequestId;

import java.time.Instant;

/**
 * Domain event raised when a translation fails.
 */
public record TranslationFailed(
        TranslationRequestId requestId,
        String errorMessage,
        Instant occurredAt
) implements TranslationEvent {
}
