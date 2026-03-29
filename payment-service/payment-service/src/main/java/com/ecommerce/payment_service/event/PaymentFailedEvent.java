package com.ecommerce.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentFailedEvent {

    private Long orderId;

    private Integer quantity;

    private String reason;

    private Long productId;

}
