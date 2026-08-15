package com.example.overall.s5beanlifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

// This singleton service also owns one simulated long-lived connection.
// @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON) could be added explicitly, but it would be redundant here.
@Service
public class OrderService {
    private final ObjectProvider<OrderDraft> orderDraftProvider;
    private boolean connectionOpen;

    public OrderService(ObjectProvider<OrderDraft> orderDraftProvider) {
        this.orderDraftProvider = orderDraftProvider;
        System.out.println("2. Constructor: OrderService created");
    }

    @PostConstruct
    public void connect() {
        // In this simplified example, setting the flag could be done in the constructor itself.
        // In real applications, use @PostConstruct for initialization that must run after dependency injection, such as validating configuration, warming a cache, opening a client or connection, or starting a managed worker.
        connectionOpen = true;
        System.out.println("3. @PostConstruct: shared order connection opened");
    }

    public void placeOrder(String itemName) {
        if (!connectionOpen) {
            throw new IllegalStateException("Order connection is closed");
        }

        // Directly injecting a prototype into this singleton would inject only one instance.
        // ObjectProvider asks Spring for a fresh prototype every time this method runs.
        OrderDraft orderDraft = orderDraftProvider.getObject();
        orderDraft.setItemName(itemName);
        System.out.println("7. Saved " + orderDraft + " using the shared connection");
    }

    @PreDestroy
    public void disconnect() {
        // In this simplified example, cleanup only changes a flag. In real applications, use @PreDestroy to close clients or connections, stop executors or worker threads, flush buffered data, and release other resources owned by the bean.
        connectionOpen = false;
        System.out.println("10. @PreDestroy: shared order connection closed");
    }
}
