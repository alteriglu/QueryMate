package org.nexa.querymate.application.services;

import org.nexa.querymate.application.ports.in.ConnectionServicePort;
import org.nexa.querymate.application.ports.in.QueryServicePort;
import org.nexa.querymate.application.ports.out.DatabasePort;
import org.nexa.querymate.application.ports.out.LLMPort;
import org.nexa.querymate.application.ports.out.SchemaDiscoveryPort;
import org.nexa.querymate.domain.connection.Connection;
import org.nexa.querymate.domain.exception.ConnectionException;
import org.nexa.querymate.domain.query.Query;
import org.nexa.querymate.domain.query.QueryResult;
import org.nexa.querymate.domain.query.SqlStatement;
import org.nexa.querymate.domain.translation.NaturalLanguageQuery;
import org.nexa.querymate.domain.translation.SchemaContext;
import org.nexa.querymate.domain.translation.TranslationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Application service for executing queries.
 */
@Service
public class QueryService implements QueryServicePort {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private final ConnectionServicePort connectionService;
    private final DatabasePort databasePort;
    private final SchemaDiscoveryPort schemaDiscoveryPort;
    private final LLMPort llmPort;

    public QueryService(
            ConnectionServicePort connectionService,
            DatabasePort databasePort,
            SchemaDiscoveryPort schemaDiscoveryPort,
            LLMPort llmPort
    ) {
        this.connectionService = connectionService;
        this.databasePort = databasePort;
        this.schemaDiscoveryPort = schemaDiscoveryPort;
        this.llmPort = llmPort;
    }

    @Override
    public Query executeSql(SqlStatement sql) {
        Connection connection = requireActiveConnection();

        log.info("Executing SQL query",
                kv("connectionId", connection.id()),
                kv("sql", truncate(sql.value(), 100)));

        Query query = Query.fromSql(connection.id(), sql);
        return executeQuery(query);
    }

    @Override
    public Query executeNaturalLanguage(NaturalLanguageQuery naturalLanguageQuery) {
        Connection connection = requireActiveConnection();

        log.info("Executing natural language query",
                kv("connectionId", connection.id()),
                kv("query", truncate(naturalLanguageQuery.value(), 100)));

        SqlStatement sql = translateOnly(naturalLanguageQuery);

        Query query = Query.fromNaturalLanguage(connection.id(), sql, naturalLanguageQuery.value());
        return executeQuery(query);
    }

    @Override
    public SqlStatement translateOnly(NaturalLanguageQuery query) {
        requireActiveConnection();

        log.info("Translating natural language query",
                kv("query", truncate(query.value(), 100)));

        SchemaContext schemaContext = schemaDiscoveryPort.discoverSchema();

        TranslationRequest request = TranslationRequest.create(query, schemaContext);
        SqlStatement sql = llmPort.translate(query, schemaContext);
        request.markCompleted(sql);

        log.info("Translation completed",
                kv("requestId", request.id()),
                kv("sql", truncate(sql.value(), 100)));

        return sql;
    }

    private Query executeQuery(Query query) {
        query.markExecuting();

        try {
            QueryResult result = databasePort.execute(query.statement());
            query.markCompleted(result);

            log.info("Query completed",
                    kv("queryId", query.id()),
                    kv("rowCount", result.rowCount()),
                    kv("executionTime", result.metadata().executionTime()));

            return query;
        } catch (Exception e) {
            query.markFailed(e.getMessage());

            log.error("Query failed",
                    kv("queryId", query.id()),
                    kv("error", e.getMessage()));

            throw e;
        }
    }

    private Connection requireActiveConnection() {
        return connectionService.activeConnection()
                .orElseThrow(ConnectionException::notConnected);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "null";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }
}
