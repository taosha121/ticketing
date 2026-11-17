package com.example.ticketing.event.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record UpdateEventRequest(
        @NotBlank String title,
        String description,
        @NotBlank String venue,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt
) {}
