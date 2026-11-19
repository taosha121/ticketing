package com.example.ticketing.integration;

import com.example.ticketing.event.interfaces.dto.CreateEventRequest;
import com.example.ticketing.event.interfaces.dto.TicketInventoryRequest;
import com.example.ticketing.event.interfaces.dto.UpdateEventRequest;
import com.example.ticketing.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
class EventApiTest extends AbstractIntegrationTest {

    @Autowired
    CacheManager cacheManager;

    private String organizerToken;
    private String buyerToken;

    @BeforeEach
    void setUp() throws Exception {
        long ts = System.nanoTime();
        organizerToken = registerAndLogin("event_org_" + ts + "@test.com", "password123", "ORGANIZER");
        buyerToken = registerAndLogin("event_buyer_" + ts + "@test.com", "password123", "BUYER");
    }

    private CreateEventRequest sampleRequest() {
        return new CreateEventRequest(
                "Rock Concert", "Great music", "Madison Square Garden",
                OffsetDateTime.now().plusDays(30), OffsetDateTime.now().plusDays(30).plusHours(3),
                List.of(
                        new TicketInventoryRequest("GA", BigDecimal.valueOf(50), 100),
                        new TicketInventoryRequest("VIP", BigDecimal.valueOf(200), 20)
                )
        );
    }

    @Test
    void createEvent_asOrganizer_returns201WithInventories() throws Exception {
        mockMvc.perform(post("/events")
                .header("Authorization", "Bearer " + organizerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Rock Concert"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.ticketInventories.length()").value(2));
    }

    @Test
    void createEvent_asBuyer_returns403() throws Exception {
        mockMvc.perform(post("/events")
                .header("Authorization", "Bearer " + buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEvent_populatesRedisCache() throws Exception {
        var result = mockMvc.perform(post("/events")
                .header("Authorization", "Bearer " + organizerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Long eventId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Rock Concert"));

        assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();
    }

    @Test
    void updateEvent_evictsCache() throws Exception {
        var createResult = mockMvc.perform(post("/events")
                .header("Authorization", "Bearer " + organizerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        Long eventId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // populate cache
        mockMvc.perform(get("/events/" + eventId)).andExpect(status().isOk());
        assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

        var updateReq = new UpdateEventRequest("Updated Title", "desc", "New Venue",
                OffsetDateTime.now().plusDays(30), OffsetDateTime.now().plusDays(31));
        mockMvc.perform(put("/events/" + eventId)
                .header("Authorization", "Bearer " + organizerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));

        assertThat(cacheManager.getCache("events").get(eventId)).isNull();
    }

    @Test
    void updateEvent_notOwner_returns403() throws Exception {
        var createResult = mockMvc.perform(post("/events")
                .header("Authorization", "Bearer " + organizerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        Long eventId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        String org2Token = registerAndLogin("other_org_" + System.nanoTime() + "@test.com", "password123", "ORGANIZER");

        var updateReq = new UpdateEventRequest("Hacked", "desc", "venue",
                OffsetDateTime.now().plusDays(30), OffsetDateTime.now().plusDays(31));
        mockMvc.perform(put("/events/" + eventId)
                .header("Authorization", "Bearer " + org2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void publishEvent_changesToPublished() throws Exception {
        var createResult = mockMvc.perform(post("/events")
                .header("Authorization", "Bearer " + organizerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        Long eventId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/events/" + eventId + "/publish")
                .header("Authorization", "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }
}
