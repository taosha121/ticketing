package com.example.ticketing.event.interfaces.dto;

import com.example.ticketing.event.domain.TicketType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TicketTypeResponse(
        Long id,
        String name,
        BigDecimal price,
        int totalQuantity,
        int remainingQuantity,
        OffsetDateTime createdAt
) {
    public static TicketTypeResponse from(TicketType tt) {
        return new TicketTypeResponse(
                tt.getId(),
                tt.getName(),
                tt.getPrice(),
                tt.getTotalQuantity(),
                tt.getRemainingQuantity(),
                tt.getCreatedAt()
        );
    }
}
