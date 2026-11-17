package com.example.ticketing.event.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record CreateEventRequest(
        @NotBlank String title,
        String description,
        @NotBlank String venue,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        @NotNull @Size(min = 1) @Valid List<TicketTypeRequest> ticketTypes
) {}
