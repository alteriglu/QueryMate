package org.nexa.querymate.infrastructure.adapters.out.postgres;

import org.nexa.querymate.application.ports.out.SchemaDiscoveryPort;
import org.nexa.querymate.domain.exception.ConnectionException;
import org.nexa.querymate.domain.translation.SchemaContext;
import org.nexa.querymate.domain.translation.SchemaContext.ColumnInfo;
import org.nexa.querymate.domain.translation.SchemaContext.RelationInfo;
import org.nexa.querymate.domain.translation.SchemaContext.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PostgreSQL implementation of SchemaDiscoveryPort.
 */
@Component
public class PostgresSchemaAdapter implements SchemaDiscoveryPort {

    private static final Logger log = LoggerFactory.getLogger(PostgresSchemaAdapter.class);

    /**
     * Valid PostgreSQL identifier pattern.
     * Allows: letters, digits, underscores. Must start with letter or underscore.
     * This prevents SQL injection by rejecting any suspicious characters.
     */
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final PostgresDatabaseAdapter databaseAdapter;

    public PostgresSchemaAdapter(PostgresDatabaseAdapter databaseAdapter) {
        this.databaseAdapter = databaseAdapter;
    }

    @Override
    public SchemaContext discoverSchema() {
        return discoverSchema(listTables());
    }

    @Override
    public SchemaContext discoverSchema(List<String> tableNames) {
        if (!databaseAdapter.isConnected()) {
            throw ConnectionException.notConnected();
        }

        List<TableInfo> tables = new ArrayList<>();
        for (String tableName : tableNames) {
            try {
                TableInfo tableInfo = discoverTable(tableName);
                if (tableInfo != null) {
                    tables.add(tableInfo);
                }
            } catch (Exception e) {
                log.warn("Failed to discover table: {}", tableName, e);
            }
        }

        return new SchemaContext(tables);
    }

    @Override
    public List<String> listTables() {
        String sql = """
                SELECT table_schema, table_name
                FROM information_schema.tables
                WHERE table_schema NOT IN ('pg_catalog', 'information_schema')
                  AND table_type = 'BASE TABLE'
                ORDER BY table_schema, table_name
                """;

        var result = databaseAdapter.execute(
                org.nexa.querymate.domain.query.SqlStatement.of(sql)
        );

        return result.rows().stream()
                .map(row -> {
                    String schema = String.valueOf(row.get(0));
                    String table = String.valueOf(row.get(1));
                    return "public".equals(schema) ? table : schema + "." + table;
                })
                .toList();
    }

    @Override
    public List<String> listSchemas() {
        String sql = """
                SELECT schema_name
                FROM information_schema.schemata
                WHERE schema_name NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
                ORDER BY schema_name
                """;

        var result = databaseAdapter.execute(
                org.nexa.querymate.domain.query.SqlStatement.of(sql)
        );

        return result.rows().stream()
                .map(row -> String.valueOf(row.get(0)))
                .toList();
    }

    private TableInfo discoverTable(String fullTableName) {
        String schemaName = "public";
        String tableName = fullTableName;

        if (fullTableName.contains(".")) {
            String[] parts = fullTableName.split("\\.", 2);
            schemaName = parts[0];
            tableName = parts[1];
        }

        // Validate identifiers to prevent SQL injection
        validateIdentifier(schemaName, "schema");
        validateIdentifier(tableName, "table");

        List<ColumnInfo> columns = discoverColumns(schemaName, tableName);
        List<RelationInfo> relations = discoverRelations(schemaName, tableName);

        return new TableInfo(schemaName, tableName, columns, relations);
    }

