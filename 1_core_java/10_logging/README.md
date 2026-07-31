# Java Logging Self Study

This project is intentionally small. Logs are named `AAAA`, `BBBB`, `CCCC`, `DDDDD`, `EEEE`, and `FFFF` so it is easy to compare what appears in the console and what appears in the file.

## Run

```bash
mvn compile
mvn -q exec:java
cat logs/info.log
```

`mvn compile` copies `src/main/resources/logback.xml` to `target/classes/logback.xml`. Logback auto-loads `logback.xml` from the runtime classpath.

## Current Behavior

| Log | Code Level | Source Class | Console Plain? | File JSON? | Why |
| --- | --- | --- | --- | --- | --- |
| `AAAA` | `DEBUG` | `LoggingApplication` | Yes | No | Package/root level allows `DEBUG`, but file accepts only exactly `INFO`. |
| `BBBB` | `INFO` | `LoggingApplication` | Yes | Yes | Console has no filter; file `LevelFilter` accepts `INFO`. |
| `CCCC` | `WARN` | `LoggingApplication` | Yes | No | Console accepts it; file rejects non-`INFO`. |
| `Current MDC requestId=REQ-101` | `DEBUG` | `LoggingApplication` | Yes | No | Shows `MDC.get`, but file rejects `DEBUG`. |
| `DDDDD` | `INFO` | `LoggingApplication` | Yes | Yes | MDC value appears because XML uses `%X{requestId:-none}` and JSON encoder includes MDC. |
| `EEEE orderId=ORD-1` | `INFO` | `OrderService` | Yes | Yes | Shows second class logger and SLF4J `{}` placeholder. |
| `ADDITIVITY` | `INFO` | `AdditivityService` | Yes, special format | No | `additivity=false` sends it only to its own appender, not root appenders. |
| `FFFF exception logging demo` | `ERROR` | `LoggingApplication` | Yes | No | Correct exception logging preserves stack trace; file rejects non-`INFO`. |

## Big Picture

| Thing | Meaning | In This Project |
| --- | --- | --- |
| SLF4J | Interface/API used by Java code | `Logger`, `LoggerFactory`, `MDC` imports |
| Logback | Actual implementation | `logback-classic` dependency and `logback.xml` |
| Log4j2 | Another implementation | Mentioned for comparison; not used here |
| logstash-logback-encoder | JSON encoder for Logback | Makes `logs/info.log` structured JSON |

## Interview Log Flow

This is the flow to explain when asked: "What happens when your Java application writes a log?"

```text
Application
    -> SLF4J API
    -> Logback / Log4j2 implementation
    -> log event enrichment: level, logger name, thread, MDC / trace context
    -> formatting: plain text or structured JSON
    -> async queue / buffering, if configured
    -> destination: stdout, file, or collector agent
    -> centralized logging system
    -> search, alerting, dashboards, debugging
```

| Stage | Interview Explanation | Example in This Project |
| --- | --- | --- |
| Application | Code calls `log.info(...)`, `log.error(...)`, etc. | `log.info("BBBB")` |
| SLF4J API | Code depends on a facade, not directly on Logback classes. | `Logger`, `LoggerFactory` |
| Implementation | Runtime provider decides how logs are processed. | Logback via `logback-classic` |
| Context enrichment | Framework attaches level, class/logger, thread, and MDC values. | `requestId=REQ-101` |
| Structured or plain | Encoder decides output format. | Console plain, file JSON |
| Async/buffering | Optional queue reduces direct I/O work on application thread. | `AsyncAppender` |
| Destination | Log goes to console, file, stdout, or agent. | Console plus `logs/info.log` |
| Collector | In production, agents collect logs from stdout/files. | Fluent Bit, Vector, Filebeat, cloud agent |
| Centralized logging | Logs are stored and indexed. | ELK, Loki, Splunk, CloudWatch |
| Usage | Engineers search by `requestId`, alert on errors, debug incidents. | JSON file contains searchable fields |

One-line interview answer:

```text
My code logs through SLF4J, Logback handles the event using logback.xml, MDC adds request context, encoders format it as plain or JSON, async appenders buffer it, and production collectors ship it to a centralized system for search and alerts.
```

