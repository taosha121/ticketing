package com.example.ticketing.event.interfaces.dto;

import com.example.ticketing.event.domain.Event;

import java.time.OffsetDateTime;
import java.util.List;

public record EventResponse(
        Long id,
        Long organizerId,
        String title,
        String description,
        String venue,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String status,
        String posterUrl,
        OffsetDateTime createdAt,
        List<TicketTypeResponse> ticketTypes
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getOrganizerId(),
                event.getTitle(),
                event.getDescription(),
                event.getVenue(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getStatus().name(),
                event.getPosterUrl(),
                event.getCreatedAt(),
                event.getTicketTypes().stream().map(TicketTypeResponse::from).toList()
        );
    }
}
