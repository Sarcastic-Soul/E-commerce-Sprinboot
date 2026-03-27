package com.anish.e_commerce.service;

import com.anish.e_commerce.dto.AdminStatsResponse;
import com.anish.e_commerce.model.Order;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.math.BigDecimal;
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

        // Calculate Overall Total Revenue
        BigDecimal totalRevenue = allOrders
            .stream()
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);

        // Format dates to "Mar 26" style
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        // Group orders by date (gives us Map<String, List<Order>>)
        Map<String, List<Order>> ordersByDate = allOrders
            .stream()
            .collect(
                Collectors.groupingBy(o -> o.getCreatedAt().format(formatter))
            );

        // Loop through each date, count the orders, and sum their revenue
        List<AdminStatsResponse.DailyOrderStat> trends = ordersByDate
            .entrySet()
            .stream()
            .map(entry -> {
                String date = entry.getKey();
                long count = entry.getValue().size();

                // Sum the revenue for this specific day
                BigDecimal dailyRevenue = entry
                    .getValue()
                    .stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new AdminStatsResponse.DailyOrderStat(
                    date,
                    count,
                    dailyRevenue
                );
            })
            .collect(Collectors.toList());

        stats.setOrdersOverTime(trends);

        // Calculate Out of Stock Products
        long outOfStock = productRepo
            .findAll()
            .stream()
            .filter(p -> !p.isAvailable() || p.getQuantity() == 0)
            .count();
        stats.setOutOfStockProducts(outOfStock);

        return stats;
    }
}
