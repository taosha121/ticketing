package com.example.ticketing.event.application;

import com.example.ticketing.auth.domain.UserRepository;
import com.example.ticketing.event.domain.Event;
import com.example.ticketing.event.domain.EventRepository;
import com.example.ticketing.event.domain.TicketType;
import com.example.ticketing.event.interfaces.dto.CreateEventRequest;
import com.example.ticketing.event.interfaces.dto.EventResponse;
import com.example.ticketing.event.interfaces.dto.UpdateEventRequest;
import com.example.ticketing.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EventResponse create(String organizerEmail, CreateEventRequest req) {
        Long organizerId = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();

        Event event = new Event(organizerId, req.title(), req.description(),
                req.venue(), req.startsAt(), req.endsAt());

        req.ticketTypes().forEach(tt ->
                event.getTicketTypes().add(new TicketType(event, tt.name(), tt.price(), tt.totalQuantity()))
        );

        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public EventResponse getById(Long id) {
        return eventRepository.findById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listPublished(Pageable pageable) {
        return eventRepository.findByStatus(Event.Status.PUBLISHED, pageable)
                .map(EventResponse::from);
    }

    @Transactional
    public EventResponse update(String organizerEmail, Long id, UpdateEventRequest req) {
        Event event = findOwnEvent(organizerEmail, id);
        event.update(req.title(), req.description(), req.venue(), req.startsAt(), req.endsAt());
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse publish(String organizerEmail, Long id) {
        Event event = findOwnEvent(organizerEmail, id);
        event.publish();
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse cancel(String organizerEmail, Long id) {
        Event event = findOwnEvent(organizerEmail, id);
        event.cancel();
        return EventResponse.from(eventRepository.save(event));
    }

    private Event findOwnEvent(String organizerEmail, Long id) {
        Long organizerId = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));

        if (!event.getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("Not your event");
        }
        return event;
    }
}
