package com.example.ticketing.integration;

import com.example.ticketing.event.domain.TicketInventory;
import com.example.ticketing.event.domain.TicketInventoryRepository;
import com.example.ticketing.event.interfaces.dto.CreateEventRequest;
import com.example.ticketing.event.interfaces.dto.TicketInventoryRequest;
import com.example.ticketing.order.application.OrderService;
import com.example.ticketing.order.domain.ReservationRepository;
import com.example.ticketing.order.interfaces.dto.PlaceOrderRequest;
import com.example.ticketing.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
class OrderApiTest extends AbstractIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired TicketInventoryRepository ticketInventoryRepository;

    private String organizerToken;
    private String buyerToken;
    private Long eventId;
    private Long inventoryId;

    @BeforeEach
    void setUp() throws Exception {
        long ts = System.nanoTime();
        organizerToken = registerAndLogin("order_org_" + ts + "@test.com", "password123", "ORGANIZER");
        buyerToken = registerAndLogin("order_buyer_" + ts + "@test.com", "password123", "BUYER");

        var createReq = new CreateEventRequest(
                "Order Test Event", null, "Venue",
                OffsetDateTime.now().plusDays(10), OffsetDateTime.now().plusDays(10).plusHours(2),
                List.of(new TicketInventoryRequest("GA", BigDecimal.valueOf(50), 10))
        );
        var result = mockMvc.perform(post("/events")
                .header("Authorization", "Bearer " + organizerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        eventId = node.get("id").asLong();
        inventoryId = node.get("ticketInventories").get(0).get("id").asLong();

        mockMvc.perform(post("/events/" + eventId + "/publish")
                .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk());
    }

    @Test
    void placeOrder_success_decreasesInventory() throws Exception {
        mockMvc.perform(post("/orders")
                .header("Authorization", "Bearer " + buyerToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PlaceOrderRequest(inventoryId, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.quantity").value(2));

        // GET event — cache is cold because publish() evicted it, so reads from DB
        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketInventories[0].remainingQuantity").value(8));
    }

    @Test
    void placeOrder_idempotency_sameKeyTwice_onlyOneOrder() throws Exception {
        String key = UUID.randomUUID().toString();

        var result1 = mockMvc.perform(post("/orders")
                .header("Authorization", "Bearer " + buyerToken)
                .header("X-Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PlaceOrderRequest(inventoryId, 1))))
                .andExpect(status().isCreated())
                .andReturn();

        var result2 = mockMvc.perform(post("/orders")
                .header("Authorization", "Bearer " + buyerToken)
                .header("X-Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PlaceOrderRequest(inventoryId, 1))))
                .andExpect(status().isCreated())
                .andReturn();

        Long id1 = objectMapper.readTree(result1.getResponse().getContentAsString()).get("id").asLong();
        Long id2 = objectMapper.readTree(result2.getResponse().getContentAsString()).get("id").asLong();
        assertThat(id1).isEqualTo(id2);

        // Inventory should only have been deducted once
        TicketInventory inv = ticketInventoryRepository.findById(inventoryId).orElseThrow();
        assertThat(inv.getRemainingQuantity()).isEqualTo(9);
    }

    @Test
    void placeOrder_exceedsInventory_returns422() throws Exception {
        mockMvc.perform(post("/orders")
                .header("Authorization", "Bearer " + buyerToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PlaceOrderRequest(inventoryId, 999))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getOrder_notOwner_returns403() throws Exception {
        var orderResult = mockMvc.perform(post("/orders")
                .header("Authorization", "Bearer " + buyerToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PlaceOrderRequest(inventoryId, 1))))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();
        String anotherBuyerToken = registerAndLogin(
                "another_buyer_" + System.nanoTime() + "@test.com", "password123", "BUYER");

        mockMvc.perform(get("/orders/" + orderId)
                .header("Authorization", "Bearer " + anotherBuyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void expireReservation_restoresInventoryAndCancelsOrder() throws Exception {
        var orderResult = mockMvc.perform(post("/orders")
                .header("Authorization", "Bearer " + buyerToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PlaceOrderRequest(inventoryId, 3))))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        // Find the reservation for this order
        var reservation = reservationRepository.findAll().stream()
                .filter(r -> r.getOrderId().equals(orderId))
                .findFirst().orElseThrow();

        orderService.expireReservation(reservation.getId());

        // Check inventory directly (bypass HTTP cache)
        TicketInventory inv = ticketInventoryRepository.findById(inventoryId).orElseThrow();
        assertThat(inv.getRemainingQuantity()).isEqualTo(10);

        // Order status via HTTP (no cache on orders)
        mockMvc.perform(get("/orders/" + orderId)
                .header("Authorization", "Bearer " + buyerToken))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
