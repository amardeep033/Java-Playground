package com.example.basiccrud.repository;

import com.example.basiccrud.model.CustomerOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey);

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);
}