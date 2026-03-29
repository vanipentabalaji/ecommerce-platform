package com.ecommerce.order_service.service;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ecommerce.order_service.constants.OrderStatus;
import com.ecommerce.order_service.dto.ProductDTO;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.event.OrderCancelledEvent;
import com.ecommerce.order_service.event.OrderCreatedEvent;
import com.ecommerce.order_service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final RestTemplate restTemplate;

    private final KafkaTemplate<String, OrderCreatedEvent> sendOrderEvent;

    private final KafkaTemplate<String, OrderCancelledEvent> sendOrderCancelledEvent;

    public String placeOrder(Long productId, Integer quantity) throws InterruptedException, ExecutionException {
        // Fetch product details from product service
        String productServiceUrl = "http://localhost:8081/products/" + productId;
        ProductDTO product = restTemplate.getForObject(productServiceUrl, ProductDTO.class);

        if (product == null) {
            return "Product not found";
        }

        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        // Create order
        Order order = new Order();
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setAmount(product.getPrice() * quantity);
        order.setStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);

        // Publish order created event
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(savedOrder.getId());
        event.setProductId(savedOrder.getProductId());
        event.setQuantity(savedOrder.getQuantity());
        event.setAmount(savedOrder.getAmount());

        var result = sendOrderEvent.send("order-created", event).get();
        log.info("Kafka metadata: " + result.getRecordMetadata());
        log.info("Event sent to Kafka");

        return "Order placed successfully with ID: " + savedOrder.getId();
    }

    public String getOrderStatus(Long orderId) {
    return orderRepository.findById(orderId)
            .map(order -> "Order " + orderId + " status: " + order.getStatus())
            .orElse("Order " + orderId + " not found!");
}

    public String getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) return "No orders found!";

        StringBuilder sb = new StringBuilder("Your Orders:\n");
        orders.forEach(o -> sb.append("Order #")
                .append(o.getId())
                .append(" → ")
                .append(o.getStatus())
                .append("\n"));
        return sb.toString();
    }

    public String cancelOrder(Long orderId) {
        return orderRepository.findById(orderId).map(order -> {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                return "Order " + orderId + " is already cancelled.";
            }
            order.setStatus(OrderStatus.CANCELLED);
            
            // Publish event to Kafka to notify other services (like product service to restock the items)
            OrderCancelledEvent event = new OrderCancelledEvent();
            event.setOrderId(order.getId());
            event.setProductId(order.getProductId());
            event.setQuantity(order.getQuantity());
            
            try {
                sendOrderCancelledEvent.send("order-cancelled", event).get();
                log.info("Order Cancelled event sent to Kafka for order ID: " + orderId);
            } catch (Exception e) {
                log.error("Failed to send order cancelled event: " + e.getMessage());
            }
            
            orderRepository.save(order);
            return "Order " + orderId + " has been cancelled. Items will be restocked.";
        }).orElse("Order " + orderId + " not found.");
    }

}
