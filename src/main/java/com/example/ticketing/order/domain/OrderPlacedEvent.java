package com.example.ticketing.order.domain;

public record OrderPlacedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        Long ticketInventoryId,
        int quantity
) {}
