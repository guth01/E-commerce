package com.example.miniproject.dto;

import com.example.miniproject.models.Order;
import com.example.miniproject.models.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long customerId;
    private String customerUsername;
    private Integer quantity;
    private OrderStatus status;
    private LocalDateTime createdAt;

    public static OrderResponse fromEntity(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setProductId(order.getProduct().getId());
        response.setProductName(order.getProduct().getName());
        response.setCustomerId(order.getCustomer().getId());
        response.setCustomerUsername(order.getCustomer().getUsername());
        response.setQuantity(order.getQuantity());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }
}