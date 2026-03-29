package com.ecommerce.order_service.controller;

import java.util.concurrent.ExecutionException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order_service.dto.OrderRequest;
import com.ecommerce.order_service.service.OrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place-order")
    public String placeOrder(@RequestBody OrderRequest request) throws InterruptedException, ExecutionException {
        return orderService.placeOrder(request.getProductId(), request.getQuantity());
    }

    //TODO: Add UserID to OrderRequest and filter orders by userId
    // @GetMapping("/get-orders")
    // public String getOrders(@RequestParam Long userId) {
    //     return orderService.getOrdersForUser(userId);
    // }

    @PostMapping("/cancel-order")
    public String cancelOrder(@RequestBody OrderRequest request) {
        return orderService.cancelOrder(request.getOrderId());
    }


}
