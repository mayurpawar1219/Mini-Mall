package com.minidmart.service;

import com.minidmart.dto.CartItemRequest;
import com.minidmart.dto.CartItemUpdateRequest;
import com.minidmart.dto.CartResponse;
import com.minidmart.entity.Cart;
import com.minidmart.entity.CartItem;
import com.minidmart.entity.Inventory;
import com.minidmart.entity.Product;
import com.minidmart.entity.User;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.repository.CartItemRepository;
import com.minidmart.repository.CartRepository;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.ProductRepository;
import com.minidmart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Cart cart;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        cart = Cart.builder().id(1L).user(user).items(new ArrayList<>()).build();
        product = Product.builder().id(100L).name("Test Product").price(BigDecimal.valueOf(50)).active(true).build();
        inventory = Inventory.builder().id(10L).product(product).availableQuantity(10).build();
    }

    @Test
    void getCart_CreatesNewCart_WhenNotExists() {
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.getCart(user.getId());

        assertNotNull(response);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItemToCart_Success() {
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())).thenReturn(Optional.empty());
        
        CartItemRequest request = new CartItemRequest(product.getId(), 2);
        
        CartResponse response = cartService.addItemToCart(user.getId(), request);

        assertEquals(1, response.getItems().size());
        assertEquals(BigDecimal.valueOf(100), response.getTotalAmount());
        verify(cartRepository).save(cart);
    }

    @Test
    void addItemToCart_ExceedsStock_ThrowsException() {
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
        
        CartItemRequest request = new CartItemRequest(product.getId(), 15); // stock is 10
        
        assertThrows(InvalidOperationException.class, () -> cartService.addItemToCart(user.getId(), request));
    }

    @Test
    void updateItemQuantity_Success() {
        CartItem item = CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build();
        cart.getItems().add(item);

        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())).thenReturn(Optional.of(item));
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));

        CartItemUpdateRequest request = new CartItemUpdateRequest(5);
        
        CartResponse response = cartService.updateItemQuantity(user.getId(), product.getId(), request);

        assertEquals(5, item.getQuantity());
        assertEquals(BigDecimal.valueOf(250), response.getTotalAmount());
        verify(cartItemRepository).save(item);
    }
}
