package com.example.ticketing.event.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ticket_inventories")
public class TicketInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "remaining_quantity", nullable = false)
    private int remainingQuantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected TicketInventory() {}

    public TicketInventory(Event event, String name, BigDecimal price, int totalQuantity) {
        this.event = event;
        this.name = name;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
    }

    public void deduct(int quantity) {
        if (remainingQuantity < quantity) {
            throw new IllegalStateException("Insufficient inventory");
        }
        this.remainingQuantity -= quantity;
    }

    public void restore(int quantity) {
        this.remainingQuantity += quantity;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getRemainingQuantity() { return remainingQuantity; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
