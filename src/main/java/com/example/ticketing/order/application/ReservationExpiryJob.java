package com.example.ticketing.order.application;

import com.example.ticketing.order.domain.Reservation;
import com.example.ticketing.order.domain.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final ReservationRepository reservationRepository;
    private final OrderService orderService;

    public ReservationExpiryJob(ReservationRepository reservationRepository,
                                OrderService orderService) {
        this.reservationRepository = reservationRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.expiry-delay-ms:60000}")
    public void run() {
        List<Reservation> expired = reservationRepository
                .findByStatusAndExpiresAtBefore(Reservation.Status.ACTIVE, OffsetDateTime.now());

        if (expired.isEmpty()) return;

        log.info("Processing {} expired reservation(s)", expired.size());
        for (Reservation r : expired) {
            try {
                orderService.expireReservation(r.getId());
            } catch (Exception e) {
                log.error("Failed to expire reservation={}", r.getId(), e);
            }
        }
    }
}
