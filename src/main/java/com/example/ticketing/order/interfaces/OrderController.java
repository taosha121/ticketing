package com.example.ticketing.order.interfaces;

import com.example.ticketing.order.application.OrderService;
import com.example.ticketing.order.interfaces.dto.OrderResponse;
import com.example.ticketing.order.interfaces.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal UserDetails user,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(user.getUsername(), idempotencyKey, req));
    }

    @GetMapping("/{id}")
    public OrderResponse getById(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return orderService.getById(user.getUsername(), id);
    }
}
