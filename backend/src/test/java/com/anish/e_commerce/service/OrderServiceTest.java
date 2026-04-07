package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.model.*;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import java.math.BigDecimal;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private ProductRepo productRepo;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");

        testProduct = new Product();
        testProduct.setId(1);
        testProduct.setName("Duck Keyboard");
        testProduct.setPrice(new BigDecimal("100.00"));
        testProduct.setQuantity(10); // 10 in stock
        testProduct.setAvailable(true);

        testCart = new Cart();
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>());

        testCartItem = new CartItem();
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(2); // User wants to buy 2
    }

    @Test
    void testPlaceOrder_Success_DeductsInventoryAndClearsCart() {
        // Arrange
        testCart.getItems().add(testCartItem);
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);
        when(orderRepo.save(any(Order.class))).thenAnswer(i ->
            i.getArguments()[0]
        );

        // Act
        Order createdOrder = orderService.placeOrder("testuser");

        // Assert
        assertNotNull(createdOrder);
        assertEquals("COMPLETED", createdOrder.getStatus());
        assertEquals(testUser, createdOrder.getUser());

        // Inventory should be deducted (10 - 2 = 8)
        assertEquals(8, testProduct.getQuantity());
        assertTrue(testProduct.isAvailable());

        // Verifications
        verify(productRepo, times(1)).save(testProduct);
        verify(cartService, times(1)).clearCart("testuser");
        verify(orderRepo, times(1)).save(any(Order.class));
    }

    @Test
    void testPlaceOrder_FailsWhenCartIsEmpty() {
        // Arrange: Cart has 0 items
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> {
                orderService.placeOrder("testuser");
            }
        );

        assertEquals("Cart is empty", exception.getMessage());
        verify(orderRepo, never()).save(any(Order.class)); // Order should never be saved
    }

    @Test
    void testPlaceOrder_FailsWhenNotEnoughStock() {
        // Arrange: User tries to buy 15, but only 10 are in stock
        testCartItem.setQuantity(15);
        testCart.getItems().add(testCartItem);

        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> {
                orderService.placeOrder("testuser");
            }
        );

        assertTrue(
            exception.getMessage().contains("does not have enough stock")
        );
        assertEquals(10, testProduct.getQuantity()); // Stock should remain untouched
        verify(productRepo, never()).save(any(Product.class));
    }

    @Test
    void testPlaceOrder_ExactStockPurchase_SetsProductToUnavailable() {
        // Arrange: User buys exactly the remaining stock (10)
        testCartItem.setQuantity(10);
        testCart.getItems().add(testCartItem);

        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);
        when(orderRepo.save(any(Order.class))).thenAnswer(i ->
            i.getArguments()[0]
        );

        // Act
        orderService.placeOrder("testuser");

        // Assert
        assertEquals(0, testProduct.getQuantity()); // Stock should be 0
        assertFalse(testProduct.isAvailable()); // Product should automatically become unavailable
        verify(productRepo, times(1)).save(testProduct);
    }
}
