package org.nexa.querymate.domain.translation;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique translation request identifier.
 */
public record TranslationRequestId(UUID value) {

    public TranslationRequestId {
        Objects.requireNonNull(value, "Translation request ID cannot be null");
    }

    public static TranslationRequestId generate() {
        return new TranslationRequestId(UUID.randomUUID());
    }

    public static TranslationRequestId of(String value) {
        return new TranslationRequestId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
