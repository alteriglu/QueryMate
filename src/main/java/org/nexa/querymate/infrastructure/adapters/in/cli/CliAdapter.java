package org.nexa.querymate.infrastructure.adapters.in.cli;

import org.nexa.querymate.application.ports.in.ConnectionServicePort;
import org.nexa.querymate.application.ports.in.QueryServicePort;
import org.nexa.querymate.domain.connection.Connection;
import org.nexa.querymate.domain.connection.ConnectionConfig;
import org.nexa.querymate.domain.exception.QueryMateException;
import org.nexa.querymate.domain.query.Query;
import org.nexa.querymate.domain.query.QueryResult;
import org.nexa.querymate.domain.query.SqlStatement;
import org.nexa.querymate.domain.translation.NaturalLanguageQuery;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;

/**
 * CLI adapter for interactive QueryMate usage.
 */
@Component
public class CliAdapter implements CommandLineRunner {

    private static final String PROMPT = "querymate> ";
    private static final String NL_PREFIX = "?";

    private final ConnectionServicePort connectionService;
    private final QueryServicePort queryService;
    private final PrintStream out;
    private final BufferedReader reader;

    private boolean running = true;

    public CliAdapter(
            ConnectionServicePort connectionService,
            QueryServicePort queryService
    ) {
        this.connectionService = connectionService;
        this.queryService = queryService;
        this.out = System.out;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run(String... args) throws Exception {
        printWelcome();

        while (running) {
            out.print(PROMPT);
            out.flush();

            String line = reader.readLine();
            if (line == null) {
                break;
            }

            line = line.strip();
            if (line.isEmpty()) {
                continue;
            }

            try {
                processCommand(line);
            } catch (QueryMateException e) {
                printError(e.getMessage());
            } catch (Exception e) {
                printError("Unexpected error: " + e.getMessage());
            }
        }

        out.println("Goodbye!");
    }

    private void processCommand(String input) {
        String lower = input.toLowerCase();

        if (lower.equals("exit") || lower.equals("quit") || lower.equals("\\q")) {
            running = false;
            return;
        }

        if (lower.equals("help") || lower.equals("\\h")) {
            printHelp();
            return;
        }

        if (lower.startsWith("\\c ") || lower.startsWith("connect ")) {
            handleConnect(input);
            return;
        }

        if (lower.equals("\\d") || lower.equals("disconnect")) {
            handleDisconnect();
            return;
        }

        if (lower.equals("\\s") || lower.equals("status")) {
            handleStatus();
            return;
        }

        // Natural language query (starts with ?)
        if (input.startsWith(NL_PREFIX)) {
            handleNaturalLanguageQuery(input.substring(1).strip());
            return;
        }

        // Direct SQL query
        handleSqlQuery(input);
    }

    private void handleConnect(String input) {
        // Parse: \c host:port/database user password
        // or: connect host:port/database user password
        String[] parts = input.split("\\s+", 4);
        if (parts.length < 4) {
            printError("Usage: \\c host:port/database username password");
            return;
        }

        String connectionString = parts[1];
        String username = parts[2];
        String password = parts[3];

        try {
            ConnectionConfig config = parseConnectionString(connectionString, username, password);
            Connection connection = connectionService.connect(config);
            out.println("Connected to " + config.toDisplayString());
        } catch (IllegalArgumentException e) {
            printError("Invalid connection string: " + e.getMessage());
        }
    }

    private ConnectionConfig parseConnectionString(String connectionString, String username, String password) {
        // Format: host:port/database or host/database (port defaults to 5432)
        String host;
        int port = 5432;
        String database;

        int slashIdx = connectionString.indexOf('/');
        if (slashIdx == -1) {
            throw new IllegalArgumentException("Missing database name. Use format: host:port/database");
        }

        database = connectionString.substring(slashIdx + 1);
        if (database.isBlank()) {
            throw new IllegalArgumentException("Database name cannot be empty");
        }

        String hostPort = connectionString.substring(0, slashIdx);
        if (hostPort.isBlank()) {
            throw new IllegalArgumentException("Host cannot be empty");
        }

        int colonIdx = hostPort.indexOf(':');
        if (colonIdx == -1) {
            host = hostPort;
        } else {
            host = hostPort.substring(0, colonIdx);
            if (host.isBlank()) {
                throw new IllegalArgumentException("Host cannot be empty");
            }
            String portStr = hostPort.substring(colonIdx + 1);
            try {
                port = Integer.parseInt(portStr);
                if (port <= 0 || port > 65535) {
                    throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number: '" + portStr + "'");
            }
        }

        return new ConnectionConfig(host, port, database, username, password);
    }

    private void handleDisconnect() {
        connectionService.activeConnection().ifPresentOrElse(
                connection -> {
                    connectionService.disconnect(connection.id());
                    out.println("Disconnected.");
                },
                () -> out.println("Not connected.")
        );
    }

    private void handleStatus() {
        connectionService.activeConnection().ifPresentOrElse(
                connection -> {
                    out.println("Connected to: " + connection.config().toDisplayString());
                    out.println("Status: " + connection.status());
                    if (connection.connectedAt() != null) {
                        out.println("Connected since: " + connection.connectedAt());
                    }
                },
                () -> out.println("Not connected.")
        );
    }

    private void handleNaturalLanguageQuery(String query) {
        if (query.isEmpty()) {
            printError("Empty query. Usage: ? <your question>");
            return;
        }

        out.println("Translating...");
        Query result = queryService.executeNaturalLanguage(NaturalLanguageQuery.of(query));

        out.println("\nGenerated SQL:");
        out.println(result.statement().value());
        out.println();

        printQueryResult(result);
    }

    private void handleSqlQuery(String sql) {
        Query result = queryService.executeSql(SqlStatement.of(sql));
        printQueryResult(result);
    }

    private void printQueryResult(Query query) {
        if (query.isFailed()) {
            printError(query.errorMessage());
            return;
        }

        QueryResult result = query.result();
        if (result.isEmpty() && result.columns().isEmpty()) {
            out.println("Query executed. Rows affected: " + result.metadata().affectedRows());
            return;
        }

        printTable(result.columns(), result.rows());
        out.println();
        out.println("Rows: " + result.rowCount() +
                " | Time: " + result.metadata().executionTime().toMillis() + "ms");
    }

    private void printTable(List<String> columns, List<List<Object>> rows) {
        if (columns.isEmpty()) {
            return;
        }

        // Calculate column widths
        int[] widths = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).length();
        }
        for (List<Object> row : rows) {
            for (int i = 0; i < row.size() && i < widths.length; i++) {
                String value = formatValue(row.get(i));
                widths[i] = Math.max(widths[i], value.length());
            }
        }

        // Print header
        printRow(columns.stream().map(Object.class::cast).toList(), widths);
        printSeparator(widths);

        // Print rows
        for (List<Object> row : rows) {
            printRow(row, widths);
        }
    }

