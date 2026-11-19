package com.example.ticketing.unit.domain;

import com.example.ticketing.order.domain.Reservation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class ReservationTest {

    private Reservation newReservation() {
        return new Reservation(1L, 1L, 1L, 2, OffsetDateTime.now().plusMinutes(15));
    }

    @Test
    void initialStatus_isActive() {
        assertThat(newReservation().getStatus()).isEqualTo(Reservation.Status.ACTIVE);
    }

    @Test
    void expire_changesStatusToExpired() {
        var res = newReservation();
        res.expire();
        assertThat(res.getStatus()).isEqualTo(Reservation.Status.EXPIRED);
    }

    @Test
    void confirm_changesStatusToConfirmed() {
        var res = newReservation();
        res.confirm();
        assertThat(res.getStatus()).isEqualTo(Reservation.Status.CONFIRMED);
    }
}
