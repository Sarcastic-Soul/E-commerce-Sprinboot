package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.dto.OrderResponse;
import com.anish.e_commerce.dto.PlaceOrderRequest;
import com.anish.e_commerce.exception.BusinessException;
import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.*;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Mock
    private AddressService addressService;

    @Mock
    private CouponService couponService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
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

        testAddress = new Address();
        testAddress.setId(5L);
        testAddress.setUser(testUser);
        testAddress.setFullName("Asha Rao");
        testAddress.setPhone("9876543210");
        testAddress.setLine1("12 MG Road");
        testAddress.setCity("Bengaluru");
        testAddress.setState("Karnataka");
        testAddress.setPostalCode("560001");
        testAddress.setCountry("India");
    }

    private static PlaceOrderRequest request(String coupon) {
        PlaceOrderRequest r = new PlaceOrderRequest();
        r.setAddressId(5L);
        r.setCouponCode(coupon);
        return r;
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

    // ---------- placeOrder: reserves stock, snapshots address, applies coupon ----------

    @Test
    void testPlaceOrder_ReservesStockAndSnapshotsAddressButKeepsCart()
        throws Exception {
        testCart.getItems().add(testCartItem);
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);
        when(addressService.getOwned(5L, "testuser")).thenReturn(testAddress);
        when(productRepo.reserveStock(1, 2)).thenReturn(1);
        stubOrderSave();
        when(
            paymentService.createRazorpayOrder(any(BigDecimal.class), eq("42"))
        ).thenReturn("order_rzp_123");

        OrderResponse created = orderService.placeOrder("testuser", request(null));

        assertEquals(OrderStatus.PENDING, created.status());
        assertEquals("testuser", created.username());
        assertEquals(new BigDecimal("200.00"), created.subtotal());
        assertEquals(new BigDecimal("0.00"), created.discount());
        assertEquals(new BigDecimal("200.00"), created.totalAmount());
        assertEquals("order_rzp_123", created.razorpayOrderId());
        assertTrue(created.canCancel());

        assertEquals(1, created.items().size());
        assertEquals(1, created.items().get(0).productId());
        assertEquals(2, created.items().get(0).quantity());

        assertEquals("Bengaluru", created.shippingAddress().city());
        // A snapshot: editing the address book afterwards does not touch the order.
        testAddress.setCity("Mysuru");
        verify(orderRepo, atLeastOnce()).save(argThat(o ->
            "Bengaluru".equals(o.getShippingAddress().getCity())
        ));

        verify(productRepo).reserveStock(1, 2);
        verify(productRepo).markSoldOutIfEmpty(1);
        verify(cartService, never()).clearCart(anyString());
    }

    @Test
    void testPlaceOrder_AppliesCouponAndChargesDiscountedTotal() throws Exception {
        testCart.getItems().add(testCartItem);
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);
        when(addressService.getOwned(5L, "testuser")).thenReturn(testAddress);
        when(productRepo.reserveStock(1, 2)).thenReturn(1);

        Coupon coupon = new Coupon();
        coupon.setCode("WELCOME10");
        when(couponService.requireApplicable("welcome10", new BigDecimal("200.00")))
            .thenReturn(coupon);
        when(couponService.discountFor(coupon, new BigDecimal("200.00")))
            .thenReturn(new BigDecimal("20.00"));
        stubOrderSave();
        when(paymentService.createRazorpayOrder(any(BigDecimal.class), eq("42")))
            .thenReturn("order_rzp_123");

        OrderResponse created = orderService.placeOrder("testuser", request("welcome10"));

        assertEquals("WELCOME10", created.couponCode());
        assertEquals(new BigDecimal("20.00"), created.discount());
        assertEquals(new BigDecimal("180.00"), created.totalAmount());
        // Razorpay must be asked for the discounted amount, not the subtotal.
        verify(paymentService).createRazorpayOrder(new BigDecimal("180.00"), "42");
    }

    @Test
    void testPlaceOrder_FailsWhenCartIsEmpty() {
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> orderService.placeOrder("testuser", request(null))
        );

        assertEquals("Cart is empty", exception.getMessage());
        verify(orderRepo, never()).save(any(Order.class));
    }

    @Test
    void testPlaceOrder_FailsWhenAddressIsNotTheCallers() {
        testCart.getItems().add(testCartItem);
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);
        when(addressService.getOwned(5L, "testuser")).thenThrow(
            new ResourceNotFoundException("Address not found with id: 5")
        );

        assertThrows(
            ResourceNotFoundException.class,
            () -> orderService.placeOrder("testuser", request(null))
        );
        verify(productRepo, never()).reserveStock(anyInt(), anyInt());
    }

    @Test
    void testPlaceOrder_FailsWhenStockCannotBeReserved() {
        testCart.getItems().add(testCartItem);
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);
        when(addressService.getOwned(5L, "testuser")).thenReturn(testAddress);
        // Someone else took the stock between the cart and this checkout.
        when(productRepo.reserveStock(1, 2)).thenReturn(0);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> orderService.placeOrder("testuser", request(null))
        );

        assertEquals("Duck Keyboard is out of stock", exception.getMessage());
        verify(orderRepo, never()).save(any(Order.class));
        verifyNoInteractions(paymentService);
    }

    @Test
    void testPlaceOrder_FailsForSoftDeletedProduct() {
        testProduct.setActive(false);
        testCart.getItems().add(testCartItem);
        when(cartService.getCartByUsername("testuser")).thenReturn(testCart);
        when(addressService.getOwned(5L, "testuser")).thenReturn(testAddress);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> orderService.placeOrder("testuser", request(null))
        );

        assertTrue(exception.getMessage().contains("no longer available"));
        verify(productRepo, never()).reserveStock(anyInt(), anyInt());
    }

    // ---------- processSuccessfulPayment: commits the order ----------

    /** Builds an order in `status` for `quantity` units of testProduct. */
    private Order order(OrderStatus status, int quantity) {
        Order order = new Order();
        order.setId(42L);
        order.setUser(testUser);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
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
    void testProcessSuccessfulPayment_MarksPaidWithoutTouchingStock() {
        Order order = order(OrderStatus.PENDING, 2);
        order.setCouponCode("WELCOME10");
        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        orderService.processSuccessfulPayment("order_rzp_123", "pay_1", "testuser");

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertNotNull(order.getPaidAt());
        assertEquals("pay_1", order.getRazorpayPaymentId());
        // Stock was reserved at placeOrder; paying must not take it a second time.
        assertEquals(10, testProduct.getQuantity());
        verify(productRepo, never()).save(any(Product.class));
        verify(productRepo, never()).reserveStock(anyInt(), anyInt());
        verify(couponService).recordUse("WELCOME10");
        verify(cartService, times(1)).clearCart("testuser");
    }

    @Test
    void testProcessSuccessfulPayment_IsIdempotent() {
        Order order = order(OrderStatus.PAID, 2); // webhook/callback fired twice
        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        orderService.processSuccessfulPayment("order_rzp_123", "pay_1", "testuser");

        verify(orderRepo, never()).save(any(Order.class));
        verify(cartService, never()).clearCart(anyString());
        verifyNoInteractions(couponService);
    }

    @Test
    void testProcessSuccessfulPayment_OnExpiredOrderRefundsAndFails() {
        Order order = order(OrderStatus.EXPIRED, 2);
        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        assertThrows(
            BusinessException.class,
            () -> orderService.processSuccessfulPayment("order_rzp_123", "pay_1", "testuser")
        );

        assertEquals(OrderStatus.EXPIRED, order.getStatus());
        verify(paymentService).refund("pay_1", new BigDecimal("200.00"));
        verify(cartService, never()).clearCart(anyString());
    }

    @Test
    void testProcessFailedPayment_MarksPendingOrderRejectedAndRestoresStock() {
        Order order = order(OrderStatus.PENDING, 2);
        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        orderService.processFailedPayment("order_rzp_123", "testuser");

        assertEquals(OrderStatus.REJECTED, order.getStatus());
        verify(productRepo).restoreStock(1, 2);
    }

    // ---------- ownership: the razorpay order id is not a secret ----------

    @Test
    void testProcessSuccessfulPayment_RejectsCallerWhoDoesNotOwnTheOrder() {
        Order order = order(OrderStatus.PENDING, 2);
        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        assertThrows(
            org.springframework.security.access.AccessDeniedException.class,
            () ->
                orderService.processSuccessfulPayment(
                    "order_rzp_123",
                    "pay_1",
                    "mallory"
                )
        );

        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(cartService, never()).clearCart(anyString());
    }

    @Test
    void testProcessFailedPayment_RejectsCallerWhoDoesNotOwnTheOrder() {
        Order order = order(OrderStatus.PENDING, 2);
        when(orderRepo.findByRazorpayOrderId("order_rzp_123")).thenReturn(
            Optional.of(order)
        );

        assertThrows(
            org.springframework.security.access.AccessDeniedException.class,
            () -> orderService.processFailedPayment("order_rzp_123", "mallory")
        );

        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(productRepo, never()).restoreStock(anyInt(), anyInt());
    }

    // ---------- cancel ----------

    @Test
    void testCancel_PendingOrderRestoresStockWithoutRefund() {
        Order order = order(OrderStatus.PENDING, 2);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        OrderResponse result = orderService.cancelOrder(42L, "testuser");

        assertEquals(OrderStatus.CANCELLED, result.status());
        assertNotNull(result.cancelledAt());
        assertFalse(result.canCancel());
        verify(productRepo).restoreStock(1, 2);
        verify(paymentService, never()).refund(anyString(), any());
        verify(notificationService).notifyUser(eq(testUser), contains("cancelled"));
    }

    @Test
    void testCancel_PaidOrderTriesRefundAndStillCancelsWhenItFails() {
        Order order = order(OrderStatus.PAID, 2);
        order.setRazorpayPaymentId("pay_1");
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);
        when(paymentService.refund("pay_1", new BigDecimal("200.00"))).thenReturn(false);

        OrderResponse result = orderService.cancelOrder(42L, "testuser");

        assertEquals(OrderStatus.CANCELLED, result.status());
        verify(paymentService).refund("pay_1", new BigDecimal("200.00"));
        verify(productRepo).restoreStock(1, 2);
    }

    @Test
    void testCancel_ShippedOrderIsRejected() {
        Order order = order(OrderStatus.SHIPPED, 2);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));

        assertThrows(
            BusinessException.class,
            () -> orderService.cancelOrder(42L, "testuser")
        );

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        verify(productRepo, never()).restoreStock(anyInt(), anyInt());
    }

    @Test
    void testCancel_SomeoneElsesOrderLooksMissing() {
        Order order = order(OrderStatus.PENDING, 2);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));

        assertThrows(
            ResourceNotFoundException.class,
            () -> orderService.cancelOrder(42L, "mallory")
        );
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void testGetOrder_AdminMaySeeAnyOrderOthersMayNot() {
        Order order = order(OrderStatus.PAID, 1);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));

        assertEquals(42L, orderService.getOrder(42L, "admin", true).id());
        assertThrows(
            ResourceNotFoundException.class,
            () -> orderService.getOrder(42L, "mallory", false)
        );
    }

    // ---------- admin status transitions ----------

    @Test
    void testUpdateStatus_PaidToShippedToDelivered() {
        Order order = order(OrderStatus.PAID, 1);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        OrderResponse shipped = orderService.updateStatus(42L, OrderStatus.SHIPPED);
        assertEquals(OrderStatus.SHIPPED, shipped.status());
        assertNotNull(shipped.shippedAt());

        OrderResponse delivered = orderService.updateStatus(42L, OrderStatus.DELIVERED);
        assertEquals(OrderStatus.DELIVERED, delivered.status());
        assertNotNull(delivered.deliveredAt());

        verify(notificationService, times(2)).notifyUser(eq(testUser), anyString());
        verify(productRepo, never()).restoreStock(anyInt(), anyInt());
    }

    @Test
    void testUpdateStatus_AdminCancelOfPaidOrderRestoresStock() {
        Order order = order(OrderStatus.PAID, 3);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        OrderResponse result = orderService.updateStatus(42L, OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, result.status());
        verify(productRepo).restoreStock(1, 3);
        verify(notificationService).notifyUser(eq(testUser), contains("cancelled"));
    }

    @Test
    void testUpdateStatus_InvalidTransitionsAreRejected() {
        Order pending = order(OrderStatus.PENDING, 1);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(pending));
        assertThrows(BusinessException.class, () -> orderService.updateStatus(42L, OrderStatus.SHIPPED));

        Order paid = order(OrderStatus.PAID, 1);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(paid));
        assertThrows(BusinessException.class, () -> orderService.updateStatus(42L, OrderStatus.DELIVERED));

        Order shipped = order(OrderStatus.SHIPPED, 1);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(shipped));
        assertThrows(BusinessException.class, () -> orderService.updateStatus(42L, OrderStatus.CANCELLED));

        Order delivered = order(OrderStatus.DELIVERED, 1);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(delivered));
        assertThrows(BusinessException.class, () -> orderService.updateStatus(42L, OrderStatus.PAID));

        verify(orderRepo, never()).save(any(Order.class));
        verifyNoInteractions(notificationService);
    }

    // ---------- expiry ----------

    @Test
    void testExpireOrder_OnlyExpiresStillPendingOrders() {
        Order pending = order(OrderStatus.PENDING, 2);
        when(orderRepo.findById(42L)).thenReturn(Optional.of(pending));

        assertTrue(orderService.expireOrder(42L));
        assertEquals(OrderStatus.EXPIRED, pending.getStatus());
        verify(productRepo).restoreStock(1, 2);

        Order paid = order(OrderStatus.PAID, 2);
        when(orderRepo.findById(43L)).thenReturn(Optional.of(paid));
        assertFalse(orderService.expireOrder(43L));
        assertEquals(OrderStatus.PAID, paid.getStatus());
        verify(productRepo, times(1)).restoreStock(anyInt(), anyInt());
    }
}
