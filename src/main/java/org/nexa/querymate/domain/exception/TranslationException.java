package org.nexa.querymate.domain.exception;

/**
 * Exception thrown when natural language to SQL translation fails.
 */
public final class TranslationException extends QueryMateException {

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static TranslationException llmUnavailable(Throwable cause) {
        return new TranslationException("LLM service is unavailable", cause);
    }

    public static TranslationException invalidResponse(String response) {
        return new TranslationException(
                "LLM returned invalid response: %s".formatted(truncate(response, 100))
        );
    }

    public static TranslationException timeout() {
        return new TranslationException("Translation request timed out");
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "null";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }
}
