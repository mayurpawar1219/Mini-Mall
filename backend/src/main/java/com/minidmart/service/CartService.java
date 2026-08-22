package com.minidmart.service;

import com.minidmart.dto.CartItemRequest;
import com.minidmart.dto.CartItemResponse;
import com.minidmart.dto.CartItemUpdateRequest;
import com.minidmart.dto.CartResponse;
import com.minidmart.entity.Cart;
import com.minidmart.entity.CartItem;
import com.minidmart.entity.Inventory;
import com.minidmart.entity.Product;
import com.minidmart.entity.User;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.exception.ResourceNotFoundException;
import com.minidmart.repository.CartItemRepository;
import com.minidmart.repository.CartRepository;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.ProductRepository;
import com.minidmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse addItemToCart(UUID userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = getActiveProduct(request.getProductId());
        Inventory inventory = getInventory(request.getProductId());

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
        
        CartItem item;
        if (existingItemOpt.isPresent()) {
            item = existingItemOpt.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            validateStock(inventory, newQuantity);
            item.setQuantity(newQuantity);
        } else {
            validateStock(inventory, request.getQuantity());
            item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(item);
        }
        
        cartRepository.save(cart);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID userId, Long productId, CartItemUpdateRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = getCartItem(cart.getId(), productId);
        Inventory inventory = getInventory(productId);

        validateStock(inventory, request.getQuantity());
        item.setQuantity(request.getQuantity());
        
        cartItemRepository.save(item);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItemFromCart(UUID userId, Long productId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = getCartItem(cart.getId(), productId);
        
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        
        return mapToCartResponse(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
    }

    public Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    private Product getActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!product.isActive()) {
            throw new InvalidOperationException("Cannot add inactive product to cart");
        }
        return product;
    }

    private Inventory getInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product"));
    }

    private CartItem getCartItem(Long cartId, Long productId) {
        return cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product is not in your cart"));
    }

    private void validateStock(Inventory inventory, int requestedQuantity) {
        if (requestedQuantity > inventory.getAvailableQuantity()) {
            throw new InvalidOperationException("Requested quantity exceeds available stock");
        }
    }

    public CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    int stockQty = inventoryRepository.findByProductId(item.getProduct().getId())
                            .map(Inventory::getAvailableQuantity).orElse(0);
                    return CartItemResponse.builder()
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .imageUrl(item.getProduct().getImageUrl())
                            .unitPrice(item.getProduct().getPrice())
                            .quantity(item.getQuantity())
                            .stockQuantity(stockQty)
                            .subtotal(subtotal)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .totalAmount(total)
                .build();
    }
}
