package com.example.miniproject.service;

import com.example.miniproject.dto.OrderRequest;
import com.example.miniproject.dto.OrderResponse;
import com.example.miniproject.models.Order;
import com.example.miniproject.models.OrderStatus;
import com.example.miniproject.models.Product;
import com.example.miniproject.models.User;
import com.example.miniproject.repository.OrderRepository;
import com.example.miniproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    // NOTE: We depend on ProductService, not ProductRepository directly.
    // This is the "Order module calls Product module" boundary from the
    // requirement. In a real microservice split, this @Autowired would be
    // swapped for an HTTP client calling the Product service's API instead
    // — the rest of this class wouldn't need to change.
    @Autowired
    private ProductService productService;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public OrderResponse createOrder(OrderRequest request, String username) {
        User customer = findUserOrThrow(username);

        // --- Inter-module call: ask the Product module if this product
        // exists and has enough stock. ---
        Product product = productService.getProductEntityById(request.getProductId());

        if (product.getStock() < request.getQuantity()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock. Available: " + product.getStock() + ", requested: " + request.getQuantity());
        }

        // Decrement stock and persist back through the Product module.
        product.setStock(product.getStock() - request.getQuantity());
        productService.saveProduct(product);

        Order order = new Order();
        order.setProduct(product);
        order.setCustomer(customer);
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PENDING);

        Order saved = orderRepository.save(order);

        // Fire-and-forget: this call returns immediately because
        // sendOrderConfirmation is @Async. The HTTP response to the
        // customer does NOT wait on the email actually being sent.
        notificationService.sendOrderConfirmation(saved.getId());

        return OrderResponse.fromEntity(saved);
    }

    public List<OrderResponse> getMyOrders(String username) {
        User customer = findUserOrThrow(username);

        return orderRepository.findByCustomerId(customer.getId())
                .stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));
    }
}