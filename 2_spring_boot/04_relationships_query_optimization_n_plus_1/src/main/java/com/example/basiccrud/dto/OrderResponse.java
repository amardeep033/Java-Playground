package com.example.basiccrud.dto;

import com.example.basiccrud.model.CustomerOrder;
import com.example.basiccrud.model.OrderItem;
import java.math.BigDecimal;
import java.util.List;

public class OrderResponse {

    private Long id;
    private String orderNumber;
    private String status;
    private Long customerId;
    private String customerName;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;

    public OrderResponse(
            Long id,
            String orderNumber,
            String status,
            Long customerId,
            String customerName,
            List<OrderItemResponse> items,
            BigDecimal totalAmount) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.customerId = customerId;
        this.customerName = customerName;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    public static OrderResponse from(CustomerOrder order) {
        return from(order, order.getItems());
        // This touches LAZY items. In /n-plus-one, that is intentional.
    }

    public static OrderResponse from(CustomerOrder order, List<OrderItem> preloadedItems) {
        List<OrderItemResponse> itemResponses = preloadedItems.stream()
                .map(OrderItemResponse::from)
                .toList();

        BigDecimal total = preloadedItems.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                itemResponses,
                total);
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
