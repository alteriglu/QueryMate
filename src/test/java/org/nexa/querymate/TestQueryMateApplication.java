package org.nexa.querymate;

import org.springframework.boot.SpringApplication;

/**
 * Development entry point that uses Testcontainers for the database.
 * Run this instead of QueryMateApplication for local development.
 */
public class TestQueryMateApplication {

    public static void main(String[] args) {
        SpringApplication.from(QueryMateApplication::main)
                .with(TestcontainersConfig.class)
                .run(args);
    }
}
