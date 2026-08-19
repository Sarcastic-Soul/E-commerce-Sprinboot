package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.model.*;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
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

    @Mock
    private PaymentService paymentService;

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

    /** Persists an id on first save so placeOrder can build the Razorpay receipt. */
    private void stubOrderSave() {
        when(orderRepo.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            if (o.getId() == null) {
                o.setId(42L);
            }
            return o;
        });
    }

    // ---------- placeOrder: reserves nothing until payment clears ----------

    @Test
    void testPlaceOrder_CreatesPendingOrderWithoutTouchingStockOrCart()
        throws Exception {
        testCart.getItems().add(testCartItem);
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);
        stubOrderSave();
        when(
            paymentService.createRazorpayOrder(any(BigDecimal.class), eq("42"))
        ).thenReturn("order_rzp_123");

        Order createdOrder = orderService.placeOrder("testuser");

        assertNotNull(createdOrder);
        assertEquals("PENDING", createdOrder.getStatus());
        assertEquals(testUser, createdOrder.getUser());
        assertEquals(new BigDecimal("200.00"), createdOrder.getTotalAmount());
        assertEquals("order_rzp_123", createdOrder.getRazorpayOrderId());

        // The order snapshots what was bought...
        assertEquals(1, createdOrder.getItems().size());
        assertEquals(2, createdOrder.getItems().get(0).getQuantity());
        assertEquals(
            new BigDecimal("100.00"),
            createdOrder.getItems().get(0).getPriceAtPurchase()
        );

        // ...but nothing is committed until the payment is verified.
        assertEquals(10, testProduct.getQuantity());
        verify(productRepo, never()).save(any(Product.class));
        verify(cartService, never()).clearCart(anyString());
    }

    @Test
    void testPlaceOrder_FailsWhenCartIsEmpty() {
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> orderService.placeOrder("testuser")
        );

        assertEquals("Cart is empty", exception.getMessage());
        verify(orderRepo, never()).save(any(Order.class));
    }

    @Test
    void testPlaceOrder_FailsWhenNotEnoughStock() {
        testCartItem.setQuantity(15); // only 10 in stock
        testCart.getItems().add(testCartItem);
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> orderService.placeOrder("testuser")
        );

        assertTrue(
            exception.getMessage().contains("does not have enough stock")
        );
        assertEquals(10, testProduct.getQuantity());
        verify(productRepo, never()).save(any(Product.class));
        verify(orderRepo, never()).save(any(Order.class));
    }

    // ---------- processSuccessfulPayment: commits the order ----------

    /** Builds a PENDING order for `quantity` units of testProduct. */
    private Order pendingOrder(int quantity) {
        Order order = new Order();
        order.setId(42L);
        order.setUser(testUser);
        order.setStatus("PENDING");
        order.setRazorpayOrderId("order_rzp_123");
        order.setTotalAmount(
            testProduct.getPrice().multiply(BigDecimal.valueOf(quantity))
        );

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(testProduct);
        orderItem.setQuantity(quantity);
        orderItem.setPriceAtPurchase(testProduct.getPrice());
        order.getItems().add(orderItem);

        return order;
    }

    @Test
    void testProcessSuccessfulPayment_DeductsInventoryAndClearsCart() {
        Order order = pendingOrder(2);
        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        orderService.processSuccessfulPayment("order_rzp_123");

        assertEquals("COMPLETED", order.getStatus());
        assertEquals(8, testProduct.getQuantity()); // 10 - 2
        assertTrue(testProduct.isAvailable());
        verify(productRepo, times(1)).save(testProduct);
        verify(cartService, times(1)).clearCart("testuser");
    }

    /**
     * Regression: stock must come from the order, not from the live cart. The cart
     * is mutable and can change between placing the order and the payment callback.
     */
    @Test
    void testProcessSuccessfulPayment_IgnoresCartMutatedAfterOrderWasPlaced() {
        Order order = pendingOrder(2); // the user actually paid for 2

        // Meanwhile the cart was changed to 9 units of the same product.
        CartItem tamperedItem = new CartItem();
        tamperedItem.setProduct(testProduct);
        tamperedItem.setQuantity(9);
        testCart.getItems().add(tamperedItem);

        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        orderService.processSuccessfulPayment("order_rzp_123");

        // 10 - 2 (ordered), NOT 10 - 9 (whatever is sitting in the cart now)
        assertEquals(8, testProduct.getQuantity());
        assertTrue(testProduct.isAvailable());
        verify(cartService, never()).getCartByUsername(anyString());
    }

    @Test
    void testProcessSuccessfulPayment_ExactStockPurchase_SetsProductToUnavailable() {
        Order order = pendingOrder(10); // buys the entire remaining stock
        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        orderService.processSuccessfulPayment("order_rzp_123");

        assertEquals(0, testProduct.getQuantity());
        assertFalse(testProduct.isAvailable());
        verify(productRepo, times(1)).save(testProduct);
    }

    @Test
    void testProcessSuccessfulPayment_IsIdempotent() {
        Order order = pendingOrder(2);
        order.setStatus("COMPLETED"); // webhook/callback fired twice
        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        orderService.processSuccessfulPayment("order_rzp_123");

        assertEquals(10, testProduct.getQuantity()); // no second deduction
        verify(productRepo, never()).save(any(Product.class));
        verify(cartService, never()).clearCart(anyString());
    }

    @Test
    void testProcessFailedPayment_MarksPendingOrderRejected() {
        Order order = pendingOrder(2);
        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        orderService.processFailedPayment("order_rzp_123");

        assertEquals("REJECTED", order.getStatus());
        assertEquals(10, testProduct.getQuantity());
        verify(productRepo, never()).save(any(Product.class));
    }
}
