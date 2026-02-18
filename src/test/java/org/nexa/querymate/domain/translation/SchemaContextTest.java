package org.nexa.querymate.domain.translation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nexa.querymate.domain.translation.SchemaContext.ColumnInfo;
import org.nexa.querymate.domain.translation.SchemaContext.RelationInfo;
import org.nexa.querymate.domain.translation.SchemaContext.TableInfo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SchemaContext")
class SchemaContextTest {

    @Nested
    @DisplayName("when creating")
    class WhenCreating {

        @Test
        @DisplayName("should create empty context")
        void shouldCreateEmpty() {
            SchemaContext context = SchemaContext.empty();

            assertThat(context.tables()).isEmpty();
        }

        @Test
        @DisplayName("should create context with tables")
        void shouldCreateWithTables() {
            TableInfo users = new TableInfo(
                    "public",
                    "users",
                    List.of(
                            new ColumnInfo("id", "bigint", false, true),
                            new ColumnInfo("name", "varchar", false, false),
                            new ColumnInfo("email", "varchar", true, false)
                    ),
                    List.of()
            );

            SchemaContext context = new SchemaContext(List.of(users));

            assertThat(context.tables()).hasSize(1);
            assertThat(context.tables().get(0).tableName()).isEqualTo("users");
        }
    }

    @Nested
    @DisplayName("TableInfo")
    class TableInfoTests {

        @Test
        @DisplayName("should return simple name for public schema")
        void shouldReturnSimpleNameForPublic() {
            TableInfo table = new TableInfo("public", "users", List.of(), List.of());

            assertThat(table.fullName()).isEqualTo("users");
        }

        @Test
        @DisplayName("should return qualified name for non-public schema")
        void shouldReturnQualifiedName() {
            TableInfo table = new TableInfo("custom", "users", List.of(), List.of());

            assertThat(table.fullName()).isEqualTo("custom.users");
        }
    }

    @Nested
    @DisplayName("when generating prompt string")
    class WhenGeneratingPrompt {

        @Test
        @DisplayName("should format empty context")
        void shouldFormatEmpty() {
            SchemaContext context = SchemaContext.empty();

            assertThat(context.toPromptString())
                    .contains("No schema information available");
        }

        @Test
        @DisplayName("should format tables with columns and relations")
        void shouldFormatTables() {
            TableInfo orders = new TableInfo(
                    "public",
                    "orders",
                    List.of(
                            new ColumnInfo("id", "bigint", false, true),
                            new ColumnInfo("user_id", "bigint", false, false),
                            new ColumnInfo("total", "decimal", true, false)
                    ),
                    List.of(
                            new RelationInfo("user_id", "users", "id")
                    )
            );

            SchemaContext context = new SchemaContext(List.of(orders));
            String prompt = context.toPromptString();

            assertThat(prompt)
                    .contains("Table: orders")
                    .contains("id (bigint) PRIMARY KEY NOT NULL")
                    .contains("user_id (bigint)")
                    .contains("total (decimal)")
                    .contains("user_id -> users.id");
        }
    }
}
