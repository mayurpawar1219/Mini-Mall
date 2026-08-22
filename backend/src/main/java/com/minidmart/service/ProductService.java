package com.minidmart.service;

import com.minidmart.dto.ProductRequest;
import com.minidmart.dto.ProductResponse;
import com.minidmart.dto.ProductStatusRequest;
import com.minidmart.entity.Category;
import com.minidmart.entity.Product;
import com.minidmart.exception.DuplicateResourceException;
import com.minidmart.exception.ResourceNotFoundException;
import com.minidmart.repository.CategoryRepository;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(boolean activeOnly) {
        List<Product> products = activeOnly ? productRepository.findByActiveTrue() : productRepository.findAll();
        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword).stream()
                .filter(Product::isActive)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .filter(Product::isActive)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request, String staffId) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .active(true)
                .category(category)
                .build();

        product = productRepository.save(product);
        inventoryService.initializeInventory(product, request.getStockQuantity() != null ? request.getStockQuantity() : 0);

        if (staffId != null) {
            auditService.log(null, "PRODUCT_CREATED", "Product", product.getId().toString(), "Created by: " + staffId);
        }

        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, String staffId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);

        product = productRepository.save(product);

        if (staffId != null) {
            auditService.log(null, "PRODUCT_UPDATED", "Product", product.getId().toString(), "Updated by: " + staffId);
        }

        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProductStatus(Long id, ProductStatusRequest request, String staffId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        product.setActive(request.getActive());
        product = productRepository.save(product);

        if (staffId != null) {
            auditService.log(null, "PRODUCT_STATUS_CHANGED", "Product", product.getId().toString(), "Status: " + request.getActive() + " by: " + staffId);
        }

        return toResponse(product);
    }

    private ProductResponse toResponse(Product product) {
        Integer stockQty = inventoryRepository.findByProductId(product.getId())
                .map(inv -> inv.getAvailableQuantity())
                .orElse(0);

        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .active(product.isActive())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .stockQuantity(stockQty)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
