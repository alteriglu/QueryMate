package org.nexa.querymate.infrastructure.adapters.out.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.nexa.querymate.application.ports.out.LLMPort;
import org.nexa.querymate.domain.exception.TranslationException;
import org.nexa.querymate.domain.query.SqlStatement;
import org.nexa.querymate.domain.translation.NaturalLanguageQuery;
import org.nexa.querymate.domain.translation.SchemaContext;
import org.nexa.querymate.infrastructure.config.OllamaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ollama implementation of LLMPort for natural language to SQL translation.
 */
@Component
public class OllamaLLMAdapter implements LLMPort {

    private static final Logger log = LoggerFactory.getLogger(OllamaLLMAdapter.class);

    private static final String SYSTEM_PROMPT = """
            You are a SQL expert. Your task is to translate natural language queries into valid PostgreSQL SQL statements.
            
            Rules:
            1. Output ONLY the SQL statement, nothing else
            2. Do not include explanations or markdown formatting
            3. Use the provided schema context to generate accurate queries
            4. If the query is ambiguous, make reasonable assumptions
            5. Always use proper SQL syntax for PostgreSQL
            """;

    private static final Pattern SQL_BLOCK_PATTERN = Pattern.compile(
            "```(?:sql)?\\s*([^`]+)```",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private final WebClient webClient;
    private final OllamaProperties properties;

    public OllamaLLMAdapter(WebClient ollamaWebClient, OllamaProperties properties) {
        this.webClient = ollamaWebClient;
        this.properties = properties;
    }

    @Override
    public SqlStatement translate(NaturalLanguageQuery query, SchemaContext schemaContext) {
        log.debug("Translating query: {}", query.value());

        String prompt = buildPrompt(query, schemaContext);
        OllamaRequest request = new OllamaRequest(
                properties.model(),
                prompt,
                false
        );

        try {
            OllamaResponse response = webClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                    .block();

            if (response == null || response.response() == null || response.response().isBlank()) {
                throw TranslationException.invalidResponse("Empty response from LLM");
            }

            String sql = extractSql(response.response());
            log.debug("Translated SQL: {}", sql);

            return SqlStatement.of(sql);

        } catch (WebClientException e) {
            log.error("Ollama API error", e);
            throw TranslationException.llmUnavailable(e);
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Timeout")) {
                throw TranslationException.timeout();
            }
            throw TranslationException.llmUnavailable(e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            log.debug("Ollama not available", e);
            return false;
        }
    }

    private String buildPrompt(NaturalLanguageQuery query, SchemaContext schemaContext) {
        return """
                %s
                
                %s
                
                User query: %s
                
                Generate the SQL:
                """.formatted(SYSTEM_PROMPT, schemaContext.toPromptString(), query.value());
    }

    private String extractSql(String response) {
        // Try to extract SQL from markdown code block
        Matcher matcher = SQL_BLOCK_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).strip();
        }

        // Otherwise, clean up and return as-is
        return response.strip()
                .replaceAll("^(SELECT|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|WITH)\\s+", "$0")
                .replaceAll("\\s*;\\s*$", "")
                .strip() + ";";
    }

    private record OllamaRequest(
            String model,
            String prompt,
            boolean stream
    ) {
    }

    private record OllamaResponse(
            String model,
            @JsonProperty("created_at") String createdAt,
            String response,
            boolean done
    ) {
    }
}
