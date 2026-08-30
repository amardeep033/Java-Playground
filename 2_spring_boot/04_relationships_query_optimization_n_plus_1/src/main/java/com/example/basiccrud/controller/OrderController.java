package com.example.basiccrud.controller;

import com.example.basiccrud.dto.OrderResponse;
import com.example.basiccrud.service.OrderQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderQueryService orderQueryService;

    public OrderController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    // default -- making use of JoinFetch
    @GetMapping
    public List<OrderResponse> getOrders(
            @RequestParam(required = false, defaultValue = "PLACED") String status) {
        return orderQueryService.getOrdersUsingJoinFetch(status);
    }

    //----------------------------------------------------------------------------------------

    //0
    @GetMapping("/n-plus-one")
    public List<OrderResponse> getOrdersWithNPlusOne(
            @RequestParam(required = false, defaultValue = "PLACED") String status) {
        return orderQueryService.getOrdersWithNPlusOne(status);
        // Watch SQL logs: this intentionally demonstrates the problem.
    }

    //1
    @GetMapping("/join-fetch")
    public List<OrderResponse> getOrdersUsingJoinFetch(
            @RequestParam(required = false, defaultValue = "PLACED") String status) {
        return orderQueryService.getOrdersUsingJoinFetch(status);
    }

    //2
    @GetMapping("/entity-graph")
    public List<OrderResponse> getOrdersUsingEntityGraph(
            @RequestParam(required = false, defaultValue = "PLACED") String status) {
        return orderQueryService.getOrdersUsingEntityGraph(status);
    }

    //3
    @GetMapping("/batch")
    public List<OrderResponse> getOrdersUsingBatchLoad(
            @RequestParam(required = false, defaultValue = "PLACED") String status) {
        return orderQueryService.getOrdersUsingBatchLoad(status);
    }
}
