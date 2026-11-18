package com.example.ticketing.event.interfaces.dto;

import com.example.ticketing.event.domain.TicketInventory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TicketInventoryResponse(
        Long id,
        String name,
        BigDecimal price,
        int totalQuantity,
        int remainingQuantity,
        OffsetDateTime createdAt
) {
    public static TicketInventoryResponse from(TicketInventory ti) {
        return new TicketInventoryResponse(
                ti.getId(),
                ti.getName(),
                ti.getPrice(),
                ti.getTotalQuantity(),
                ti.getRemainingQuantity(),
                ti.getCreatedAt()
        );
    }
}
