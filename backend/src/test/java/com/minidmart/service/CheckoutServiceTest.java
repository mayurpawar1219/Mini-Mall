package com.minidmart.service;

import com.minidmart.dto.CheckoutConfirmRequest;
import com.minidmart.dto.OrderResponse;
import com.minidmart.entity.Cart;
import com.minidmart.entity.CartItem;
import com.minidmart.entity.Inventory;
import com.minidmart.entity.Order;
import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.OrderType;
import com.minidmart.entity.Product;
import com.minidmart.entity.User;
import com.minidmart.exception.EmptyCartException;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.repository.CartItemRepository;
import com.minidmart.repository.CartRepository;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.OrderItemRepository;
import com.minidmart.repository.OrderRepository;
import com.minidmart.repository.UserRepository;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private CheckoutService checkoutService;

    private User user;
    private Cart cart;
    private Product product;
    private Inventory inventory;
    private PaymentIntent mockPaymentIntent;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).firstName("test").lastName("user").build();
        cart = Cart.builder().id(1L).user(user).items(new ArrayList<>()).build();
        product = Product.builder().id(100L).name("Test Product").price(BigDecimal.valueOf(50)).active(true).build();
        inventory = Inventory.builder().id(10L).product(product).availableQuantity(10).build();
        
        mockPaymentIntent = new PaymentIntent();
        mockPaymentIntent.setStatus("succeeded");
        mockPaymentIntent.setId("pi_test_123");
    }

    @Test
    void checkout_EmptyCart_ThrowsException() {
        try (MockedStatic<PaymentIntent> mockedStatic = Mockito.mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(mockPaymentIntent);
            
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

            CheckoutConfirmRequest request = new CheckoutConfirmRequest(OrderType.STORE_PICKUP, "pi_test_123", null, null, null, null, null);

            assertThrows(EmptyCartException.class, () -> checkoutService.checkout(user.getId(), request));
        }
    }

    @Test
    void checkout_Success() {
        try (MockedStatic<PaymentIntent> mockedStatic = Mockito.mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(mockPaymentIntent);
            
            CartItem item = CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build();
            cart.getItems().add(item);

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
            when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
            
            Order mockOrder = Order.builder()
                    .id(UUID.randomUUID())
                    .orderNumber("ORD-123")
                    .status(OrderStatus.PLACED)
                    .totalAmount(BigDecimal.valueOf(100))
                    .build();
            when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

            CheckoutConfirmRequest request = new CheckoutConfirmRequest(OrderType.STORE_PICKUP, "pi_test_123", null, null, null, null, null);

            OrderResponse response = checkoutService.checkout(user.getId(), request);

            assertNotNull(response);
            assertEquals(OrderStatus.PLACED, response.getStatus());
            assertEquals(8, inventory.getAvailableQuantity()); // 10 - 2
            
            verify(inventoryRepository).save(inventory);
            verify(orderRepository).save(any(Order.class));
            verify(orderItemRepository).saveAll(any());
            verify(cartItemRepository).deleteByCartId(cart.getId());
            verify(cartRepository).save(cart);
            verify(auditService).log(any(UUID.class), eq("CHECKOUT_COMPLETED"), anyString(), anyString(), anyString());
        }
    }

    @Test
    void checkout_ExceedsStock_ThrowsException() {
        try (MockedStatic<PaymentIntent> mockedStatic = Mockito.mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(mockPaymentIntent);
            
            CartItem item = CartItem.builder().id(1L).cart(cart).product(product).quantity(15).build();
            cart.getItems().add(item);

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
            when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));

            CheckoutConfirmRequest request = new CheckoutConfirmRequest(OrderType.STORE_PICKUP, "pi_test_123", null, null, null, null, null);

            assertThrows(InvalidOperationException.class, () -> checkoutService.checkout(user.getId(), request));
        }
    }
}
