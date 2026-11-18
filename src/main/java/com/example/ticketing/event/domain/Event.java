package com.example.ticketing.event.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String venue;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketInventory> ticketTypes = new ArrayList<>();

    protected Event() {}

    public Event(Long organizerId, String title, String description,
                 String venue, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        this.organizerId = organizerId;
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void publish() {
        this.status = Status.PUBLISHED;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }

    public void update(String title, String description, String venue,
                       OffsetDateTime startsAt, OffsetDateTime endsAt) {
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public Long getId() { return id; }
    public Long getOrganizerId() { return organizerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVenue() { return venue; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public Status getStatus() { return status; }
    public String getPosterUrl() { return posterUrl; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<TicketInventory> getTicketTypes() { return ticketTypes; }

    public enum Status { DRAFT, PUBLISHED, CANCELLED }
}
