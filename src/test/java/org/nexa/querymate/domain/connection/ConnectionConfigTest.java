package org.nexa.querymate.domain.connection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConnectionConfig")
class ConnectionConfigTest {

    @Nested
    @DisplayName("when creating a valid config")
    class WhenCreatingValid {

        @Test
        @DisplayName("should create config with all properties")
        void shouldCreateWithAllProperties() {
            ConnectionConfig config = new ConnectionConfig(
                    "localhost", 5432, "testdb", "user", "password"
            );

            assertThat(config.host()).isEqualTo("localhost");
            assertThat(config.port()).isEqualTo(5432);
            assertThat(config.database()).isEqualTo("testdb");
            assertThat(config.username()).isEqualTo("user");
            assertThat(config.password()).isEqualTo("password");
        }

        @Test
        @DisplayName("should generate correct JDBC URL")
        void shouldGenerateJdbcUrl() {
            ConnectionConfig config = new ConnectionConfig(
                    "db.example.com", 5433, "production", "admin", "secret"
            );

            assertThat(config.toJdbcUrl())
                    .isEqualTo("jdbc:postgresql://db.example.com:5433/production");
        }

        @Test
        @DisplayName("should generate display string without password")
        void shouldGenerateDisplayString() {
            ConnectionConfig config = new ConnectionConfig(
                    "localhost", 5432, "testdb", "admin", "secret"
            );

            String display = config.toDisplayString();

            assertThat(display).isEqualTo("admin@localhost:5432/testdb");
            assertThat(display).doesNotContain("secret");
        }
    }

    @Nested
    @DisplayName("when creating an invalid config")
    class WhenCreatingInvalid {

        @Test
        @DisplayName("should reject null host")
        void shouldRejectNullHost() {
            assertThatThrownBy(() -> new ConnectionConfig(
                    null, 5432, "db", "user", "pass"
            )).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject blank host")
        void shouldRejectBlankHost() {
            assertThatThrownBy(() -> new ConnectionConfig(
                    "  ", 5432, "db", "user", "pass"
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Host cannot be blank");
        }

        @Test
        @DisplayName("should reject invalid port")
        void shouldRejectInvalidPort() {
            assertThatThrownBy(() -> new ConnectionConfig(
                    "localhost", 0, "db", "user", "pass"
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Port must be between");

            assertThatThrownBy(() -> new ConnectionConfig(
                    "localhost", 70000, "db", "user", "pass"
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Port must be between");
        }

        @Test
        @DisplayName("should reject null database")
        void shouldRejectNullDatabase() {
            assertThatThrownBy(() -> new ConnectionConfig(
                    "localhost", 5432, null, "user", "pass"
            )).isInstanceOf(NullPointerException.class);
        }
    }
}
