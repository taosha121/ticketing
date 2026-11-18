package com.example.ticketing.order.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ticket_type_id", nullable = false)
    private Long ticketInventoryId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Reservation() {}

    public Reservation(Long orderId, Long userId, Long ticketInventoryId,
                       int quantity, OffsetDateTime expiresAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.ticketInventoryId = ticketInventoryId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
    }

    public void confirm() { this.status = Status.CONFIRMED; }
    public void expire() { this.status = Status.EXPIRED; }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Long getTicketInventoryId() { return ticketInventoryId; }
    public int getQuantity() { return quantity; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public Status getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public enum Status { ACTIVE, CONFIRMED, EXPIRED }
}
