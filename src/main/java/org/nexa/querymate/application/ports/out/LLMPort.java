package org.nexa.querymate.application.ports.out;

import org.nexa.querymate.domain.query.SqlStatement;
import org.nexa.querymate.domain.translation.NaturalLanguageQuery;
import org.nexa.querymate.domain.translation.SchemaContext;

/**
 * Driven port for LLM-based natural language to SQL translation.
 * Implementations provide adapter for specific LLM providers (Ollama, OpenAI, etc.).
 */
public interface LLMPort {

    /**
     * Translates a natural language query to SQL using the provided schema context.
     *
     * @param query the natural language query
     * @param schemaContext the database schema context
     * @return the translated SQL statement
     * @throws org.nexa.querymate.domain.exception.TranslationException if translation fails
     */
    SqlStatement translate(NaturalLanguageQuery query, SchemaContext schemaContext);

    /**
     * Returns true if the LLM service is available.
     */
    boolean isAvailable();
}
