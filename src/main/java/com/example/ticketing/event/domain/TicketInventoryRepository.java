package com.example.ticketing.event.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketInventoryRepository extends JpaRepository<TicketInventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketInventory t WHERE t.id = :id")
    Optional<TicketInventory> findByIdForUpdate(@Param("id") Long id);
}
