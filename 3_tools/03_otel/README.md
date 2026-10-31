# 03 OpenTelemetry + Spring MVC

One small coffee-order API demonstrating request/trace/correlation IDs, structured
logs, counters, histograms, percentiles, and spans. Native Jaeger, no Docker or database.

## 1. Curls And URLs: Run, Compare, And Observe

### 1.1 Start The App

From `03_otel/`, run:

```bash
./run.sh
```

The single script reuses `.tools/jaeger` or downloads the pinned native Jaeger
binary on first run, verifies its binaries against the published checksums, builds
Spring Boot, and runs both processes. Keep this terminal open. **Ctrl+C stops both.**
If the app port is occupied, the script sends its listener SIGTERM and waits up to
10 seconds for release. Occupied Jaeger ports still cause an error.

| Requirement / Setting | This Project |
| --- | --- |
| Java / Maven | Requires Java 21+ and Maven; installed on the development machine. |
| Shell tools | Bash, curl, tar, sha256sum, ss, and lsof. |
| First-run network | Needed for Jaeger and Maven dependencies, stored in `.tools/`. |
| App | `http://localhost:8080`; use `SERVER_PORT=8081 ./run.sh` to change it. |
| Jaeger UI | [http://localhost:16686](http://localhost:16686) |
| OTLP receiver | HTTP/protobuf on `127.0.0.1:4318`; gRPC on `127.0.0.1:4317`. |
| Metrics | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) |
| Health | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |
| Trace storage | In memory, capped at 10,000 traces; cleared when Jaeger stops. |
| Logs | `.run/app.log` for app JSON; `.run/otel-logs.log` for received OTLP records; `.run/jaeger.log` for receiver diagnostics. |

Use another terminal for the commands below. Match `BASE` to the port printed by `run.sh`.

```bash
BASE=http://localhost:8080
curl -s "$BASE/actuator/health"
# Expected: {"status":"UP"}
```

### 1.2 Successful And Failed Orders

```bash
curl -i "$BASE/order"
# Expected: HTTP 200, message "Coffee ordered", and all three IDs.

curl -i "$BASE/order?fail=true"
# Expected: HTTP 500, message "Demo payment declined", and all three IDs.
```

The same IDs appear in response headers and JSON. They are two representations
in one HTTP response, not separate responses.

```json
{"message":"Coffee ordered","requestId":"a UUID","traceId":"32 hex characters","correlationId":"a UUID"}
```

### 1.3 Reuse Correlation Across Attempts

```bash
curl -i -H 'X-Correlation-ID: checkout-123' "$BASE/order"
curl -i -H 'X-Correlation-ID: checkout-123' "$BASE/order?fail=true"
```

| Compare | Expected Observation |
| --- | --- |
| `X-Correlation-ID` / `correlationId` | Both requests use `checkout-123`. |
| `X-Request-ID` / `requestId` | A new UUID per received request. |
| `X-Trace-ID` / `traceId` | Separate traces because neither request supplies a parent. |

```bash
curl -i -H 'X-Correlation-ID: contains spaces' "$BASE/order"
# Expected: a generated correlation UUID replaces the invalid value.
```

### 1.4 Continue An Upstream Trace And Open It

```bash
TRACE_ID=1234567890abcdef1234567890abcdef
curl -i "$BASE/order" \
  -H 'X-Correlation-ID: checkout-123' \
  -H "traceparent: 00-$TRACE_ID-1234567890abcdef-01"

# Open this URL in your browser after allowing a few seconds for export:
echo "http://localhost:16686/trace/$TRACE_ID"

# Inspect the stored trace through the query API:
curl -s "http://localhost:16686/api/traces/$TRACE_ID"
```

The response trace ID should match `TRACE_ID`. Use a fresh trace ID when comparing
independent experiments; repeating this header deliberately adds spans to the same trace.
In the UI, select **otel-demo** and find traces, or use the direct trace URL.

```text
GET /order                      automatic server span, child of caller's span
└── place-order                 manual business span; order.item=coffee
    ├── check-stock   ~80 ms    reads the in-memory repository
    └── take-payment ~120 ms    ~250 ms and ERROR on fail=true
```

