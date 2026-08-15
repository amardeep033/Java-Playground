package com.example.overall.s5beanlifecycle;

import java.util.UUID;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// In a web application, Spring creates one target instance for each HTTP request.
// The proxy allows this shorter-lived bean to be injected into longer-lived singleton beans safely.
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestOrderContext {
    private final String requestId = UUID.randomUUID().toString();

    public String getRequestId() {
        return requestId;
    }
}
