package com.example.ticketing.notification.application;

import com.example.ticketing.order.domain.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("[NOTIFICATION] Order confirmed — orderId={} userEmail={} ticketInventoryId={} qty={}",
                event.orderId(), event.userEmail(), event.ticketInventoryId(), event.quantity());
        // TODO: 阶段 5 换成 GCP Pub/Sub 发真实消息
    }
}
