package org.nexa.querymate.domain.translation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nexa.querymate.domain.query.SqlStatement;
import org.nexa.querymate.domain.translation.events.TranslationCompleted;
import org.nexa.querymate.domain.translation.events.TranslationFailed;
import org.nexa.querymate.domain.translation.events.TranslationRequested;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TranslationRequest")
class TranslationRequestTest {

    private static final NaturalLanguageQuery QUERY = NaturalLanguageQuery.of("Show all users");
    private static final SchemaContext CONTEXT = SchemaContext.empty();

    @Nested
    @DisplayName("when creating a new request")
    class WhenCreating {

        @Test
        @DisplayName("should create with PENDING status and emit TranslationRequested event")
        void shouldCreatePending() {
            TranslationRequest request = TranslationRequest.create(QUERY, CONTEXT);

            assertThat(request.id()).isNotNull();
            assertThat(request.naturalLanguageQuery()).isEqualTo(QUERY);
            assertThat(request.schemaContext()).isEqualTo(CONTEXT);
            assertThat(request.status()).isEqualTo(TranslationStatus.PENDING);
            assertThat(request.requestedAt()).isNotNull();

            assertThat(request.events())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(TranslationRequested.class);
        }
    }

    @Nested
    @DisplayName("when completing translation")
    class WhenCompleting {

        @Test
        @DisplayName("should transition to TRANSLATED with SQL and emit event")
        void shouldTransitionToTranslated() {
            TranslationRequest request = TranslationRequest.create(QUERY, CONTEXT);
            request.drainEvents();

            SqlStatement sql = SqlStatement.of("SELECT * FROM users");
            request.markCompleted(sql);

            assertThat(request.status()).isEqualTo(TranslationStatus.TRANSLATED);
            assertThat(request.isTranslated()).isTrue();
            assertThat(request.translatedSql()).isEqualTo(sql);
            assertThat(request.completedAt()).isNotNull();

            assertThat(request.events())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(TranslationCompleted.class);
        }

        @Test
        @DisplayName("should reject completing twice")
        void shouldRejectCompletingTwice() {
            TranslationRequest request = TranslationRequest.create(QUERY, CONTEXT);
            request.markCompleted(SqlStatement.of("SELECT 1"));

            assertThatThrownBy(() -> request.markCompleted(SqlStatement.of("SELECT 2")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("when failing translation")
    class WhenFailing {

        @Test
        @DisplayName("should transition to FAILED with error and emit event")
        void shouldTransitionToFailed() {
            TranslationRequest request = TranslationRequest.create(QUERY, CONTEXT);
            request.drainEvents();

            request.markFailed("LLM unavailable");

            assertThat(request.status()).isEqualTo(TranslationStatus.FAILED);
            assertThat(request.isFailed()).isTrue();
            assertThat(request.errorMessage()).isEqualTo("LLM unavailable");
            assertThat(request.completedAt()).isNotNull();

            assertThat(request.events())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(TranslationFailed.class);
        }
    }
}
