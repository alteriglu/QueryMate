package org.nexa.querymate.domain.translation.events;

import org.nexa.querymate.domain.translation.TranslationRequestId;

import java.time.Instant;

/**
 * Base interface for all translation-related domain events.
 */
public sealed interface TranslationEvent
        permits TranslationRequested, TranslationCompleted, TranslationFailed {

    TranslationRequestId requestId();

    Instant occurredAt();
}
