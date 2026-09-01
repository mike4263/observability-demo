# Observability Demo for OpenShift

A Quarkus Java web application for demonstrating OpenShift observability capabilities. Use the UI to generate CPU load and log events, then observe the signals in your cluster's logging, metrics, health, and tracing stack.

## Features

- **Web UI** with buttons to trigger CPU load (light/medium/heavy presets or custom) and emit logs at different levels
- **Structured JSON logging** via `quarkus-logging-json`
- **Prometheus metrics** at `/q/metrics`
- **Health checks** at `/q/health/live` and `/q/health/ready`
- **OpenTelemetry tracing** with OTLP export (configurable endpoint)

## Prerequisites

- Java 21+
- Maven 3.9+ (or use the included `./mvnw`)
- Podman or Docker (for container builds)
- OpenShift CLI (`oc`) for cluster deployment

## Local Development

```bash
./mvnw quarkus:dev
```

Open http://localhost:8080 to use the demo UI.

Run tests:

```bash
./mvnw test
```

## Build Container Image

```bash
podman build -t observability-demo:latest -f Containerfile .
```

Or with Docker:

```bash
docker build -t observability-demo:latest -f Containerfile .
```

## Deploy to OpenShift

### Option 1: Apply manifests

Push or import the image into your cluster's registry, then update the image reference in `deploy/deployment.yaml` if needed.

```bash
oc new-project observability-demo   # optional
oc apply -f deploy/
oc get route observability-demo
```

### Option 2: OpenShift build from source

```bash
oc new-app . --name=observability-demo --strategy=docker
oc expose svc/observability-demo
```

## Using the Demo

1. Open the app route URL in your browser.
2. Click **CPU Load** buttons to generate measurable CPU usage (visible in metrics and node monitoring).
3. Click **Logging** buttons to emit structured JSON logs at various levels.
4. Use **Burst** to simulate a burst of mixed-level log volume.
5. Visit the observability endpoint links to verify health and scrape Prometheus metrics.

## Observability Signals

| Signal | Endpoint | OpenShift Integration |
|--------|----------|----------------------|
| Logs | stdout (JSON) | Cluster Logging / Loki / Elasticsearch |
| Metrics | `/q/metrics` | User Workload Monitoring / Prometheus |
| Health | `/q/health` | Liveness/readiness probes |
| Traces | OTLP export | Tempo / Jaeger / OTel Collector |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317` | OTLP collector endpoint for trace export |
| `QUARKUS_LOG_LEVEL` | `INFO` | Log level for `com.example.obsdemo` package |

Set `OTEL_EXPORTER_OTLP_ENDPOINT` in the Deployment to point at your cluster's OpenTelemetry collector when available.

## API Reference

| Method | Path | Parameters |
|--------|------|------------|
| `POST` | `/api/cpu/load` | `threads` (1-4), `durationSeconds` (1-60) |
| `POST` | `/api/logs` | `level`, `message`, `count` (1-100) |
| `POST` | `/api/logs/burst` | `count` (1-100) |

CPU load runs in the background. A second request while load is active returns HTTP 409.

## Safety Guardrails

- CPU threads capped at 4, duration capped at 60 seconds
- Only one CPU load job runs at a time
- Deployment resource limits prevent runaway CPU usage