### 1.5 Inspect Local JSON And Received OTel Logs

```bash
# App-generated JSON logs:
tail -f .run/app.log

# In another terminal: records received over OTLP and written by the debug exporter:
tail -f .run/otel-logs.log

# Search both outputs for one business correlation:
rg -n 'checkout-123' .run/app.log .run/otel-logs.log
```

Allow a few seconds for application and receiver batching. Business JSON logs include
`event`, `requestId`, `traceId`, `correlationId`, and the active `span_id`. Received
OTLP records include body, severity, attributes, native trace/span IDs, and resource
information. Jaeger's UI displays traces; inspect exported logs in the file above.

### 1.6 Generate And Inspect Metrics

```bash
for i in {1..20}; do curl -s "$BASE/order" > /dev/null; done
for i in {1..5}; do curl -s "$BASE/order?fail=true" > /dev/null; done

curl -s "$BASE/actuator/prometheus" | rg '^orders_'
curl -s "$BASE/actuator/metrics/orders.requests"
curl -s "$BASE/actuator/metrics/orders.requests?tag=outcome:error"
```

| Output | Expected Observation |
| --- | --- |
| `orders_requests_total{outcome="success"}` | Successful attempts increase this series. |
| `orders_requests_total{outcome="error"}` | Failed business attempts increase this series. |
| `orders_duration_seconds_bucket{outcome="...",le="..."}` | Cumulative duration buckets; upper bounds are in seconds. |
| `orders_duration_seconds_count` / `_sum` | Timed attempts / total elapsed time per outcome. |
| `orders_latency_seconds{outcome="...",quantile="0.5"}` | Approximate local p50 latency in seconds. |
| `orders_latency_seconds{outcome="...",quantile="0.95"}` | Approximate local p95 latency in seconds. |

Counts include earlier experiments. Values appear after requests create the meters.
The summary quantiles use a rolling window, so inspect them soon after generating traffic.
There is no separate Prometheus server installed; this endpoint exposes scrape data.
For a walkthrough of counters, buckets, and percentiles, read [metrics.md](metrics.md).

### 1.7 Stop, Rebuild, And Troubleshoot

Press **Ctrl+C in the `run.sh` terminal** to stop both processes. Run `./run.sh`
again after code/config changes; it performs a clean Maven build each time.

| Symptom | Check / Action |
| --- | --- |
| Port occupied | The app-port listener is stopped automatically. If termination fails or the port remains busy after 10 seconds, inspect the listener/ownership. Stop an earlier Jaeger run with Ctrl+C; its ports stay fixed. |
| Startup fails | Read `.run/app.log` and `.run/jaeger.log`; the script prints their last lines on readiness failure. |
| Trace not immediately visible | Allow batching delay, check the actual response trace ID, then inspect receiver errors. |
| Logs exist locally but no OTLP records | Check `otel.logs.exporter`, the Logback appender configuration, and the `logs` pipeline in `config/jaeger.yaml`. |
| No `orders_` metrics | Make an `/order` request first. A health check does not call `OrderService`. |
| p50/p95 becomes NaN after inactivity | Generate new requests; the local percentile window has no recent samples. |
| App port conflicts with Jaeger | Choose a `SERVER_PORT` other than 4317, 4318, or 16686; the runner rejects these before stopping any listener. |

## 2. Tabular Mental Model By Topic

### 2.1 MVC And The Request Path

