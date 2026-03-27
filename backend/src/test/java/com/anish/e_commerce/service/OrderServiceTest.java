package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.model.*;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    @Mock
    private ProductRepo productRepo;

    private User testUser;
    private Cart testCart;
    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");

        testProduct1 = new Product();
        testProduct1.setPrice(new BigDecimal("10.00"));
        testProduct1.setName("Duck Toy");
        testProduct1.setQuantity(10);

        testProduct2 = new Product();
        testProduct2.setPrice(new BigDecimal("20.50"));
        testProduct2.setName("Keyboard");
        testProduct2.setQuantity(5);

        testCart = new Cart();
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>());
    }

    @Test
    void testPlaceOrder_Success_CalculatesTotalAndClearsCart() {
        // Arrange: Add 2 Duck Toys ($20.00) and 1 Keyboard ($20.50) -> Total should be $40.50
        CartItem item1 = new CartItem(1L, testCart, testProduct1, 2);
        CartItem item2 = new CartItem(2L, testCart, testProduct2, 1);
        testCart.getItems().addAll(List.of(item1, item2));

        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);
        when(orderRepo.save(any(Order.class))).thenAnswer(i ->
            i.getArguments()[0]
        ); // Return the saved order

        // Act
        Order savedOrder = orderService.placeOrder("testuser");

        // Assert
        assertNotNull(savedOrder);
        assertEquals("COMPLETED", savedOrder.getStatus());
        assertEquals(new BigDecimal("40.50"), savedOrder.getTotalAmount()); // Verifies exact BigDecimal math
        assertEquals(2, savedOrder.getItems().size()); // Verifies all items moved over

        // Verify side-effects
        verify(cartService, times(1)).clearCart("testuser");
        verify(orderRepo, times(1)).save(any(Order.class));
    }

    @Test
    void testPlaceOrder_EmptyCart_ThrowsException() {
        // Arrange: Cart is completely empty
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> {
                orderService.placeOrder("testuser");
            }
        );

        assertEquals("Cart is empty", exception.getMessage());
        verify(orderRepo, never()).save(any(Order.class)); // Ensures no junk order was saved to the DB
    }
}
