package com.example.miniproject.service;

import com.example.miniproject.dto.ProductRequest;
import com.example.miniproject.dto.ProductResponse;
import com.example.miniproject.models.Product;
import com.example.miniproject.models.Role;
import com.example.miniproject.models.User;
import com.example.miniproject.repository.ProductRepository;
import com.example.miniproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    // ---------- Public reads ----------

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        Product product = findProductOrThrow(id);
        return ProductResponse.fromEntity(product);
    }
    public Product getProductEntityById(Long id) {
        return findProductOrThrow(id);
    }

    // Called by OrderService after it has validated and decremented stock.
    // Keeps ProductRepository encapsulated inside ProductService.
    public void saveProduct(Product product) {
        productRepository.save(product);
    }

    // ---------- Vendor/Admin writes ----------

    public ProductResponse createProduct(ProductRequest request, String username) {
        User vendor = findUserOrThrow(username);

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setVendor(vendor);

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request, String username) {
        Product product = findProductOrThrow(id);
        User currentUser = findUserOrThrow(username);

        assertOwnerOrAdmin(product, currentUser);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    public void deleteProduct(Long id, String username) {
        Product product = findProductOrThrow(id);
        User currentUser = findUserOrThrow(username);

        assertOwnerOrAdmin(product, currentUser);

        productRepository.delete(product);
    }

    // ---------- Helpers ----------

    private void assertOwnerOrAdmin(Product product, User currentUser) {
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = product.getVendor().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to modify this product");
        }
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + id));
    }

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));
    }
}