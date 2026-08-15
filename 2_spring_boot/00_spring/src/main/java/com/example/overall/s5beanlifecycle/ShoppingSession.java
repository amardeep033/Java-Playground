package com.example.overall.s5beanlifecycle;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

// In a web application, Spring creates one target instance for each HTTP session.
// Different requests in the same session therefore access the same session-scoped target.
@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ShoppingSession {
    private final List<String> items = new ArrayList<>();

    public void addItem(String itemName) {
        items.add(itemName);
    }

    public List<String> getItems() {
        return List.copyOf(items);
    }
}
