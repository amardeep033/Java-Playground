package com.example.basiccrud.service;

import com.example.basiccrud.dto.OrderResponse;
import com.example.basiccrud.model.CustomerOrder;
import com.example.basiccrud.repository.OrderRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    //--------------------------------------------------------------------------------------

    //0
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersWithNPlusOne(String status) {
        List<CustomerOrder> orders = orderRepository.findByStatusOrderById(normalizeStatus(status)); //if this was in separate transc fn - and - no transc here -- then LazyInitializationException -- we could do something like spring.jpa.open-in-view=true but it can lead to hide accidental database access

        List<OrderResponse> responses = new ArrayList<>();
        for (CustomerOrder order : orders) {
            // This line touches order.getItems() inside OrderResponse.from(order).
            // Because items are LAZY, this can trigger one extra item query per order: N+1.
            responses.add(OrderResponse.from(order));
        }

        return responses;
    }

    //1
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersUsingJoinFetch(String status) {
        // JOIN FETCH initializes items in the main query, so OrderResponse.from(order) does not trigger item N+1.
        return orderRepository.findByStatusUsingJoinFetch(normalizeStatus(status)).stream()
                .map(OrderResponse::from)
                .toList();
    }

    //2
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersUsingEntityGraph(String status) {
        // @EntityGraph initializes items for this repository method without writing custom JPQL.
        return orderRepository.findWithItemsByStatusOrderById(normalizeStatus(status)).stream()
                .map(OrderResponse::from)
                .toList();
    }

    //3
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersUsingBatchLoad(String status) {
        List<CustomerOrder> orders = orderRepository.findByStatusOrderById(normalizeStatus(status));

        List<OrderResponse> responses = new ArrayList<>();
        for (CustomerOrder order : orders) {
            // Same lazy access as the N+1 demo, but CustomerOrder.items has @BatchSize(size = 10).
            // Hibernate groups lazy collection loads into fewer "where order_id in (...)" queries.
            responses.add(OrderResponse.from(order));
        }

        return responses;
    }

    //--------------------------------------------------------------------------------------

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PLACED";
        }
        return status.trim().toUpperCase();
    }
}
