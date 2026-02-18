package org.nexa.querymate.domain.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SqlStatement")
class SqlStatementTest {

    @Nested
    @DisplayName("when creating a valid statement")
    class WhenCreatingValid {

        @Test
        @DisplayName("should create with value")
        void shouldCreateWithValue() {
            SqlStatement stmt = SqlStatement.of("SELECT 1");

            assertThat(stmt.value()).isEqualTo("SELECT 1");
            assertThat(stmt.toString()).isEqualTo("SELECT 1");
        }

        @Test
        @DisplayName("should normalize by stripping whitespace")
        void shouldNormalize() {
            SqlStatement stmt = SqlStatement.of("  SELECT 1  ");

            assertThat(stmt.normalized()).isEqualTo("SELECT 1");
        }
    }

    @Nested
    @DisplayName("when creating an invalid statement")
    class WhenCreatingInvalid {

        @Test
        @DisplayName("should reject null")
        void shouldRejectNull() {
            assertThatThrownBy(() -> SqlStatement.of(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject blank")
        void shouldRejectBlank() {
            assertThatThrownBy(() -> SqlStatement.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be blank");
        }
    }
}
