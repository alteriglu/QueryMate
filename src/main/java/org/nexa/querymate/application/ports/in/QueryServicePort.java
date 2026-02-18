package org.nexa.querymate.application.ports.in;

import org.nexa.querymate.domain.query.Query;
import org.nexa.querymate.domain.query.SqlStatement;
import org.nexa.querymate.domain.translation.NaturalLanguageQuery;

/**
 * Driving port for executing queries.
 */
public interface QueryServicePort {

    /**
     * Executes a SQL query directly.
     *
     * @param sql the SQL statement to execute
     * @return the executed query with results
     */
    Query executeSql(SqlStatement sql);

    /**
     * Translates a natural language query to SQL and executes it.
     *
     * @param query the natural language query
     * @return the executed query with results
     */
    Query executeNaturalLanguage(NaturalLanguageQuery query);

    /**
     * Translates a natural language query to SQL without executing.
     *
     * @param query the natural language query
     * @return the translated SQL statement
     */
    SqlStatement translateOnly(NaturalLanguageQuery query);
}
