package org.nexa.querymate.domain.translation;

import java.util.List;
import java.util.Objects;

/**
 * Value object representing database schema context for translation.
 */
public record SchemaContext(
        List<TableInfo> tables
) {

    public SchemaContext {
        tables = List.copyOf(Objects.requireNonNull(tables, "Tables cannot be null"));
    }

    /**
     * Creates an empty schema context.
     */
    public static SchemaContext empty() {
        return new SchemaContext(List.of());
    }

    /**
     * Represents information about a database table.
     */
    public record TableInfo(
            String schemaName,
            String tableName,
            List<ColumnInfo> columns,
            List<RelationInfo> relations
    ) {
        public TableInfo {
            Objects.requireNonNull(tableName, "Table name cannot be null");
            columns = List.copyOf(Objects.requireNonNull(columns, "Columns cannot be null"));
            relations = relations == null ? List.of() : List.copyOf(relations);
        }

        public String fullName() {
            if (schemaName == null || schemaName.isBlank() || "public".equals(schemaName)) {
                return tableName;
            }
            return schemaName + "." + tableName;
        }
    }

    /**
     * Represents information about a table column.
     */
    public record ColumnInfo(
            String name,
            String dataType,
            boolean nullable,
            boolean primaryKey
    ) {
        public ColumnInfo {
            Objects.requireNonNull(name, "Column name cannot be null");
            Objects.requireNonNull(dataType, "Data type cannot be null");
        }
    }

    /**
     * Represents a foreign key relationship.
     */
    public record RelationInfo(
            String columnName,
            String referencedTable,
            String referencedColumn
    ) {
        public RelationInfo {
            Objects.requireNonNull(columnName, "Column name cannot be null");
            Objects.requireNonNull(referencedTable, "Referenced table cannot be null");
            Objects.requireNonNull(referencedColumn, "Referenced column cannot be null");
        }
    }

    /**
     * Formats the schema context as a prompt-friendly string.
     */
    public String toPromptString() {
        if (tables.isEmpty()) {
            return "No schema information available.";
        }

        StringBuilder sb = new StringBuilder("Database Schema:\n\n");
        for (TableInfo table : tables) {
            sb.append("Table: ").append(table.fullName()).append("\n");
            sb.append("Columns:\n");
            for (ColumnInfo column : table.columns) {
                sb.append("  - ").append(column.name())
                        .append(" (").append(column.dataType()).append(")");
                if (column.primaryKey()) {
                    sb.append(" PRIMARY KEY");
                }
                if (!column.nullable()) {
                    sb.append(" NOT NULL");
                }
                sb.append("\n");
            }
            if (!table.relations.isEmpty()) {
                sb.append("Relations:\n");
                for (RelationInfo relation : table.relations) {
                    sb.append("  - ").append(relation.columnName())
                            .append(" -> ").append(relation.referencedTable())
                            .append(".").append(relation.referencedColumn())
                            .append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
