package com.eventrelay.serviceb.delivery;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/internal/v1/deliveries")
public class InternalDeliveryController {
    private final DeliveryStore store;
    public InternalDeliveryController(DeliveryStore store) { this.store = store; }
    @GetMapping("/{id}") public Map<String, Object> details(@PathVariable UUID id) {
        return store.details(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
    }
}
