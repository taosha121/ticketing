package com.example.ticketing.order.interfaces.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
        @NotNull Long ticketInventoryId,
        @Min(1) int quantity
) {}