    private void printRow(List<Object> values, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size() && i < widths.length; i++) {
            if (i > 0) sb.append(" | ");
            String value = formatValue(values.get(i));
            sb.append(String.format("%-" + widths[i] + "s", value));
        }
        out.println(sb);
    }

    private void printSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            if (i > 0) sb.append("-+-");
            sb.append("-".repeat(widths[i]));
        }
        out.println(sb);
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        return value.toString();
    }

    private void printWelcome() {
        out.println("""
                
                ╔═══════════════════════════════════════════════════════════╗
                ║                    QueryMate v0.1.0                       ║
                ║           SQL Client with Natural Language Support        ║
                ╚═══════════════════════════════════════════════════════════╝
                
                Type 'help' for available commands.
                """);
    }

    private void printHelp() {
        out.println("""
                Commands:
                  \\c host:port/database user pass  Connect to database
                  \\d, disconnect                   Disconnect from database
                  \\s, status                       Show connection status
                  \\h, help                         Show this help
                  \\q, exit, quit                   Exit QueryMate
                
                Queries:
                  SELECT * FROM ...                Execute SQL directly
                  ? <natural language>             Translate and execute NL query
                
                Examples:
                  \\c localhost:5432/mydb admin secret
                  SELECT * FROM users LIMIT 10;
                  ? Show me all users who signed up last month
                """);
    }

    private void printError(String message) {
        out.println("ERROR: " + message);
    }
}
