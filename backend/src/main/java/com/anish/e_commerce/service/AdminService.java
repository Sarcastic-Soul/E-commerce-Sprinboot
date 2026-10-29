package com.anish.e_commerce.service;

import com.anish.e_commerce.dto.AdminStatsResponse;
import com.anish.e_commerce.model.Order;
import com.anish.e_commerce.model.OrderStatus;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import com.anish.e_commerce.util.CsvWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepo userRepo;
    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;

    /**
     * Every figure here is a database aggregate. The previous implementation loaded
     * every order and every product into memory to count and sum them, which grew
     * linearly with the tables and — because Order.items used to be EAGER — fired an
     * extra query per order. Revenue only counts orders that were actually paid for.
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        stats.setTotalUsers(userRepo.count());
        stats.setTotalProducts(productRepo.countByActiveTrue());
        stats.setTotalOrders(orderRepo.count());
        stats.setTotalRevenue(orderRepo.sumRevenue(OrderStatus.PURCHASED));
        stats.setOutOfStockProducts(productRepo.countOutOfStock());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        // Already grouped and ordered by the database — no in-memory sort needed.
        List<AdminStatsResponse.DailyOrderStat> trends = orderRepo
            .findDailyOrderStats(OrderStatus.PURCHASED)
            .stream()
            .map(row ->
                new AdminStatsResponse.DailyOrderStat(
                    LocalDate.of(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue()
                    ).format(formatter),
                    ((Number) row[3]).longValue(),
                    toBigDecimal(row[4])
                )
            )
            .toList();

        stats.setOrdersOverTime(trends);
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Product> lowStock(int threshold) {
        return productRepo.findLowStock(threshold);
    }

    @Transactional(readOnly = true)
    public String exportOrdersCsv() {
        CsvWriter csv = new CsvWriter().row(
            "id", "date", "username", "status", "items",
            "subtotal", "discount", "total", "coupon", "city"
        );
        for (Order o : orderRepo.findAllByOrderByCreatedAtDesc()) {
            String items = o
                .getItems()
                .stream()
                .map(i -> i.getProduct().getName() + " x" + i.getQuantity())
                .collect(Collectors.joining("; "));
            BigDecimal total = o.getTotalAmount();
            csv.row(
                o.getId(),
                o.getCreatedAt(),
                o.getUser().getUsername(),
                o.getStatus(),
                items,
                o.getSubtotal() != null ? o.getSubtotal() : total,
                o.getDiscount() != null ? o.getDiscount() : BigDecimal.ZERO,
                total,
                o.getCouponCode(),
                o.getShippingAddress() != null ? o.getShippingAddress().getCity() : null
            );
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String exportProductsCsv() {
        CsvWriter csv = new CsvWriter().row(
            "id", "name", "brand", "category", "price", "quantity",
            "available", "active", "averageRating", "reviewCount", "createdAt"
        );
        for (Product p : productRepo.findAllByOrderByIdAsc()) {
            csv.row(
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getCategory(),
                p.getPrice(),
                p.getQuantity(),
                p.isAvailable(),
                p.isActive(),
                p.getAverageRating(),
                p.getReviewCount(),
                p.getCreatedAt()
            );
        }
        return csv.toString();
    }

    // SUM over a CASE can come back as a different numeric type per dialect.
    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
