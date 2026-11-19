package com.example.ticketing.integration;

import com.example.ticketing.auth.domain.User;
import com.example.ticketing.auth.domain.UserRepository;
import com.example.ticketing.event.domain.Event;
import com.example.ticketing.event.domain.EventRepository;
import com.example.ticketing.event.domain.TicketInventory;
import com.example.ticketing.event.domain.TicketInventoryRepository;
import com.example.ticketing.order.application.OrderService;
import com.example.ticketing.order.interfaces.dto.PlaceOrderRequest;
import com.example.ticketing.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@Tag("integration")
class ConcurrentOrderTest extends AbstractIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired UserRepository userRepository;
    @Autowired EventRepository eventRepository;
    @Autowired TicketInventoryRepository ticketInventoryRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void concurrentOrders_onlyInventoryCountSucceed() throws InterruptedException {
        int totalInventory = 5;
        int totalThreads = 10;

        List<User> savedUsers = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            String email = "conc_" + System.nanoTime() + "_" + i + "@test.com";
            savedUsers.add(userRepository.save(
                    new User(email, passwordEncoder.encode("pass"), User.Role.BUYER)));
        }

        Event event = eventRepository.save(new Event(
                savedUsers.get(0).getId(), "Concurrent Test", null, "Venue",
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(2)));

        TicketInventory inventory = ticketInventoryRepository.save(
                new TicketInventory(event, "GA", BigDecimal.valueOf(50), totalInventory));

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalThreads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);
        for (int i = 0; i < totalThreads; i++) {
            final String email = savedUsers.get(i).getEmail();
            pool.submit(() -> {
                try {
                    startGate.await();
                    orderService.placeOrder(email, UUID.randomUUID().toString(),
                            new PlaceOrderRequest(inventory.getId(), 1));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(successCount.get()).isEqualTo(totalInventory);
        assertThat(failCount.get()).isEqualTo(totalThreads - totalInventory);

        TicketInventory updated = ticketInventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(updated.getRemainingQuantity()).isZero();
    }
}
