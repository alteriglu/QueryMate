package org.nexa.querymate.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient beans used by infrastructure adapters.
 */
@Configuration
public class WebClientConfig {

    /**
     * WebClient for Ollama API calls.
     * Named bean allows for targeted mocking in tests.
     */
    @Bean
    public WebClient ollamaWebClient(OllamaProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
