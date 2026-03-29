package com.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class OrderRequest {

    private Long orderId;

    private Long productId;

    private Integer quantity;

}
