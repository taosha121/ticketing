package com.example.ticketing.event.interfaces;

import com.example.ticketing.event.application.EventService;
import com.example.ticketing.event.interfaces.dto.CreateEventRequest;
import com.example.ticketing.event.interfaces.dto.EventResponse;
import com.example.ticketing.event.interfaces.dto.UpdateEventRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventResponse> create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CreateEventRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(user.getUsername(), req));
    }

    @GetMapping
    public Page<EventResponse> listPublished(
            @PageableDefault(size = 20, sort = "startsAt") Pageable pageable) {
        return eventService.listPublished(pageable);
    }

    @GetMapping("/{id}")
    public EventResponse getById(@PathVariable Long id) {
        return eventService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public EventResponse update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest req) {
        return eventService.update(user.getUsername(), id, req);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ORGANIZER')")
    public EventResponse publish(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return eventService.publish(user.getUsername(), id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ORGANIZER')")
    public EventResponse cancel(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        return eventService.cancel(user.getUsername(), id);
    }
}