    /**
     * Validates that an identifier is safe to use in SQL queries.
     * Rejects any identifier containing potentially dangerous characters.
     *
     * @param identifier the identifier to validate
     * @param type       description of the identifier type for error messages
     * @throws IllegalArgumentException if the identifier is invalid
     */
    private void validateIdentifier(String identifier, String type) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Invalid %s name: cannot be null or blank".formatted(type));
        }
        if (!VALID_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                    "Invalid %s name '%s': must contain only letters, digits, and underscores, and start with a letter or underscore"
                            .formatted(type, identifier)
            );
        }
    }

    /**
     * Escapes a PostgreSQL identifier by wrapping in double quotes and escaping internal quotes.
     * This provides defense-in-depth alongside validation.
     *
     * @param identifier the identifier to escape
     * @return the safely escaped identifier
     */
    private String escapeIdentifier(String identifier) {
        // Double any existing double quotes and wrap in double quotes
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private List<ColumnInfo> discoverColumns(String schemaName, String tableName) {
        // Use escaped identifiers for defense-in-depth
        String escapedSchema = escapeIdentifier(schemaName);
        String escapedTable = escapeIdentifier(tableName);

        // Note: information_schema queries use string literals for comparison, not identifiers.
        // We use the validated (unescaped) values in WHERE clauses as string literals.
        String sql = """
                SELECT
                    c.column_name,
                    c.data_type,
                    c.is_nullable,
                    CASE WHEN pk.column_name IS NOT NULL THEN true ELSE false END as is_primary_key
                FROM information_schema.columns c
                LEFT JOIN (
                    SELECT kcu.column_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                        ON tc.constraint_name = kcu.constraint_name
                        AND tc.table_schema = kcu.table_schema
                    WHERE tc.constraint_type = 'PRIMARY KEY'
                        AND tc.table_schema = %s
                        AND tc.table_name = %s
                ) pk ON c.column_name = pk.column_name
                WHERE c.table_schema = %s
                    AND c.table_name = %s
                ORDER BY c.ordinal_position
                """.formatted(
                        escapeStringLiteral(schemaName),
                        escapeStringLiteral(tableName),
                        escapeStringLiteral(schemaName),
                        escapeStringLiteral(tableName)
                );

        var result = databaseAdapter.execute(
                org.nexa.querymate.domain.query.SqlStatement.of(sql)
        );

        return result.rows().stream()
                .map(row -> new ColumnInfo(
                        String.valueOf(row.get(0)),
                        String.valueOf(row.get(1)),
                        "YES".equals(String.valueOf(row.get(2))),
                        Boolean.parseBoolean(String.valueOf(row.get(3)))
                ))
                .toList();
    }

    private List<RelationInfo> discoverRelations(String schemaName, String tableName) {
        String sql = """
                SELECT
                    kcu.column_name,
                    ccu.table_name AS referenced_table,
                    ccu.column_name AS referenced_column
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                    ON tc.constraint_name = kcu.constraint_name
                    AND tc.table_schema = kcu.table_schema
                JOIN information_schema.constraint_column_usage ccu
                    ON ccu.constraint_name = tc.constraint_name
                    AND ccu.table_schema = tc.table_schema
                WHERE tc.constraint_type = 'FOREIGN KEY'
                    AND tc.table_schema = %s
                    AND tc.table_name = %s
                """.formatted(
                        escapeStringLiteral(schemaName),
                        escapeStringLiteral(tableName)
                );

        var result = databaseAdapter.execute(
                org.nexa.querymate.domain.query.SqlStatement.of(sql)
        );

        return result.rows().stream()
                .map(row -> new RelationInfo(
                        String.valueOf(row.get(0)),
                        String.valueOf(row.get(1)),
                        String.valueOf(row.get(2))
                ))
                .toList();
    }

    /**
     * Escapes a string value for use as a SQL string literal.
     * Wraps in single quotes and escapes internal single quotes.
     *
     * @param value the value to escape
     * @return the safely escaped string literal
     */
    private String escapeStringLiteral(String value) {
        // PostgreSQL uses '' to escape single quotes within string literals
        return "'" + value.replace("'", "''") + "'";
    }
}
