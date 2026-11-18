package com.example.ticketing.order.interfaces.dto;

import com.example.ticketing.order.domain.Order;

import java.time.OffsetDateTime;

public record OrderResponse(
        Long id,
        Long userId,
        Long ticketInventoryId,
        int quantity,
        String status,
        String idempotencyKey,
        OffsetDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTicketInventoryId(),
                order.getQuantity(),
                order.getStatus().name(),
                order.getIdempotencyKey(),
                order.getCreatedAt()
        );
    }
}
