package org.nexa.querymate.domain.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nexa.querymate.domain.connection.ConnectionId;
import org.nexa.querymate.domain.query.events.QueryExecuted;
import org.nexa.querymate.domain.query.events.QueryFailed;
import org.nexa.querymate.domain.query.events.QuerySubmitted;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Query")
class QueryTest {

    private static final ConnectionId CONNECTION_ID = ConnectionId.generate();
    private static final SqlStatement SQL = SqlStatement.of("SELECT * FROM users");

    @Nested
    @DisplayName("when creating from SQL")
    class WhenCreatingFromSql {

        @Test
        @DisplayName("should create with DIRECT_SQL origin and emit QuerySubmitted event")
        void shouldCreateWithDirectSqlOrigin() {
            Query query = Query.fromSql(CONNECTION_ID, SQL);

            assertThat(query.id()).isNotNull();
            assertThat(query.connectionId()).isEqualTo(CONNECTION_ID);
            assertThat(query.statement()).isEqualTo(SQL);
            assertThat(query.origin()).isEqualTo(QueryOrigin.DIRECT_SQL);
            assertThat(query.originalNaturalLanguage()).isNull();
            assertThat(query.status()).isEqualTo(QueryStatus.PENDING);
            assertThat(query.submittedAt()).isNotNull();

            assertThat(query.events())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(QuerySubmitted.class);
        }
    }

    @Nested
    @DisplayName("when creating from natural language")
    class WhenCreatingFromNaturalLanguage {

        @Test
        @DisplayName("should create with NATURAL_LANGUAGE origin and store original query")
        void shouldCreateWithNaturalLanguageOrigin() {
            String originalQuery = "Show me all users";
            Query query = Query.fromNaturalLanguage(CONNECTION_ID, SQL, originalQuery);

            assertThat(query.origin()).isEqualTo(QueryOrigin.NATURAL_LANGUAGE);
            assertThat(query.originalNaturalLanguage()).isEqualTo(originalQuery);
            assertThat(query.statement()).isEqualTo(SQL);
        }
    }

    @Nested
    @DisplayName("when executing")
    class WhenExecuting {

        @Test
        @DisplayName("should transition through EXECUTING to COMPLETED with result")
        void shouldTransitionToCompleted() {
            Query query = Query.fromSql(CONNECTION_ID, SQL);
            query.drainEvents();

            query.markExecuting();
            assertThat(query.status()).isEqualTo(QueryStatus.EXECUTING);

            QueryResult result = new QueryResult(
                    List.of("id", "name"),
                    List.of(List.of(1, "Alice"), List.of(2, "Bob")),
                    new QueryResult.QueryMetadata(2, Duration.ofMillis(50))
            );
            query.markCompleted(result);

            assertThat(query.status()).isEqualTo(QueryStatus.COMPLETED);
            assertThat(query.isCompleted()).isTrue();
            assertThat(query.result()).isEqualTo(result);
            assertThat(query.completedAt()).isNotNull();

            assertThat(query.events())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(QueryExecuted.class);
        }

        @Test
        @DisplayName("should reject completing without executing first")
        void shouldRejectCompletingWithoutExecuting() {
            Query query = Query.fromSql(CONNECTION_ID, SQL);

            assertThatThrownBy(() -> query.markCompleted(QueryResult.empty(Duration.ZERO)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("EXECUTING");
        }
    }

    @Nested
    @DisplayName("when failing")
    class WhenFailing {

        @Test
        @DisplayName("should transition to FAILED with error message")
        void shouldTransitionToFailed() {
            Query query = Query.fromSql(CONNECTION_ID, SQL);
            query.drainEvents();

            query.markFailed("Syntax error");

            assertThat(query.status()).isEqualTo(QueryStatus.FAILED);
            assertThat(query.isFailed()).isTrue();
            assertThat(query.errorMessage()).isEqualTo("Syntax error");
            assertThat(query.completedAt()).isNotNull();

            assertThat(query.events())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(QueryFailed.class);
        }
    }
}