| Order / File | Responsibility | Important Boundary |
| --- | --- | --- |
| Container | Creates the servlet request/response. | The filter does not create `HttpServletResponse`. |
| OTel starter | Creates/continues the HTTP server span. | Parses W3C `traceparent` before our filter uses `Span.current()`. |
| [RequestIdsFilter](app/src/main/java/com/example/otel/config/RequestIdsFilter.java) | IDs, MDC, response headers, server-span attributes, cleanup. | `chain.doFilter` continues processing; `finally` restores the previous MDC. |
| [OrderController](app/src/main/java/com/example/otel/controller/OrderController.java) | Calls the service and selects HTTP 200/500. | `ResponseEntity` describes the status and body. |
| [OrderService](app/src/main/java/com/example/otel/service/OrderService.java) | Business flow, manual spans, counters, timers, logs. | Simulated payment, not an external payment integration. |
| [InventoryRepository](app/src/main/java/com/example/otel/repository/InventoryRepository.java) | Reads fixed stock from an in-memory map. | No persistence, stock decrement, or database. |
| [OrderResponse](app/src/main/java/com/example/otel/model/OrderResponse.java) | Typed payload containing message and IDs. | Spring serializes this object into the original servlet response. |
| [Properties](app/src/main/resources/application.properties), [Jaeger config](config/jaeger.yaml) | App instrumentation/export and receiver pipelines. | App-side export and receiver-side handling both need configuration. |

### 2.2 Three IDs And Propagation

| Concern | Request ID | Trace ID | Correlation ID |
| --- | --- | --- | --- |
| Scope here | One received request attempt at this service. | One instrumented execution across spans/services. | Related business requests/events, including retries. |
| Source | New UUID generated by our filter. | Created or continued by OTel. | Valid incoming value or new UUID. |
| Incoming header | Incoming `X-Request-ID` is not reused here. | W3C `traceparent`; `X-Trace-ID` is not a propagation mechanism here. | `X-Correlation-ID`. |
| Response header | `X-Request-ID`. | `X-Trace-ID`. | `X-Correlation-ID`. |
| JSON / MDC field | `requestId`. | `traceId`; OTel also supplies `trace_id` in logs. | `correlationId`. |
| Validation | Generated UUID. | OTel validates trace context. | `[A-Za-z0-9._-]{1,64}` validates the value, not the header name. |
| Future downstream call | A downstream service can generate its own attempt ID. | Use an instrumented client to inject context. | Forward explicitly unless you configure propagation. |

### 2.3 Logs, Metrics, Traces, And Tools

| Signal / Tool | Behavior In This App | Limit |
| --- | --- | --- |
| JSON logs | Timestamp, severity, message, event, contextual IDs. | Startup logs may have no request or active span. |
| MDC | Adds IDs to logs on the current thread. | Does not automatically follow arbitrary async tasks; restore it on reused threads. |
| OTel logs | Logback events travel over OTLP to the receiver's log pipeline. | Debug output is local inspection, not a searchable log database. |
| Metrics | Micrometer records counters/timers and exposes Prometheus text. | `otel.metrics.exporter=none` disables OTel metric export, not Micrometer metrics. |
| Traces | OTel exports server and manual spans over OTLP. | `always_on` is for this demo; production sampling needs deliberate configuration. |
| Actuator | Exposes configured operational endpoints. | Merely adding Actuator does not expose every endpoint. |
| Micrometer | Backend-neutral metric instrumentation. | Independent of Prometheus; the registry supplies its exposition format. |
| Jaeger v2 | Embedded Collector components receive logs/traces; trace storage serves the UI. | Logs go to the configured debug exporter, not trace storage. |

### 2.4 Spans And Context

| Code / Situation | Effect | Common Trap |
| --- | --- | --- |
| `tracer.spanBuilder(...).startSpan()` | Creates a span with the current context as parent by default. | Starting a span does not automatically make it current. |
| `span.makeCurrent()` | Makes the span current within a scope. | Close the scope to restore the previous context. |
| `Span.current()` | Retrieves the current span. | Does not create a span; without context it may be invalid. |
| `setAttribute(...)` | Attaches details to a span. | IDs are useful for trace lookup but should not become metric labels. |
| `recordException(...)` | Adds an exception event. | Recording an exception alone does not set ERROR status. |
| `setStatus(ERROR, ...)` | Marks the span as failed. | A handled exception still needs explicit business-span status. |
| `span.end()` in `finally` | Finishes the span even on failure. | Closing a scope alone does not finish its span. |
| Span context | Trace ID, span ID, flags, trace state, remote indicator. | Parent span ID is recorded separately on span data. |

