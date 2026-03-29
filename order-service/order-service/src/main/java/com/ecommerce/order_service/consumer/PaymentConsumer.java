package com.ecommerce.order_service.consumer;

import java.util.Optional;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.order_service.constants.OrderStatus;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.event.PaymentFailedEvent;
import com.ecommerce.order_service.event.PaymentSuccessEvent;
import com.ecommerce.order_service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "payment-success", groupId = "order-group-1")
    public void handlePaymentSuccess(PaymentSuccessEvent event) {

        Long orderId = event.getOrderId();
        log.info("Received PaymentSuccessEvent for order ID: {}", orderId);
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(OrderStatus.PLACED);
            orderRepository.save(order);
            log.info("Order with ID {} marked as PLACED", orderId);
        } else {
            log.warn("Order with ID {} not found", orderId);
        }
    }


    @KafkaListener(topics = "payment-failed", groupId = "order-group-1")
    public void handlePaymentFailed(PaymentFailedEvent event) {

        Long orderId = event.getOrderId();
        log.info("Received PaymentFailedEvent for order ID: {}", orderId);
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            log.info("Order with ID {} marked as FAILED due to {}", orderId, event.getReason());
        } else {
            log.warn("Order with ID {} not found", orderId);
        }
    }



}
