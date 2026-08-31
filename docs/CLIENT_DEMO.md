# LOG-A-TRON Client Demo

This walkthrough uses the real local stack and deterministic synthetic telemetry. The UI does not hard-code demo results.

## Prepare the demo state

Shortly before the meeting, start the stack and refresh the seed-42 scenario:

```powershell
docker compose up --build -d
docker compose run --rm log-generator --seed 42 --scenario all --count 80 --rate 20
docker compose ps -a
```

Open `http://localhost:4200` in a desktop browser.

## Demo identity and visible navigation

Choose **Support Demo** (**Production support · PRODUCTION_SUPPORT**) on the **Development sign in** page, then click **Issue signed development token**.

The support role sees **Overview**, **Log Explorer**, **Traces**, **Investigations**, and **Projects**. It does not see **Administration**. The single authorized project/environment is selected from backend catalog data as **Demo Marketplace / PROD**.

Development authentication is profile-isolated. Production deployments use the configured OIDC/JWT issuer.

## Exact demo values

| Item | Value |
|---|---|
| Demo identity | `support` |
| Effective role | `PRODUCTION_SUPPORT` |
| Project / environment | `Demo Marketplace / PROD` |
| Failed trace | `52b11701038c5605a9c374965e4e8d8c` |
| Trace correlation ID | `corr-trace-42` |
| Trace request ID | `req-trace-42` |
| Trace order token | `order-42` |
| Event to open | `Settlement PostgreSQL operation timed out` |

## Recommended client flow

### 1. Establish role and scope

1. Sign in as **Support Demo**.
2. Point out the user name, `PRODUCTION_SUPPORT` badge, project, and environment in the top bar.
3. Explain that the selectors contain only projects and environments returned by the authorized catalog APIs.

### 2. Show the telemetry overview

1. Open **Overview**.
2. Confirm **Demo Marketplace / PROD** in **Effective query scope**.
3. Show **Total logs**, **Errors**, **Warnings**, and **Error ratio**.
4. Point out **Errors over time** and **Top services by errors**; both are calculated from the selected authorized scope.

### 3. Find the failed event

1. Open **Log Explorer**.
2. Confirm **Demo Marketplace / PROD**.
3. Set **Time range** to **Last 24 hours**.
4. Expand **Identifier filters** and enter `52b11701038c5605a9c374965e4e8d8c` under **Trace ID**.
5. Click **Search**.
6. Select **Settlement PostgreSQL operation timed out**.
7. In **Event detail**, show severity, timestamp, message, source metadata, identifiers, and the stack trace.
8. Click **View surrounding logs** to show the selected event in chronological context.
9. Click **Open trace**.

### 4. Explain the six-span trace

1. In **Trace waterfall**, show the six spans, service count, total duration, and two errors.
2. Select `settlement-service / settleBid`, then `postgresql / INSERT settlement`.
3. Show the error styling, duration, hierarchy, safe attributes, and **Related logs** pivot.
4. Click **Investigate** in the trace header.

### 5. Present deterministic root-cause analysis

1. On **Root-cause investigation**, click **Investigate**.
2. Present **ROOT CAUSE** and **FIRST FAILURE**: **DB connection pool saturation (100%)**.
3. Walk through **What happened, in order**:

   - DB connection pool saturation reaches 100% — **STRONG EVIDENCE / FIRST FAILURE**;
   - settlement PostgreSQL operation times out — **CONFIRMED FACT**;
   - Kafka retry is scheduled — **DOWNSTREAM SYMPTOM**;
   - duplicate delivery is rejected — **DOWNSTREAM SYMPTOM**;
   - Kafka consumer lag increases to 125 — **STRONG EVIDENCE / DOWNSTREAM SYMPTOM**.

4. Show **Affected services**, **Dependency flow**, grounded log/trace/metric references, suggested verification steps, and uncertainty reporting.
5. In **AI analysis boundary**, state that the included `deterministic-stub` makes no external LLM call and validates only the bounded structured RCA contract and evidence citations.

## Optional role-differentiation moment

Sign out and choose **Admin Demo**. Administration appears because `/me` reports `ADMIN`. Sign out again and choose **Developer Demo**; Administration disappears and the authorized scope changes to Demo Marketplace DEV/UAT. This is capability-driven UX backed by server authorization, not a replacement for backend enforcement.

## Recovery if telemetry is stale

Refresh seed 42, wait briefly for ingestion, and repeat the flow:

```powershell
docker compose run --rm log-generator --seed 42 --scenario all --count 80 --rate 20
```

The trace identifier remains deterministic. A fresh run keeps the DB-pool and Kafka-lag metric samples chronologically aligned with the generated trace.
