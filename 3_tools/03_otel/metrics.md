# Actuator And Metrics: How To Read `/actuator/prometheus`

Start the app with `./run.sh`, then open
[http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus).
This page is a snapshot of exported metric values, not a graph or a request log.

## 1. Which Actuator URL Should I Open?

| URL | What to read there |
| --- | --- |
| [/actuator/health](http://localhost:8080/actuator/health) | Application health status. |
| [/actuator/metrics](http://localhost:8080/actuator/metrics) | Available meter names, such as `orders.requests` and `jvm.memory.used`. |
| [/actuator/metrics/orders.requests](http://localhost:8080/actuator/metrics/orders.requests) | JSON measurements and available tags for the order counter. |
| [/actuator/metrics/orders.requests?tag=outcome:success](http://localhost:8080/actuator/metrics/orders.requests?tag=outcome:success) | Counter filtered to successful orders. |
| [/actuator/prometheus](http://localhost:8080/actuator/prometheus) | Metrics formatted as text for Prometheus to scrape. Includes buckets and local quantiles. |

The app enables these endpoints with:

```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
```

```text
Actuator + Micrometer binders → record JVM/system/framework metrics
OrderService                 → records our custom order metrics
                                    ↓
                               MeterRegistry
                                    ↓
                          Prometheus registry
                                    ↓
                          /actuator/prometheus
```

Actuator supplies operational endpoints and metric auto-configuration; Micrometer
records the measurements. The Prometheus registry formats them. A separate
Prometheus server is not required to read this page, but is needed for PromQL
queries and stored metric history in the usual Prometheus setup.

## 2. Generate Traffic Before Reading The Page

```bash
BASE=http://localhost:8080

curl -i "$BASE/order"
curl -i "$BASE/order"
curl -i "$BASE/order?fail=true"

# Show only our custom metrics, including their HELP/TYPE lines.
curl -s "$BASE/actuator/prometheus" | rg '^(# (HELP|TYPE) )?orders_'
```

Our meters are created on first use. A fresh app needs an order request before
`orders_` values appear. Earlier requests count too; the examples below are
illustrative, not exact output guaranteed after these commands.

## 3. Read One Metric Line

```text
# HELP orders_requests_total Number of order attempts
# TYPE orders_requests_total counter
orders_requests_total{outcome="success"} 2.0
```

| Part | How to read it |
| --- | --- |
| `# HELP ...` | Description, when supplied by the instrument. Wording may differ or be blank here. |
| `# TYPE ... counter` | Metric family type. |
| `orders_requests_total` | Exported metric name. |
| `{outcome="success"}` | Labels selecting one particular time series. |
| `2.0` | Current numeric value. For this counter, two events; `.0` does not mean half-events are involved. |

Same name + different label values = different series. Name and labels identify
the series; the trailing number is its value. This is the Prometheus text
[exposition format](https://prometheus.io/docs/instrumenting/exposition_formats/).

### Java Names Versus Exported Names

| Name used in code / Actuator JSON | Name seen in Prometheus text |
| --- | --- |
| `orders.requests` Counter | `orders_requests_total` |
| `orders.duration` Timer with buckets | `orders_duration_seconds_bucket`, `_count`, `_sum` |
| `orders.latency` Timer with local quantiles | `orders_latency_seconds{quantile="..."}`, plus `_count` and `_sum` |

Use dotted names in `/actuator/metrics/<name>` and property keys. Use exported
names when reading scrape output or writing PromQL.

## 4. Counter: How Many Orders?

```text
# TYPE orders_requests_total counter
orders_requests_total{outcome="success"} 2
orders_requests_total{outcome="error"} 1
```

Read this as **two successful attempts and one failed attempt** since these
in-process counters started. It is not two requests per second.

In `OrderService`:

```java
meters.counter("orders.requests", "outcome", outcome).increment();
```

| Question | Answer for this app |
| --- | --- |
| Does refreshing the metrics page increment this counter? | No. Only `placeOrder` increments it. |
| Does `/actuator/health` count as an order? | No. |
| Does a simulated payment failure count? | Yes, under `outcome="error"`; increment happens in `finally`. |
| Why use only success/error labels? | Two bounded values; request IDs as labels would create new series per request. |
| Does restarting the app preserve the count? | No; these meters live in the JVM. |

## 5. Histogram: How Long Did Orders Take?

Your original example, with selected buckets:

```text
# TYPE orders_duration_seconds histogram
orders_duration_seconds_bucket{outcome="success",le="0.20132659"} 0
orders_duration_seconds_bucket{outcome="success",le="0.223696211"} 2
orders_duration_seconds_bucket{outcome="success",le="0.246065832"} 2
orders_duration_seconds_bucket{outcome="success",le="2.0"} 2
orders_duration_seconds_bucket{outcome="success",le="+Inf"} 2
orders_duration_seconds_count{outcome="success"} 2
orders_duration_seconds_sum{outcome="success"} 0.42312697
```

### `le` Is The Inclusive Upper Bound

`le` means **less than or equal to**. The number after the labels is a
**count of observations**, not a duration.

| Bucket | Read it as |
| --- | --- |
| `le="0.20132659"} 0` | Zero successful orders took at most 201.32659 ms. |
| `le="0.223696211"} 2` | Two successful orders took at most 223.696211 ms. |
| `le="0.246065832"} 2` | Those same two orders also took at most 246.065832 ms. |
| `le="2.0"} 2` | Two orders took at most 2 seconds. |
| `le="+Inf"} 2` | Two orders in total, including any duration above the configured maximum. |

### Buckets Are Cumulative, Not Separate Boxes

One 210 ms order increments every bucket whose upper bound is at least 210 ms.
Do not add all the bucket counts together: you would count the same order repeatedly.

From the example:

```text
Count in (201.32659 ms, 223.696211 ms]
= bucket(223.696211 ms) - bucket(201.32659 ms)
= 2 - 0
= 2 orders
```

So both observations fall in that interval. The buckets do not reveal each
individual duration. See Prometheus's explanation of
[cumulative histogram buckets](https://prometheus.io/docs/practices/histograms/).

### `_count`, `_sum`, And Average

```text
_count = 2 observations
_sum   = 0.42312697 seconds across those observations

Average = _sum / _count
        = 0.42312697 / 2
        = 0.211563485 seconds
        ≈ 211.56 ms
```

That is the average for the selected label set, not p50 or p95. For a valid
histogram snapshot, the `+Inf` bucket count matches `_count` for the same labels.

### Why These Bounds, And Why Seconds?

```properties
management.metrics.distribution.percentiles-histogram.orders.duration=true
management.metrics.distribution.minimum-expected-value.orders.duration=1ms
management.metrics.distribution.maximum-expected-value.orders.duration=2s
```

| Setting / Unit | What it means here |
| --- | --- |
| Histogram enabled | Publish buckets for the durations recorded by the Timer. |
| 1 ms to 2 s | Expected range used when choosing finite bucket boundaries. It does not reject or cap observed durations. |
| `seconds` in exported names | Prometheus registry exports Timer durations in seconds even when configuration uses ms. Multiply by 1,000 for ms. |
| A duration above 2 seconds | Still contributes to `_sum`, `_count`, and `+Inf`, but not a finite bucket below that duration. |
| Odd boundaries such as `0.223696211` | Generated bucket boundaries; they need not be round human-selected values. |

Your rough `1ms, 2ms, 4ms, ... 2s` sequence is a useful illustration of increasing
bounds, not the actual generated list. Read the `le` labels in the live output.

## 6. Local p50 And p95: Read The Value As Time

Illustrative output:

```text
# TYPE orders_latency_seconds summary
orders_latency_seconds{outcome="success",quantile="0.5"} 0.210
orders_latency_seconds{outcome="success",quantile="0.95"} 0.230
```

| Line | Meaning |
| --- | --- |
| `quantile="0.5"} 0.210` | Estimated local p50 is 210 ms. |
| `quantile="0.95"} 0.230` | Estimated local p95 is 230 ms. |

Unlike a bucket line, the trailing value here is **seconds**, not a count.
These estimates cover a rolling window, separately for each outcome. With no
recent observations, values may become NaN. More observations give a more useful
picture than two requests.

We record the same elapsed duration under two names: `orders.duration` publishes
histogram buckets; `orders.latency` publishes local quantiles. The current
Prometheus registry favors histograms and omits local quantiles when both are
configured on one meter. See [Micrometer's histogram/summary behavior](https://github.com/micrometer-metrics/micrometer/wiki/1.13-Migration-Guide#histogram-vs-summary).

Local quantiles cannot be averaged across instances. Histogram buckets support
aggregation before percentile estimation.

## 7. Other Metrics On The Page: Where Did They Come From?

You did not write all these instruments in `OrderService`. Spring Boot
[auto-configures Micrometer binders](https://docs.spring.io/spring-boot/3.5/reference/actuator/metrics.html)
for supported JVM, system, and framework metrics. Exact availability depends on
components and configuration.

```text
Spring Boot Actuator + Micrometer
    |
    +-- JVM instrumentation
    |     +-- Memory, garbage collection, threads, classes
    |
    +-- System/process instrumentation
    |     +-- CPU, disk, process uptime
    |
    +-- Framework instrumentation
    |     +-- HTTP requests, supported executors, container-specific metrics
    |
    +-- Our explicit instrumentation
          +-- Order counter and duration/latency timers
```

| Example exported metric | How to read it |
| --- | --- |
| `jvm_memory_used_bytes{area="heap",id="..."}` | Current used bytes in the selected memory pool. A gauge can go up or down. |
| `jvm_threads_live_threads` | Current live JVM thread count. |
| `process_cpu_usage` | Recent process CPU usage as a fraction; 0.25 means 25% of the processor capacity reported by the JVM metric. |
| `process_uptime_seconds` | Process uptime in seconds. |
| `http_server_requests_seconds_count{method="GET",uri="/order",status="200",...}` | Completed matching HTTP requests. Labels shown here are illustrative. |
| `orders_requests_total{outcome="success"}` | Our explicit business counter, independent of the HTTP timer. |

The HTTP timer covers the server request; our business timer covers only the
`placeOrder` section. Their durations and counts need not match. Other routes
contribute HTTP metrics without calling `placeOrder`. Tomcat-specific metrics
may require additional container configuration; check what is actually exposed.

```bash
curl -s "$BASE/actuator/prometheus" | rg '^(jvm_|process_|system_|disk_|http_server_)'
```

## 8. Snapshot Versus History And Graphs

Refreshing the endpoint gives another snapshot. It does not store previous
snapshots or execute PromQL. Jaeger displays traces, not these metric graphs.

If you later configure a Prometheus server to scrape this app, these queries can
run there, once enough samples exist:

```promql
# Successful orders per second, accounting for counter resets.
rate(orders_requests_total{outcome="success"}[5m])

# Mean successful order latency in ms over five minutes.
1000 * rate(orders_duration_seconds_sum{outcome="success"}[5m])
     / rate(orders_duration_seconds_count{outcome="success"}[5m])

# Estimated p95 latency in ms across outcomes/instances with compatible buckets.
1000 * histogram_quantile(
  0.95,
  sum by (le) (rate(orders_duration_seconds_bucket[5m]))
)
```

Read any unfamiliar line in this order: **name → type → labels → unit → value →
measurement window**. For this app, start with `orders_`, then compare the JVM
and HTTP metrics once those lines make sense.
