package com.anish.e_commerce.dto;

import com.anish.e_commerce.model.Order;
import com.anish.e_commerce.model.OrderItem;
import com.anish.e_commerce.model.OrderStatus;
import com.anish.e_commerce.model.ShippingAddress;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * What every order endpoint returns. The entity itself is not serialized any more:
 * it drags in the whole User and Product graph and exposes internal fields.
 */
public record OrderResponse(
    Long id,
    OrderStatus status,
    String username,
    LocalDateTime createdAt,
    LocalDateTime paidAt,
    LocalDateTime shippedAt,
    LocalDateTime deliveredAt,
    LocalDateTime cancelledAt,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal totalAmount,
    String couponCode,
    String razorpayOrderId,
    ShippingAddressResponse shippingAddress,
    List<Item> items,
    boolean canCancel
) {
    public record Item(
        int productId,
        String productName,
        String imageUrl,
        int quantity,
        BigDecimal priceAtPurchase
    ) {
        static Item from(OrderItem item) {
            return new Item(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getImageUrl(),
                item.getQuantity(),
                item.getPriceAtPurchase()
            );
        }
    }

    public record ShippingAddressResponse(
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
    ) {
        static ShippingAddressResponse from(ShippingAddress a) {
            // Hibernate gives an all-null embeddable back as null; legacy orders hit this.
            if (a == null || a.getFullName() == null) return null;
            return new ShippingAddressResponse(
                a.getFullName(),
                a.getPhone(),
                a.getLine1(),
                a.getLine2(),
                a.getCity(),
                a.getState(),
                a.getPostalCode(),
                a.getCountry()
            );
        }
    }

    /** Must be called inside a transaction: it walks the lazy items collection. */
    public static OrderResponse from(Order o) {
        BigDecimal total = o.getTotalAmount();
        // Orders from before coupons existed only have a total.
        BigDecimal subtotal = o.getSubtotal() != null ? o.getSubtotal() : total;
        BigDecimal discount = o.getDiscount() != null ? o.getDiscount() : BigDecimal.ZERO;
        return new OrderResponse(
            o.getId(),
            o.getStatus(),
            o.getUser().getUsername(),
            o.getCreatedAt(),
            o.getPaidAt(),
            o.getShippedAt(),
            o.getDeliveredAt(),
            o.getCancelledAt(),
            subtotal,
            discount,
            total,
            o.getCouponCode(),
            o.getRazorpayOrderId(),
            ShippingAddressResponse.from(o.getShippingAddress()),
            o.getItems().stream().map(Item::from).toList(),
            o.getStatus() != null && o.getStatus().isCancellableByUser()
        );
    }
}
