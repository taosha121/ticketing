package com.example.ticketing.event.application;

import com.example.ticketing.auth.domain.UserRepository;
import com.example.ticketing.common.exception.ResourceNotFoundException;
import com.example.ticketing.common.storage.StorageService;
import com.example.ticketing.event.domain.Event;
import com.example.ticketing.event.domain.EventRepository;
import com.example.ticketing.event.domain.TicketInventory;
import com.example.ticketing.event.interfaces.dto.CreateEventRequest;
import com.example.ticketing.event.interfaces.dto.EventResponse;
import com.example.ticketing.event.interfaces.dto.UpdateEventRequest;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        StorageService storageService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional
    public EventResponse create(String organizerEmail, CreateEventRequest req) {
        Long organizerId = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();

        Event event = new Event(organizerId, req.title(), req.description(),
                req.venue(), req.startsAt(), req.endsAt());

        req.ticketInventories().forEach(tt ->
                event.getTicketInventories().add(new TicketInventory(event, tt.name(), tt.price(), tt.totalQuantity()))
        );

        return EventResponse.from(eventRepository.save(event));
    }

    @Cacheable(value = "events", key = "#id")
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

    @CacheEvict(value = "events", key = "#id")
    @Transactional
    public EventResponse update(String organizerEmail, Long id, UpdateEventRequest req) {
        Event event = findOwnEvent(organizerEmail, id);
        event.update(req.title(), req.description(), req.venue(), req.startsAt(), req.endsAt());
        return EventResponse.from(eventRepository.save(event));
    }

    @CacheEvict(value = "events", key = "#id")
    @Transactional
    public EventResponse publish(String organizerEmail, Long id) {
        Event event = findOwnEvent(organizerEmail, id);
        event.publish();
        return EventResponse.from(eventRepository.save(event));
    }

    @CacheEvict(value = "events", key = "#id")
    @Transactional
    public EventResponse cancel(String organizerEmail, Long id) {
        Event event = findOwnEvent(organizerEmail, id);
        event.cancel();
        return EventResponse.from(eventRepository.save(event));
    }

    @CacheEvict(value = "events", key = "#id")
    @Transactional
    public EventResponse uploadPoster(String organizerEmail, Long id, MultipartFile file) {
        Event event = findOwnEvent(organizerEmail, id);
        String url = storageService.store(file);
        event.setPosterUrl(url);
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
