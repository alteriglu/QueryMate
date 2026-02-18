package org.nexa.querymate.application.ports.out;

import org.nexa.querymate.domain.translation.SchemaContext;

import java.util.List;

/**
 * Driven port for discovering database schema information.
 * Used to provide context for natural language to SQL translation.
 */
public interface SchemaDiscoveryPort {

    /**
     * Discovers the full schema context for the connected database.
     *
     * @return schema context with all tables, columns, and relations
     */
    SchemaContext discoverSchema();

    /**
     * Discovers schema context for specific tables only.
     *
     * @param tableNames the tables to discover
     * @return schema context for the specified tables
     */
    SchemaContext discoverSchema(List<String> tableNames);

    /**
     * Returns all table names in the database.
     */
    List<String> listTables();

    /**
     * Returns all schema names in the database.
     */
    List<String> listSchemas();
}
