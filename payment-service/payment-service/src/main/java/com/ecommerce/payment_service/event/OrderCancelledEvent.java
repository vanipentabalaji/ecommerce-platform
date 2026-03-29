package com.ecommerce.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCancelledEvent {
    private Long orderId;
    private Long productId;
    private Integer quantity;
}