### 2.5 Counters, Timers, Histograms, And Percentiles

| Topic | Behavior Here | Reasoning / Limit |
| --- | --- | --- |
| Counter | `orders.requests`, incremented once per business attempt in `finally`. | Not a count of every HTTP request; health/metrics routes bypass it. |
| Labels | Only `outcome=success` or `outcome=error` on custom meters. | Bounded cardinality; each distinct label combination creates a time series. |
| Histogram timer | `orders.duration` records elapsed business-flow time and exports buckets. | Timer count already counts observations; the explicit counter is included for learning. |
| Percentile timer | `orders.latency` records the same elapsed duration with local p50/p95. | Separate metric family because Prometheus histograms and summaries differ. |
| p50 / p95 | Approximate median / 95th-percentile latency per outcome. | Not an average; not a guarantee that every future request meets that latency. |
| Local quantiles | Computed per meter and instance over a rolling window. | Cannot average them into a fleet-wide percentile. |
| Histogram buckets | Cumulative counts at fixed upper bounds. | Can aggregate compatible buckets before estimating a percentile. |
| Timer boundary | Covers `placeOrder` work and scoped business logging. | Excludes parts of HTTP handling, payload serialization, and telemetry export. |
| Restart | In-process counters and timers restart with the app. | A backend must account for counter resets when computing rates. |

If you later connect a Prometheus server, compute overall p95 from buckets with:

```promql
histogram_quantile(0.95, sum by (le) (rate(orders_duration_seconds_bucket[5m])))
```

### 2.6 Export, Batching, And Failure Boundaries

| Stage / Failure | Behavior Here | Practical Consequence |
| --- | --- | --- |
| App traces/logs | HTTP/protobuf to `localhost:4318`; SDK batching delays set to one second. | HTTP completion does not mean telemetry is already visible. |
| Receiver | OTLP routes to separate `traces` and `logs` pipelines; receiver batch timeout is one second. | Another batching stage can add delay; check the actual output. |
| Trace destination | In-memory Jaeger storage. | Restart loses stored traces. |
| Log destination | Debug exporter appends detailed received records to `.run/otel-logs.log`. | Format is for inspection; existing output can include previous runs. |
| Console destination | App stdout redirects to `.run/app.log`, overwritten on startup. | A local log line alone does not prove OTLP delivery. |
| Receiver outage | SDK export can fail asynchronously while business requests still succeed. | Inspect exporter errors; this demo has no durable telemetry queue. |
| Ctrl+C | `run.sh` signals and waits for its app and Jaeger processes. | Forced termination or outages can lose queued telemetry; no guaranteed final flush ordering. |

## 3. Interview Scenarios And Tricky Q&A

