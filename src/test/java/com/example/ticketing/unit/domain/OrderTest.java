package com.example.ticketing.unit.domain;

import com.example.ticketing.order.domain.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class OrderTest {

    private Order newOrder() {
        return new Order(1L, 1L, 2, "key-abc");
    }

    @Test
    void initialStatus_isPending() {
        assertThat(newOrder().getStatus()).isEqualTo(Order.Status.PENDING);
    }

    @Test
    void confirm_changesStatusToConfirmed() {
        var order = newOrder();
        order.confirm();
        assertThat(order.getStatus()).isEqualTo(Order.Status.CONFIRMED);
    }

    @Test
    void cancel_changesStatusToCancelled() {
        var order = newOrder();
        order.cancel();
        assertThat(order.getStatus()).isEqualTo(Order.Status.CANCELLED);
    }
}
