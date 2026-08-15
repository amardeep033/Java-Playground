package com.example.overall.s5beanlifecycle;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// A draft is short-lived state for one order, so each lookup should create a fresh instance.
// In most real applications, a simple domain object would be created with new; prototype scope is useful when each new instance also needs Spring-managed dependencies.
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OrderDraft {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final int draftId = SEQUENCE.incrementAndGet();
    private String itemName;

    public OrderDraft() {
        System.out.println("6. Prototype: created OrderDraft " + draftId);
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    @Override
    public String toString() {
        return "OrderDraft[draftId=" + draftId + ", itemName=" + itemName + "]";
    }
}
