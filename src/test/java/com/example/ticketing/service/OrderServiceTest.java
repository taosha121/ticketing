package com.example.ticketing.service;

import com.example.ticketing.auth.domain.User;
import com.example.ticketing.auth.domain.UserRepository;
import com.example.ticketing.common.exception.ResourceNotFoundException;
import com.example.ticketing.event.domain.TicketInventory;
import com.example.ticketing.event.domain.TicketInventoryRepository;
import com.example.ticketing.order.application.OrderService;
import com.example.ticketing.order.domain.Order;
import com.example.ticketing.order.domain.OrderRepository;
import com.example.ticketing.order.domain.ReservationRepository;
import com.example.ticketing.order.interfaces.dto.PlaceOrderRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("service")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock TicketInventoryRepository ticketInventoryRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks OrderService orderService;

    private User user(long id, String email) {
        User u = new User(email, "hash", User.Role.BUYER);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    void placeOrder_idempotency_skipsInventoryDeduction() {
        Order existing = new Order(1L, 1L, 2, "key-123");
        when(orderRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existing));

        orderService.placeOrder("user@test.com", "key-123", new PlaceOrderRequest(1L, 2));

        verify(ticketInventoryRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void placeOrder_insufficientInventory_throwsIllegalState() {
        when(orderRepository.findByIdempotencyKey("key-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user(1L, "user@test.com")));
        // inventory has only 1 ticket
        TicketInventory inv = new TicketInventory(null, "GA", BigDecimal.TEN, 1);
        when(ticketInventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() ->
                orderService.placeOrder("user@test.com", "key-new", new PlaceOrderRequest(1L, 5)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void placeOrder_inventoryNotFound_throwsResourceNotFound() {
        when(orderRepository.findByIdempotencyKey("key-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user(1L, "user@test.com")));
        when(ticketInventoryRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.placeOrder("user@test.com", "key-new", new PlaceOrderRequest(99L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_notOwner_throwsAccessDenied() {
        User otherUser = user(2L, "other@test.com");
        Order order = new Order(1L, 1L, 2, "key-abc"); // userId = 1

        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getById("other@test.com", 10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_orderNotFound_throwsResourceNotFound() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user(1L, "user@test.com")));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById("user@test.com", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
