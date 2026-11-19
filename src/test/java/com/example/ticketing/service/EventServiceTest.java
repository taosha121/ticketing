package com.example.ticketing.service;

import com.example.ticketing.auth.domain.User;
import com.example.ticketing.auth.domain.UserRepository;
import com.example.ticketing.common.exception.ResourceNotFoundException;
import com.example.ticketing.common.storage.StorageService;
import com.example.ticketing.event.application.EventService;
import com.example.ticketing.event.domain.Event;
import com.example.ticketing.event.domain.EventRepository;
import com.example.ticketing.event.interfaces.dto.UpdateEventRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("service")
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock UserRepository userRepository;
    @Mock StorageService storageService;

    @InjectMocks EventService eventService;

    private User organizer(long id, String email) {
        User u = new User(email, "hash", User.Role.ORGANIZER);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Event event(long id, long organizerId) {
        Event e = new Event(organizerId, "Test Event", "desc", "Venue",
                OffsetDateTime.now().plusDays(7), OffsetDateTime.now().plusDays(8));
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> eventService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_notOwner_throwsAccessDenied() {
        User org2 = organizer(2L, "org2@test.com");
        Event event = event(1L, 1L); // owned by organizer id=1

        when(userRepository.findByEmail("org2@test.com")).thenReturn(Optional.of(org2));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        var req = new UpdateEventRequest("New Title", "desc", "venue",
                OffsetDateTime.now().plusDays(7), OffsetDateTime.now().plusDays(8));

        assertThatThrownBy(() -> eventService.update("org2@test.com", 1L, req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void publish_ownEvent_changesStatusToPublished() {
        User org = organizer(1L, "org@test.com");
        Event event = event(1L, 1L);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(org));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = eventService.publish("org@test.com", 1L);
        assertThat(response.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void cancel_ownEvent_changesToCancelled() {
        User org = organizer(1L, "org@test.com");
        Event event = event(1L, 1L);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(org));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = eventService.cancel("org@test.com", 1L);
        assertThat(response.status()).isEqualTo("CANCELLED");
    }
}