## Pom Dependencies

| Dependency | Why Needed |
| --- | --- |
| `slf4j-api` | Gives the logging API used in code. |
| `logback-classic` | Implements SLF4J at runtime and reads `logback.xml`. |
| `logstash-logback-encoder` | Writes structured JSON logs from Logback. |

## Logback.xml Cheatsheet

| Config Area | What It Controls | Current Example |
| --- | --- | --- |
| `property` | Reusable variable | `LOG_DIR=logs` |
| `ConsoleAppender` | Console destination | Plain text output |
| `RollingFileAppender` | File destination | `logs/info.log` |
| `TimeBasedRollingPolicy` | Daily rotation and retention | `info.%d{yyyy-MM-dd}.log`, `maxHistory=14` |
| `LevelFilter` | Accepts one exact level | Only `INFO` goes to file |
| `LogstashEncoder` | Structured JSON output | File logs are JSON |
| `PatternLayoutEncoder` | Plain text output | Console logs are readable |
| `AsyncAppender` | Queue before writing | Console and file are both async |
| `<logger name="com.example.logging" level="DEBUG" />` | Package-level gate | `TRACE` blocked, `DEBUG+` allowed |
| `additivity="false"` | Stops propagation to parent/root loggers | `AdditivityService` logs only once in a special console format |
| `<root level="DEBUG">` | Default logger and destination routing | Sends allowed events to both appenders |

## LevelFilter vs ThresholdFilter

| Filter | Meaning | Example |
| --- | --- | --- |
| `LevelFilter` | Accept exactly one level | Accept `INFO`, reject `DEBUG/WARN/ERROR` |
| `ThresholdFilter` | Accept that level and higher | Threshold `INFO` accepts `INFO/WARN/ERROR` |

This project uses `LevelFilter` because the file should contain only `INFO`, not warnings or errors.

## Two Level Checks

| Check | Question | Current Answer |
| --- | --- | --- |
| Logger/package/root level | Is this event allowed to leave the code? | `com.example.logging=DEBUG`, so `DEBUG`, `INFO`, `WARN`, `ERROR` pass. |
| Appender/filter level | Does this destination accept the event? | Console accepts all passed events; file accepts only `INFO`. |

## Logger Additivity

By default, a logger sends the event to its own appenders and then the event travels upward to parent/root loggers.

| Setting | Meaning | Result |
| --- | --- | --- |
| Default additivity | Event also propagates to parent/root | Same event may be written by child and root appenders. |
| `additivity="false"` | Stop propagation at this logger | Useful when a package has a dedicated appender and you want to avoid duplicate logs. |

In this project:

```xml
<logger name="com.example.logging.additivity" level="INFO" additivity="false">
    <appender-ref ref="ADDITIVITY_CONSOLE" />
</logger>
```

If `additivity` were true here, `ADDITIVITY` could also reach root appenders and appear in the JSON file if accepted by the file filter.

## MDC

MDC means Mapped Diagnostic Context. It stores values for the current thread.

| Operation | Meaning | In Code |
| --- | --- | --- |
| `MDC.put("requestId", requestId)` | Add context to current thread | Before `DDDDD` |
| `MDC.get("requestId")` | Read current value manually | Debug log before `DDDDD` |
| `MDC.remove("requestId")` | Remove one key | In `finally` |
| `MDC.clear()` | Remove all keys | In `finally` |

`finally` is important because thread pools reuse threads. Without cleanup, the next request on the same thread can accidentally log the previous request id.

## Structured vs Plain Logs

| Format | Best For | Example |
| --- | --- | --- |
| Plain text | Local development and quick reading | Console output |
| JSON structured | Searching, indexing, dashboards, alerts | `logs/info.log` |

## Parameterized Logging and Performance

| Style | Good? | Why |
| --- | --- | --- |
| `log.info("EEEE orderId={}", orderId)` | Yes | SLF4J handles placeholder substitution. |
| `log.debug("orderId=" + orderId)` | Avoid | String concatenation happens even if `DEBUG` is disabled. |
| `log.debug("result={}", expensiveMethod())` | Be careful | Method argument is evaluated before logger call. |
| `if (log.isDebugEnabled()) { ... }` | Use for expensive debug data | Prevents expensive computation when debug is disabled. |

