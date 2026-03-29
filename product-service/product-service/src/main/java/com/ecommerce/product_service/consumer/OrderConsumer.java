package com.ecommerce.product_service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.product_service.event.OrderCreatedEvent;
import com.ecommerce.product_service.event.OrderCancelledEvent;
import com.ecommerce.product_service.event.PaymentFailedEvent;
import com.ecommerce.product_service.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final ProductService productService;

    @KafkaListener(topics = "order-created", groupId = "product-group-1")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {

        log.info("Received order event for product Id: {}", event.getProductId());
        Long productId = event.getProductId();
        Integer quantity = event.getQuantity();

        // Reduce stock in product service
        productService.reduceStock(productId, quantity);

        log.info("Stock reduced for product Id: {}, quantity: {} and remaining stock: {}", productId, 
            quantity, productService.getProduct(productId).getStock());
    }

    @KafkaListener(topics = "payment-failed", groupId = "product-group-1")
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {

        log.info("Received payment failed event for order Id: {}", event.getOrderId());
        Long productId = event.getProductId();
        Integer quantity = event.getQuantity();

        // Restore stock in product service
        log.info("Restoring stock for product Id: {}, quantity: {} due to {}", productId, quantity, event.getReason());
        productService.restoreStock(productId, quantity);

        log.info("Stock got restored for product Id: {}, quantity: {} and current stock: {}", productId, 
            quantity, productService.getProduct(productId).getStock());
    }

    @KafkaListener(topics = "order-cancelled", groupId = "product-group-1")
    public void handleOrderCancelledEvent(OrderCancelledEvent event) {

        log.info("Received order cancelled event for order Id: {}", event.getOrderId());
        Long productId = event.getProductId();
        Integer quantity = event.getQuantity();

        // Restore stock when order is cancelled
        log.info("Restoring stock for product Id: {}, quantity: {} due to order cancellation", productId, quantity);
        productService.restoreStock(productId, quantity);

        log.info("Stock restored for product Id: {}, quantity: {} after order cancellation. Current stock: {}", productId, 
            quantity, productService.getProduct(productId).getStock());
    }
}
