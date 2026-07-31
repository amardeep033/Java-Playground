package com.example.logging;

import com.example.logging.additivity.AdditivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class LoggingApplication {
    /*
     * static final because we need only one logger per class.
     * The class name also becomes part of the log, so we know where it came from: class=%logger{0}.
     */
    // <logger name="com.example.logging" level="DEBUG" /> means TRACE is blocked for this package.
    // There are two level checks:
    // 1. Logger/package/root level: what log events are allowed to leave the code.
    // 2. Appender/filter level: what each destination accepts, for example INFO-only file.

    private static final Logger log = LoggerFactory.getLogger(LoggingApplication.class);

    public static void main(String[] args) throws InterruptedException {

        // No need to explicitly read logback.xml.
        // Logback auto-loads it when the file is named exactly logback.xml and is present on the runtime classpath.
        // In Maven, src/main/resources/logback.xml is copied to target/classes/logback.xml during compile.

        log.debug("AAAA");
        log.info("BBBB");
        log.warn("CCCC");

        Thread requestThread = new Thread(() -> runRequest("REQ-101"), "request-thread");
        requestThread.start();
        requestThread.join();

        new AdditivityService().showAdditivity();

        try {
            new OrderService().failOrder("ORD-2");
        } catch (IllegalStateException ex) {
            // Correct exception logging: pass the exception object, not only ex.getMessage().
            // This preserves the stack trace, which is usually what production debugging needs.
            log.error("FFFF exception logging demo", ex);
        }
    }

    private static void runRequest(String requestId) {
        try {
            // MDC stores context against the current thread.
            // Because logback.xml has requestId=%X{requestId:-none}, we do not pass requestId in every log message.
            MDC.put("requestId", requestId);
            // MDC.get is useful when you want to read the value manually in code.
            log.debug("Current MDC requestId={}", MDC.get("requestId"));
            log.info("DDDDD");
            // Prefer parameterized logging over "orderId=" + orderId.
            // But remember: expensiveMethod() still runs before log.debug("{}", expensiveMethod()).
            // Use log.isDebugEnabled() around genuinely expensive debug-only work.
            new OrderService().placeOrder("ORD-1");
        } finally {
            // finally is important because threads can be reused; old MDC values must not leak.
            // remove clears one key; clear removes all MDC keys for the current thread.
            MDC.remove("requestId");
            MDC.clear();
        }
    }
}
