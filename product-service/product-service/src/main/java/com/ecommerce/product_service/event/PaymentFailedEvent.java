package com.ecommerce.product_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class PaymentFailedEvent {

    private Long orderId;

    private Integer quantity;

    private String reason;

    private Long productId;

}
