package com.anish.e_commerce.service;

import com.anish.e_commerce.dto.AdminStatsResponse;
import com.anish.e_commerce.model.Order;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepo userRepo;
    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;

    public AdminStatsResponse getStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        // 1. Get Totals
        stats.setTotalUsers(userRepo.count());
        stats.setTotalProducts(productRepo.count());

        // 2. Process Orders for the Chart
        List<Order> allOrders = orderRepo.findAll();
        stats.setTotalOrders(allOrders.size());

        // Format dates to "Mar 26" style
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        // Group orders by date and count them
        Map<String, Long> groupedOrders = allOrders
            .stream()
            .collect(
                Collectors.groupingBy(
                    o -> o.getCreatedAt().format(formatter),
                    Collectors.counting()
                )
            );

        // Calculate Total Revenue
        java.math.BigDecimal revenue = allOrders
            .stream()
            .map(Order::getTotalAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        stats.setTotalRevenue(revenue);

        // Calculate Out of Stock Products (quantity is 0 or marked unavailable)
        long outOfStock = productRepo
            .findAll()
            .stream()
            .filter(p -> !p.isAvailable() || p.getQuantity() == 0)
            .count();
        stats.setOutOfStockProducts(outOfStock);

        // Convert Map to List of DTOs for the frontend chart
        List<AdminStatsResponse.DailyOrderStat> trends = groupedOrders
            .entrySet()
            .stream()
            .map(entry ->
                new AdminStatsResponse.DailyOrderStat(
                    entry.getKey(),
                    entry.getValue()
                )
            )
            .collect(Collectors.toList());

        stats.setOrdersOverTime(trends);

        return stats;
    }
}
