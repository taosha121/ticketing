package com.example.ticketing.order.application;

import com.example.ticketing.auth.domain.UserRepository;
import com.example.ticketing.common.exception.ResourceNotFoundException;
import com.example.ticketing.event.domain.TicketInventory;
import com.example.ticketing.event.domain.TicketInventoryRepository;
import com.example.ticketing.order.domain.Order;
import com.example.ticketing.order.domain.OrderRepository;
import com.example.ticketing.order.domain.Reservation;
import com.example.ticketing.order.domain.ReservationRepository;
import com.example.ticketing.order.interfaces.dto.OrderResponse;
import com.example.ticketing.order.interfaces.dto.PlaceOrderRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final TicketInventoryRepository ticketInventoryRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        ReservationRepository reservationRepository,
                        TicketInventoryRepository ticketInventoryRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.ticketInventoryRepository = ticketInventoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse placeOrder(String userEmail, String idempotencyKey, PlaceOrderRequest req) {
        // 幂等检查：同一个 key 直接返回已有订单
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(OrderResponse::from)
                .orElseGet(() -> doPlaceOrder(userEmail, idempotencyKey, req));
    }

    private OrderResponse doPlaceOrder(String userEmail, String idempotencyKey, PlaceOrderRequest req) {
        Long userId = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();

        // 悲观锁：SELECT ... FOR UPDATE，阻止并发修改同一行库存
        TicketInventory inventory = ticketInventoryRepository
                .findByIdForUpdate(req.ticketInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("TicketInventory not found: " + req.ticketInventoryId()));

        // 库存扣减（不足时 deduct() 内部抛 IllegalStateException）
        inventory.deduct(req.quantity());

        Order order = new Order(userId, req.ticketInventoryId(), req.quantity(), idempotencyKey);
        orderRepository.save(order);

        Reservation reservation = new Reservation(
                order.getId(), userId, req.ticketInventoryId(),
                req.quantity(), OffsetDateTime.now().plusMinutes(15));
        reservationRepository.save(reservation);

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(String userEmail, Long orderId) {
        Long userId = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Not your order");
        }

        return OrderResponse.from(order);
    }
}
