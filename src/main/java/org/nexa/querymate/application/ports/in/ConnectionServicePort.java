package org.nexa.querymate.application.ports.in;

import org.nexa.querymate.domain.connection.Connection;
import org.nexa.querymate.domain.connection.ConnectionConfig;
import org.nexa.querymate.domain.connection.ConnectionId;

import java.util.Optional;

/**
 * Driving port for managing database connections.
 */
public interface ConnectionServicePort {

    /**
     * Establishes a new database connection.
     *
     * @param config the connection configuration
     * @return the established connection
     */
    Connection connect(ConnectionConfig config);

    /**
     * Closes the connection with the given ID.
     *
     * @param connectionId the connection to close
     */
    void disconnect(ConnectionId connectionId);

    /**
     * Returns the currently active connection, if any.
     */
    Optional<Connection> activeConnection();

    /**
     * Tests if a connection can be established with the given configuration.
     *
     * @param config the connection configuration to test
     * @return true if connection is successful
     */
    boolean testConnection(ConnectionConfig config);
}
