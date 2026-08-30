# Local Setup

## Requirements

- Docker Desktop/Engine with Compose v2 (required for the supported runtime and Testcontainers verification).
- Host development only: Java 21–25, Node 22.12–22.x, and pnpm 11.19. Maven 3.9.11 is supplied by `backend/mvnw*`.

## Configure and start

```powershell
Copy-Item .env.example .env
```

Replace `POSTGRES_PASSWORD`, `CLICKHOUSE_PASSWORD`, and `DEV_JWT_SECRET`. `AUTH_PROFILE=dev-auth` is local-only.

```powershell
docker compose config --quiet
docker compose up --build -d
docker compose ps -a
```

Startup order is PostgreSQL/Kafka/ClickHouse, idempotent topic and ClickHouse migrations, API/processor/gateway, agent, then the one-shot generator. Named volumes persist database data, Kafka logs, collector checkpoints/queues, and generated files.

## Inspect the ingestion path

```powershell
# Generator result
docker compose logs log-generator

# Topic definitions and consumer lag
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --describe
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:9092 --group log-processor-v1 --describe

# Persisted/unique rows
docker compose exec clickhouse clickhouse-client --user logatron_ingest --password $env:CLICKHOUSE_PASSWORD --query "SELECT count(), uniqExact(event_id) FROM logatron.logs_local"

# Processor probes and metrics
Invoke-RestMethod http://localhost:8081/actuator/health/liveness
Invoke-RestMethod http://localhost:8081/actuator/health/readiness
Invoke-RestMethod http://localhost:8081/actuator/metrics/logatron.processor.received
```

If `$env:CLICKHOUSE_PASSWORD` is unset, use the value from `.env`.

## Generate focused scenarios

```powershell
docker compose run --rm --no-deps log-generator --seed 99 --scenario normal --count 20 --rate 100
docker compose run --rm --no-deps log-generator --scenario null-pointer --count 5
docker compose run --rm --no-deps log-generator --scenario malformed --count 5
docker compose run --rm --no-deps log-generator --service order --scenario normal --count 10 --rotate-every 5
```

Supported scenarios: `normal`, `db-pool-exhaustion`, `kafka-retry`, `http-timeout`, `null-pointer`, `malformed`, `missing-identifier`, `duplicate`, `out-of-order`, `sensitive`, and `all`.

## Outage drill

```powershell
docker compose stop clickhouse
docker compose run --rm --no-deps log-generator --seed 99 --scenario normal --count 20 --rate 100
# Liveness stays UP; readiness is 503; consumer lag grows and offsets are not acknowledged.
docker compose start clickhouse
# Wait, then confirm lag returns to zero and readiness is UP.
```

## Stop/reset

```powershell
docker compose down                       # retain volumes
docker compose down --volumes             # destructive local reset
docker compose up --build -d
```

## Troubleshooting

- Gateway/agent: inspect `docker compose logs otel-gateway otel-agent`; both use persistent file storage and run as UID 10001.
- Processor readiness: it includes ClickHouse; liveness intentionally does not.
- Backlog: inspect the `log-processor-v1` group. ClickHouse failure must create lag rather than acknowledged loss.
- DLQ: consume `logs.dlq.v1`; payload previews are bounded, hashed, and defensively redacted.
- Migrations: never edit applied PostgreSQL or ClickHouse migrations; add a new ordered migration.
- Port conflicts: change the corresponding value in `.env`.