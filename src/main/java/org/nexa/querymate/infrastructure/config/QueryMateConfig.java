package org.nexa.querymate.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Main configuration class for QueryMate.
 */
@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class QueryMateConfig {
}
