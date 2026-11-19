package com.example.ticketing.unit.domain;

import com.example.ticketing.event.domain.TicketInventory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class TicketInventoryTest {

    private TicketInventory newInventory(int quantity) {
        return new TicketInventory(null, "GA", BigDecimal.valueOf(100), quantity);
    }

    @Test
    void deduct_reducesRemainingQuantity() {
        var inv = newInventory(10);
        inv.deduct(3);
        assertThat(inv.getRemainingQuantity()).isEqualTo(7);
    }

    @Test
    void deduct_exactStock_succeeds() {
        var inv = newInventory(5);
        inv.deduct(5);
        assertThat(inv.getRemainingQuantity()).isZero();
    }

    @Test
    void deduct_exceedsStock_throwsIllegalState() {
        var inv = newInventory(2);
        assertThatThrownBy(() -> inv.deduct(3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    void restore_addsQuantityBack() {
        var inv = newInventory(10);
        inv.deduct(4);
        inv.restore(4);
        assertThat(inv.getRemainingQuantity()).isEqualTo(10);
    }
}