## Exception Logging

| Code | Result |
| --- | --- |
| `log.error("Failed", ex)` | Correct: message plus stack trace. |
| `log.error("Failed: {}", ex.getMessage())` | Weak: message only, stack trace lost. |

## Logback vs Log4j2

| Topic | Logback | Log4j2 |
| --- | --- | --- |
| Config file | `logback.xml` | `log4j2.xml` |
| SLF4J binding | `logback-classic` | `log4j-slf4j2-impl` |
| Async support | `AsyncAppender` | Async appenders and async loggers |
| Common usage | Simple SLF4J projects, Spring Boot default style | High-performance or advanced logging setups |
| Can use both implementations together? | No | No |

Application code should stay on SLF4J so the implementation can change with dependencies/config, not with every logging statement.

## SLF4J Binding Conflicts

| Layer | Example |
| --- | --- |
| Application code | Uses `slf4j-api` |
| One runtime provider | Use Logback OR Log4j2 |
| Bad situation | Multiple providers/bindings accidentally present |
| Symptom | Startup warnings or surprising logging behavior |
| Fix | Keep exactly one SLF4J provider implementation on the classpath |

## SDE2 Scenarios

| Scenario | What You Should Do | Why |
| --- | --- | --- |
| Request failed in production and logs are mixed with other requests | Add `requestId` or `traceId` using MDC | Lets you search all logs for one request flow. |
| Console is readable locally but hard to search in production | Keep console plain and ship JSON file/stdout in servers | Humans read plain; machines index JSON. |
| File has too many warnings/errors but requirement says audit only successful business events | Use `LevelFilter` or a dedicated logger/appender | Destination should match the purpose of the log. |
| Logging slows down request latency | Wrap slow destinations with `AsyncAppender` | Application thread queues log events instead of writing directly. |
| Logs grow forever on disk | Add rolling policy and retention | Prevents disk-full incidents. |
| Sensitive data appears in logs | Mask or avoid logging PII/secrets | Logs are widely copied and retained. |
| Same log line appears twice | Check logger additivity and appender references | Child logger and root logger may both write the same event. |
| Debug log builds a huge object even when debug is off | Guard with `log.isDebugEnabled()` | Method arguments are evaluated before the logger call. |

## Tricky Interview QA

| Question | Strong Answer |
| --- | --- |
| Why `private static final Logger log`? | One logger per class is enough, `final` prevents reassignment, and the class name becomes part of the log source. |
| Does SLF4J write logs by itself? | No. SLF4J is the facade/API. Logback or Log4j2 is the runtime implementation. |
| Why did logs appear after deleting `src/main/resources/logback.xml`? | Because `target/classes/logback.xml` may still exist, or Logback may fall back to its default console config. Run `mvn clean compile` to verify. |
| Why can `DEBUG` show on console but not in file? | Logger level allows it, but the file appender has a filter that accepts only `INFO`. |
| Why is `LevelFilter` different from threshold behavior? | `LevelFilter` can accept exactly one level. A threshold accepts that level and everything more severe. |
| Why clean MDC in `finally`? | Exceptions can skip normal cleanup, and reused threads can leak old context into the next request. |
| Does MDC automatically move to child threads? | Not reliably. MDC is thread-local; for executors you usually copy/restore context explicitly. |
| What happens if async queue is full? | Depending on settings, events can block or be discarded. This is why queue size and discarding rules matter. |
| Should every log be JSON? | Not always. Plain is convenient locally; JSON is better for production search and alerting. |
| Should you log exceptions with `ex.getMessage()` only? | Usually no. Pass the exception object to the logger so stack trace is preserved: `log.error("Failed", ex)`. |
| What is logger additivity? | Propagation from a child logger to parent/root loggers. Disable it with `additivity="false"` when a package has its own appender and should not also go to root. |
| Can parameterized logging skip expensive method calls? | No. It avoids unnecessary message formatting, but method arguments are evaluated first. Guard expensive debug-only work with `isDebugEnabled()`. |
| What happens if both Logback and Log4j2 SLF4J providers are present? | You can get startup warnings or unexpected provider selection. Keep one runtime implementation. |
