package com.anish.e_commerce.service;

import com.anish.e_commerce.dto.OrderResponse;
import com.anish.e_commerce.dto.PlaceOrderRequest;
import com.anish.e_commerce.exception.BusinessException;
import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.*;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    /** A PENDING order holds stock; after this long without payment it is released. */
    public static final Duration PENDING_TTL = Duration.ofMinutes(30);

    private final OrderRepo orderRepo;
    private final ProductRepo productRepo;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final AddressService addressService;
    private final CouponService couponService;
    private final NotificationService notificationService;

    /**
     * Stock is reserved here, not at payment: two buyers racing for the last unit
     * must not both get as far as paying. Any failure below (out of stock, bad
     * coupon, Razorpay down) rolls the whole transaction back, reservations included.
     * Quantities change, so the cached catalog is evicted.
     */
    @Transactional
    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", allEntries = true),
        }
    )
    public OrderResponse placeOrder(String username, PlaceOrderRequest request) {
        Cart cart = cartService.getCartByUsername(username);

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        Address address = addressService.getOwned(request.getAddressId(), username);

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(ShippingAddress.from(address));

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.isActive()) {
                throw new BusinessException(product.getName() + " is no longer available");
            }

            // Conditional UPDATE: checks and takes the stock in one statement.
            if (productRepo.reserveStock(product.getId(), item.getQuantity()) == 0) {
                throw new BusinessException(product.getName() + " is out of stock");
            }
            productRepo.markSoldOutIfEmpty(product.getId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            order.getItems().add(orderItem);

            subtotal = subtotal.add(
                product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = BigDecimal.ZERO.setScale(2);
        String code = request.getCouponCode();
        if (code != null && !code.isBlank()) {
            Coupon coupon = couponService.requireApplicable(code, subtotal);
            discount = couponService.discountFor(coupon, subtotal);
            order.setCouponCode(coupon.getCode());
        }

        BigDecimal total = subtotal.subtract(discount);
        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setTotalAmount(total);
        Order savedOrder = orderRepo.save(order);

        try {
            // The discounted total is what the customer is actually charged.
            String razorpayOrderId = paymentService.createRazorpayOrder(
                total,
                savedOrder.getId().toString()
            );
            savedOrder.setRazorpayOrderId(razorpayOrderId);
            savedOrder = orderRepo.save(savedOrder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize payment", e);
        }

        // Cart is NOT cleared here. We wait for payment confirmation.
        return OrderResponse.from(savedOrder);
    }

    /** Stock was already reserved at placeOrder; this only commits the order. */
    @Transactional
    public void processSuccessfulPayment(
        String razorpayOrderId,
        String razorpayPaymentId,
        String username
    ) {
        Order order = orderRepo
            .findByRazorpayOrderId(razorpayOrderId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Order not found")
            );
        assertOwnership(order, username);

        if (OrderStatus.PURCHASED.contains(order.getStatus())) {
            return; // Callback fired twice; nothing left to do.
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            // Expired/cancelled while the user was paying: its stock is gone, so
            // give the money back instead of shipping something we may not have.
            if (razorpayPaymentId != null) {
                paymentService.refund(razorpayPaymentId, order.getTotalAmount());
            }
            throw new BusinessException(
                "This order is " + order.getStatus().name().toLowerCase() +
                    "; your payment will be refunded"
            );
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setRazorpayPaymentId(razorpayPaymentId);
        orderRepo.save(order);

        if (order.getCouponCode() != null) {
            couponService.recordUse(order.getCouponCode());
        }

        // Clear the cart for the user
        cartService.clearCart(order.getUser().getUsername());
    }

    @Transactional
    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", allEntries = true),
        }
    )
    public void processFailedPayment(String razorpayOrderId, String username) {
        Order order = orderRepo
            .findByRazorpayOrderId(razorpayOrderId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Order not found")
            );
        assertOwnership(order, username);

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.REJECTED);
            restoreStock(order);
            orderRepo.save(order);
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String username) {
        return orderRepo
            .findByUser_UsernameOrderByCreatedAtDesc(username)
            .stream()
            .map(OrderResponse::from)
            .toList();
    }

    /** Owner or admin; anyone else gets the same 404 as a missing id. */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id, String username, boolean isAdmin) {
        Order order = findOrder(id);
        if (!isAdmin && !order.getUser().getUsername().equals(username)) {
            throw notFound(id);
        }
        return OrderResponse.from(order);
    }

    @Transactional
    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", allEntries = true),
        }
    )
    public OrderResponse cancelOrder(Long id, String username) {
        Order order = findOrder(id);
        if (!order.getUser().getUsername().equals(username)) {
            throw notFound(id);
        }
        if (!order.getStatus().isCancellableByUser()) {
            throw new BusinessException(
                "Only pending or paid orders can be cancelled (this one is " +
                    order.getStatus() + ")"
            );
        }
        cancel(order);
        notificationService.notifyUser(
            order.getUser(),
            "Your order #" + order.getId() + " has been cancelled."
        );
        return OrderResponse.from(orderRepo.save(order));
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(OrderStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<Order> orders = status == null
            ? orderRepo.findAll(pageable)
            : orderRepo.findByStatus(status, pageable);
        return orders.map(OrderResponse::from);
    }

    /** Allowed: PAID -> SHIPPED -> DELIVERED, and PAID -> CANCELLED. */
    @Transactional
    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", allEntries = true),
        }
    )
    public OrderResponse updateStatus(Long id, OrderStatus target) {
        Order order = findOrder(id);
        OrderStatus current = order.getStatus();
        LocalDateTime now = LocalDateTime.now();

        if (current == OrderStatus.PAID && target == OrderStatus.SHIPPED) {
            order.setStatus(OrderStatus.SHIPPED);
            order.setShippedAt(now);
        } else if (current == OrderStatus.SHIPPED && target == OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(now);
        } else if (current == OrderStatus.PAID && target == OrderStatus.CANCELLED) {
            cancel(order);
        } else {
            throw new BusinessException(
                "Cannot change order status from " + current + " to " + target
            );
        }

        notificationService.notifyUser(
            order.getUser(),
            "Your order #" + order.getId() + " is now " +
                order.getStatus().name().toLowerCase() + "."
        );
        return OrderResponse.from(orderRepo.save(order));
    }

    // ---------- expiry sweep (driven by OrderExpiryJob) ----------

    @Transactional(readOnly = true)
    public List<Long> findStalePendingOrderIds() {
        return orderRepo
            .findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING,
                LocalDateTime.now().minus(PENDING_TTL)
            )
            .stream()
            .map(Order::getId)
            .toList();
    }

    /**
     * One order per transaction, so one failure does not undo the rest. The status
     * is re-checked here, and @Version on Order makes a racing payment callback and
     * this sweep unable to both win.
     */
    @Transactional
    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", allEntries = true),
        }
    )
    public boolean expireOrder(Long id) {
        Order order = orderRepo.findById(id).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return false;
        }
        order.setStatus(OrderStatus.EXPIRED);
        restoreStock(order);
        orderRepo.save(order);
        return true;
    }

    // ---------- helpers ----------

    private void cancel(Order order) {
        // Refund is best effort (PaymentService never throws): with test keys or a
        // gateway hiccup the cancel must still go through and release the stock.
        if (order.getStatus() == OrderStatus.PAID && order.getRazorpayPaymentId() != null) {
            paymentService.refund(order.getRazorpayPaymentId(), order.getTotalAmount());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        restoreStock(order);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepo.restoreStock(item.getProduct().getId(), item.getQuantity());
        }
    }

    private Order findOrder(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Order not found with id: " + id);
    }

    // The payment callbacks are keyed only by the Razorpay order id, which is not a
    // secret — without this check, an authenticated user could complete or fail
    // someone else's order just by guessing/observing that id.
    private void assertOwnership(Order order, String username) {
        if (!order.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("This order does not belong to you.");
        }
    }
}
