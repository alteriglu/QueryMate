package org.nexa.querymate.domain.connection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nexa.querymate.domain.connection.events.ConnectionClosed;
import org.nexa.querymate.domain.connection.events.ConnectionEstablished;
import org.nexa.querymate.domain.connection.events.ConnectionFailed;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Connection")
class ConnectionTest {

    private static final ConnectionConfig TEST_CONFIG = new ConnectionConfig(
            "localhost", 5432, "testdb", "user", "password"
    );

    @Nested
    @DisplayName("when creating a new connection")
    class WhenCreating {

        @Test
        @DisplayName("should start in DISCONNECTED status")
        void shouldStartDisconnected() {
            Connection connection = Connection.create(TEST_CONFIG);

            assertThat(connection.status()).isEqualTo(ConnectionStatus.DISCONNECTED);
            assertThat(connection.isConnected()).isFalse();
            assertThat(connection.id()).isNotNull();
            assertThat(connection.config()).isEqualTo(TEST_CONFIG);
        }
    }

    @Nested
    @DisplayName("when marking as established")
    class WhenEstablished {

        @Test
        @DisplayName("should transition to CONNECTED status and emit event")
        void shouldTransitionToConnected() {
            Connection connection = Connection.create(TEST_CONFIG);

            connection.markEstablished();

            assertThat(connection.status()).isEqualTo(ConnectionStatus.CONNECTED);
            assertThat(connection.isConnected()).isTrue();
            assertThat(connection.connectedAt()).isNotNull();
            assertThat(connection.failureReason()).isNull();

            assertThat(connection.events())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(ConnectionEstablished.class);
        }
    }

    @Nested
    @DisplayName("when marking as failed")
    class WhenFailed {

        @Test
        @DisplayName("should transition to FAILED status with reason and emit event")
        void shouldTransitionToFailed() {
            Connection connection = Connection.create(TEST_CONFIG);

            connection.markFailed("Connection refused");

            assertThat(connection.status()).isEqualTo(ConnectionStatus.FAILED);
            assertThat(connection.isConnected()).isFalse();
            assertThat(connection.failureReason()).isEqualTo("Connection refused");
            assertThat(connection.connectedAt()).isNull();

            assertThat(connection.events())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(ConnectionFailed.class);
        }
    }

    @Nested
    @DisplayName("when marking as closed")
    class WhenClosed {

        @Test
        @DisplayName("should transition to DISCONNECTED status and emit event")
        void shouldTransitionToDisconnected() {
            Connection connection = Connection.create(TEST_CONFIG);
            connection.markEstablished();
            connection.drainEvents(); // Clear previous events

            connection.markClosed();

            assertThat(connection.status()).isEqualTo(ConnectionStatus.DISCONNECTED);
            assertThat(connection.isConnected()).isFalse();

            assertThat(connection.events())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(ConnectionClosed.class);
        }
    }

    @Nested
    @DisplayName("when draining events")
    class WhenDrainingEvents {

        @Test
        @DisplayName("should return events and clear the list")
        void shouldReturnAndClearEvents() {
            Connection connection = Connection.create(TEST_CONFIG);
            connection.markEstablished();

            var events = connection.drainEvents();

            assertThat(events).hasSize(1);
            assertThat(connection.events()).isEmpty();
        }
    }
}
