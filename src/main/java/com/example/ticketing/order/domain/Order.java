package com.example.ticketing.order.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ticket_type_id", nullable = false)
    private Long ticketInventoryId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Order() {}

    public Order(Long userId, Long ticketInventoryId, int quantity, String idempotencyKey) {
        this.userId = userId;
        this.ticketInventoryId = ticketInventoryId;
        this.quantity = quantity;
        this.idempotencyKey = idempotencyKey;
    }

    public void confirm() { this.status = Status.CONFIRMED; }
    public void cancel() { this.status = Status.CANCELLED; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getTicketInventoryId() { return ticketInventoryId; }
    public int getQuantity() { return quantity; }
    public Status getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public enum Status { PENDING, CONFIRMED, CANCELLED }
}
