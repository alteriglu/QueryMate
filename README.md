# QueryMate

A SQL client with natural language query support, built with Domain-Driven Design and Hexagonal Architecture.

## Features

- **Direct SQL Execution** — Run SQL queries against PostgreSQL databases
- **Natural Language Queries** — Ask questions in plain English, get SQL generated via LLM
- **Schema-Aware Translation** — LLM uses actual database schema for accurate SQL generation
- **CLI Interface** — Interactive command-line experience

## Architecture

QueryMate follows **Hexagonal Architecture** (Ports & Adapters) with **Domain-Driven Design** principles.

```
┌─────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE                           │
│  ┌─────────────┐                              ┌──────────────┐  │
│  │   CLI       │                              │  PostgreSQL  │  │
│  │  Adapter    │                              │   Adapter    │  │
│  └──────┬──────┘                              └──────┬───────┘  │
│         │                                            │          │
│         ▼                                            ▼          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    APPLICATION                          │    │
│  │  ┌─────────────────┐        ┌────────────────────┐      │    │
│  │  │ QueryServicePort│        │ ConnectionService  │      │    │
│  │  │    (in port)    │        │     Port (in)      │      │    │
│  │  └────────┬────────┘        └─────────┬──────────┘      │    │
│  │           │                           │                 │    │
│  │           ▼                           ▼                 │    │
│  │  ┌─────────────────┐        ┌────────────────────┐      │    │
│  │  │  QueryService   │◄──────►│ConnectionService   │      │    │
│  │  └────────┬────────┘        └────────────────────┘      │    │
│  │           │                                             │    │
│  │           ▼                                             │    │
│  │  ┌─────────────────┐  ┌───────────────┐  ┌──────────┐  │    │
│  │  │ DatabasePort    │  │SchemaDiscovery│  │ LLMPort  │  │    │
│  │  │   (out port)    │  │ Port (out)    │  │  (out)   │  │    │
│  │  └─────────────────┘  └───────────────┘  └──────────┘  │    │
│  └─────────────────────────────────────────────────────────┘    │
│         │                        │                  │           │
│         ▼                        ▼                  ▼           │
│  ┌─────────────┐         ┌─────────────┐    ┌─────────────┐    │
│  │  Postgres   │         │  Postgres   │    │   Ollama    │    │
│  │  Database   │         │   Schema    │    │   Adapter   │    │
│  │  Adapter    │         │   Adapter   │    │             │    │
│  └─────────────┘         └─────────────┘    └─────────────┘    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                          DOMAIN                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │   Connection    │  │     Query       │  │  Translation    │  │
│  │   Bounded       │  │    Bounded      │  │   Bounded       │  │
│  │   Context       │  │    Context      │  │   Context       │  │
│  │                 │  │                 │  │                 │  │
│  │ • Connection    │  │ • Query         │  │ • Translation   │  │
│  │ • ConnectionId  │  │ • QueryId       │  │   Request       │  │
│  │ • ConnectionCfg │  │ • SqlStatement  │  │ • NLQuery       │  │
│  │ • Events        │  │ • QueryResult   │  │ • SchemaContext │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
org.nexa.querymate
├── domain                    # Pure domain logic, no framework dependencies
│   ├── connection/           # Connection bounded context
│   ├── query/                # Query execution bounded context
│   ├── translation/          # NL→SQL translation bounded context
│   └── exception/            # Domain exceptions
├── application
│   ├── ports/
│   │   ├── in/               # Driving ports (interfaces for use cases)
│   │   └── out/              # Driven ports (interfaces for external systems)
│   └── services/             # Application services implementing driving ports
└── infrastructure
    ├── config/               # Spring configuration
    └── adapters/
        ├── in/cli/           # CLI adapter (driving)
        └── out/
            ├── postgres/     # PostgreSQL adapter (driven)
            └── ollama/       # Ollama LLM adapter (driven)
```

## Prerequisites

- **Java 21+**
- **Maven 3.9+**
- **PostgreSQL** database to connect to
- **Ollama** running locally (for natural language queries)

## Quick Start

### 1. Build

```bash
./mvnw clean package
```

### 2. Run

```bash
java -jar target/querymate-0.1.0-SNAPSHOT.jar
```

### 3. Connect to a Database

```
querymate> \c localhost:5432/mydb username password
Connected to username@localhost:5432/mydb
```

### 4. Run Queries

**Direct SQL:**
```
querymate> SELECT * FROM users LIMIT 5;
```

**Natural Language:**
```
querymate> ? Show me all users who signed up last month
Translating...

Generated SQL:
SELECT * FROM users WHERE created_at >= date_trunc('month', current_date - interval '1 month') 
  AND created_at < date_trunc('month', current_date);

id | name  | email           | created_at
---+-------+-----------------+------------
 1 | Alice | alice@test.com  | 2024-01-15
 2 | Bob   | bob@test.com    | 2024-01-20
```

## CLI Commands

| Command | Description |
|---------|-------------|
| `\c host:port/db user pass` | Connect to database |
| `\d`, `disconnect` | Disconnect from database |
| `\s`, `status` | Show connection status |
| `\h`, `help` | Show help |
| `\q`, `exit`, `quit` | Exit QueryMate |
| `SELECT ...` | Execute SQL directly |
| `? <question>` | Natural language query |

## Configuration

Configuration via `application.yml` or environment variables:

```yaml
querymate:
  ollama:
    base-url: http://localhost:11434
    model: llama3.2
    timeout-seconds: 60
```

## Development

### Run with Testcontainers (no local Postgres needed)

```bash
./mvnw spring-boot:test-run
```

This starts the app with a PostgreSQL container for testing.

### Run Tests

```bash
./mvnw test
```

## Tech Stack

- **Java 21** — Records, sealed classes, pattern matching, virtual threads
- **Spring Boot 3.4** — Modern Spring with Jakarta EE
- **PostgreSQL** — Primary database support
- **Ollama** — Local LLM for natural language translation
- **Testcontainers** — Integration testing with real containers

## License

MIT
