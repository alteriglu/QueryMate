package org.nexa.querymate.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Ollama LLM integration.
 */
@ConfigurationProperties(prefix = "querymate.ollama")
public record OllamaProperties(
        String baseUrl,
        String model,
        int timeoutSeconds
) {
    public OllamaProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434";
        }
        if (model == null || model.isBlank()) {
            model = "llama3.2";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 60;
        }
    }
}
