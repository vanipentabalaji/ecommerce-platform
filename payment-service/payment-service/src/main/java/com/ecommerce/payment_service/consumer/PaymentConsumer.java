package com.ecommerce.payment_service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ecommerce.payment_service.event.OrderCreatedEvent;
import com.ecommerce.payment_service.event.OrderCancelledEvent;
import com.ecommerce.payment_service.event.PaymentFailedEvent;
import com.ecommerce.payment_service.event.PaymentSuccessEvent;
import com.ecommerce.payment_service.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final PaymentService paymentService;

    private final KafkaTemplate<String, PaymentSuccessEvent> sendSuccessEvent;

    private final KafkaTemplate<String, PaymentFailedEvent> sendFailedEvent;

    @KafkaListener(topics = "order-created", groupId = "payment-group-1")
    public void listen(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: {}", event);
        
        boolean isPaymentSuccess = paymentService.processPayment(event);

        if (isPaymentSuccess) {
            PaymentSuccessEvent successEvent = new PaymentSuccessEvent();
            successEvent.setOrderId(event.getOrderId());

            sendSuccessEvent.send("payment-success", successEvent);
            log.info("Payment success event sent for order Id: {}", successEvent.getOrderId());
        } else {
            PaymentFailedEvent failedEvent = new PaymentFailedEvent();
            failedEvent.setOrderId(event.getOrderId());
            failedEvent.setQuantity(event.getQuantity());
            failedEvent.setReason("Payment processing failed due to insufficient funds");
            failedEvent.setProductId(event.getProductId());

            sendFailedEvent.send("payment-failed", failedEvent);
            log.info("Payment failed event sent for order Id: {}", failedEvent.getOrderId());
        }

    }

    @KafkaListener(topics = "order-cancelled", groupId = "payment-group-1")
    public void handleOrderCancelledEvent(OrderCancelledEvent event) {
        log.info("Received order cancelled event for order Id: {}", event.getOrderId());
        
        // Process refund for cancelled order
        boolean refundSuccess = paymentService.processRefund(event.getOrderId());
        
        if (refundSuccess) {
            log.info("Refund processed successfully for cancelled order Id: {}", event.getOrderId());
        } else {
            log.error("Failed to process refund for cancelled order Id: {}", event.getOrderId());
        }
    }
}
