package org.nexa.querymate.infrastructure.adapters.out.postgres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nexa.querymate.domain.exception.ConnectionException;
import org.nexa.querymate.domain.query.QueryResult;
import org.nexa.querymate.domain.query.SqlStatement;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PostgresSchemaAdapter")
class PostgresSchemaAdapterTest {

    private PostgresDatabaseAdapter databaseAdapter;
    private PostgresSchemaAdapter schemaAdapter;

    @BeforeEach
    void setUp() {
        databaseAdapter = mock(PostgresDatabaseAdapter.class);
        schemaAdapter = new PostgresSchemaAdapter(databaseAdapter);
    }

    @Nested
    @DisplayName("when discovering schema with invalid table names")
    class WhenDiscoveringSchemaWithInvalidTableNames {

        @BeforeEach
        void setUp() {
            when(databaseAdapter.isConnected()).thenReturn(true);
        }

        @ParameterizedTest
        @DisplayName("should reject SQL injection attempts in table names")
        @ValueSource(strings = {
                "users; DROP TABLE users; --",
                "users' OR '1'='1",
                "users\"; DROP TABLE users; --",
                "users$(whoami)",
                "users`ls`",
                "users|cat /etc/passwd"
        })
        void shouldRejectSqlInjectionAttempts(String maliciousTableName) {
            // The adapter should reject invalid identifiers before any SQL is executed
            // The exception is caught and logged as a warning, so the schema context will be empty
            var result = schemaAdapter.discoverSchema(List.of(maliciousTableName));
            
            // The table should be skipped (not throw, but also not include the malicious table)
            assertThat(result.tables()).isEmpty();
        }

        @ParameterizedTest
        @DisplayName("should accept valid PostgreSQL identifiers")
        @ValueSource(strings = {
                "users",
                "user_accounts",
                "_private_table",
                "Table123",
                "UPPERCASE_TABLE"
        })
        void shouldAcceptValidIdentifiers(String validTableName) {
            // Mock the execute to return empty results (we just want to verify validation passes)
            when(databaseAdapter.execute(any(SqlStatement.class)))
                    .thenReturn(new QueryResult(
                            List.of(),
                            List.of(),
                            new QueryResult.QueryMetadata(0, Duration.ZERO)
                    ));

            var result = schemaAdapter.discoverSchema(List.of(validTableName));
            
            // Should not throw and should attempt to query the table
            assertThat(result.tables()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("when not connected")
    class WhenNotConnected {

        @Test
        @DisplayName("should throw ConnectionException when discovering schema for specific tables")
        void shouldThrowConnectionException() {
            when(databaseAdapter.isConnected()).thenReturn(false);

            // discoverSchema(List) checks connection status before processing
            assertThatThrownBy(() -> schemaAdapter.discoverSchema(List.of("users")))
                    .isInstanceOf(ConnectionException.class);
        }
    }
}