| No. | Scenario / Question | Expected Reasoning |
| --- | --- | --- |
| 1 | A retry uses the same correlation ID but gets different request and trace IDs. Is propagation broken? | Not if there is no incoming `traceparent`. The business correlation groups attempts; each request starts a new trace and gets a fresh request ID. |
| 2 | Two services share a trace ID but have different span IDs. Is one ID wrong? | The trace groups the execution; each operation needs its own span ID. Inspect parent references to verify the relationship. |
| 3 | A client sends `X-Trace-ID`, but the response has another trace ID. Why? | This app propagates tracing through `traceparent`, not that custom response header. |
| 4 | `traceparent` is absent, yet the filter sees a valid trace ID. Who created it? | The starter created the server span before our filter accessed the current context. |
| 5 | A malformed `traceparent` arrives. Should our filter parse it manually? | The configured OTel propagator validates it and ignores invalid parent context; a new trace can start. |
| 6 | A correlation value contains spaces and changes in the response. Why? | The demo's value-validation rule rejects it and generates a UUID. Header-name matching is unrelated to that regex. |
| 7 | The correlation ID stays the same, so can it prevent duplicate payments? | No. This code uses it only for observability. Idempotency requires its own key handling, persistence, and concurrency rules. |
| 8 | Three code locations mention a response. Did the endpoint send three responses? | The filter adds headers to the servlet response; the record is the payload; ResponseEntity selects status/body for Spring to write into that response. |
| 9 | A later request inherits the previous user's correlation ID in logs. What would you inspect first? | Missing MDC restoration on reused threads, then async context handling. This filter restores the previous map in finally. |
| 10 | Logs written in an executor lose IDs while synchronous logs work. Why? | Thread-local MDC and the active OTel context need propagation to the task. Do not assume they automatically cross every executor boundary. |
| 11 | Child spans appear beside `place-order` instead of beneath it. What is likely missing? | The parent was started but never made current, or context was lost before the child started. |
| 12 | A scope closes successfully, but a span never appears complete. What is missing? | Closing Scope restores context; the span must also be ended, normally in finally. |
| 13 | An exception event appears but the span status is not ERROR. Is that impossible? | No. recordException and setStatus are separate APIs. This service uses both for payment/order failures. |
| 14 | A failed payment produces exception events on payment and order spans. Is that two failed orders? | One failure is recorded at two operation boundaries. The business counter increments once for the order attempt. |
| 15 | The response is HTTP 500, but request/correlation headers remain. How? | The filter added them before invoking the controller, and the handled business failure still returns through the same response. |
| 16 | JSON logs exist, but the receiver has no business record. What do you check? | OTLP exporter/appender settings, receiver connectivity, the logs pipeline, batching delay, and export errors; local output alone proves no delivery. |
| 17 | Traces are visible in Jaeger, but exported logs are absent from its UI. Is OTLP failing? | Not necessarily. This config exports logs to the debug file; Jaeger trace storage and UI do not provide a log search backend. |
| 18 | OTel metrics export is disabled, but `/actuator/prometheus` still works. Why? | Micrometer's Prometheus registry is a separate metric path. Disabling the OTel exporter does not disable it. |
| 19 | Adding request IDs as metric labels makes diagnosis easier initially. What happens under traffic? | Each request creates new series, increasing memory, storage, and query costs. Keep IDs in logs/traces and use bounded metric labels. |
| 20 | Histogram buckets contain different counts. Can you sum all buckets to get requests? | No. Buckets are cumulative and overlap. Use the timer count or the +Inf bucket. |
| 21 | Instance A has p95=100 ms and B has p95=900 ms. Is fleet p95=500 ms? | No. Quantiles cannot be averaged. Aggregate compatible histogram bucket rates and calculate the percentile afterward. |
| 22 | p95 becomes NaN after an idle period while the counter stays positive. Is data corrupted? | The percentile window has no recent observations; the cumulative counter has a different lifetime. Generate traffic and compare windows. |
| 23 | The timer count and explicit counter both show 20. Were 40 orders processed? | Both instruments observed the same 20 attempts; they are not disjoint event counts. |
| 24 | Curl reports higher latency than `orders.duration`. Which measurement is wrong? | Neither necessarily. The business timer excludes networking and parts of HTTP processing and serialization. Compare measurement boundaries. |
| 25 | Export is configured for one second, so a missing record after exactly one second proves failure? | No. Scheduling, receiver batching, and processing add delay. Poll and inspect errors rather than treating a batch setting as a visibility SLA. |
| 26 | The receiver is unavailable, but the order returns HTTP 200. Why no rollback? | Telemetry export is asynchronous and separate from business success; a failed export does not undo the business operation. |
| 27 | A run stops and all old traces disappear. Did the application delete them? | Jaeger uses in-memory storage. Restarting it discards traces independently of app logs. |
| 28 | Repeating a curl with a hardcoded traceparent shows several server spans under one trace. Why? | Reusing that trace ID deliberately joins the same trace; fresh experiments need fresh trace IDs. |
| 29 | A response carries an untrusted correlation value. Can you use it for authorization? | No. Validation limits its syntax, not the caller's authority or identity. It is diagnostic context. |
| 30 | Ctrl+C was pressed, but the last batch is absent. Does finally guarantee remote delivery? | Finally ends spans locally. Delivery also depends on SDK flushing, receiver availability, and shutdown ordering; this runner does not guarantee lossless export. |
