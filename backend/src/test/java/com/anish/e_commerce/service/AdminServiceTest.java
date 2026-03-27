package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.dto.AdminStatsResponse;
import com.anish.e_commerce.model.Order;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private ProductRepo productRepo;

    @Mock
    private OrderRepo orderRepo;

    @InjectMocks
    private AdminService adminService;

    @Test
    void testGetStats_CalculatesCorrectly() {
        // Arrange: Mock Users and Products
        when(userRepo.count()).thenReturn(5L);
        when(productRepo.count()).thenReturn(10L);

        // Arrange: Mock Out-of-Stock Products
        Product availableProduct = new Product();
        availableProduct.setAvailable(true);
        availableProduct.setQuantity(5);

        Product outOfStockProduct = new Product();
        outOfStockProduct.setAvailable(false);
        outOfStockProduct.setQuantity(0);

        when(productRepo.findAll()).thenReturn(
            List.of(availableProduct, outOfStockProduct)
        );

        // Arrange: Mock Orders (Two orders today, one yesterday)
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime yesterday = today.minusDays(1);

        Order order1 = new Order();
        order1.setTotalAmount(new BigDecimal("50.00"));
        order1.setCreatedAt(today);

        Order order2 = new Order();
        order2.setTotalAmount(new BigDecimal("150.00"));
        order2.setCreatedAt(today);

        Order order3 = new Order();
        order3.setTotalAmount(new BigDecimal("100.00"));
        order3.setCreatedAt(yesterday);

        when(orderRepo.findAll()).thenReturn(List.of(order1, order2, order3));

        // Act
        AdminStatsResponse stats = adminService.getStats();

        // Assert: Basic Counts
        assertEquals(5L, stats.getTotalUsers());
        assertEquals(10L, stats.getTotalProducts());
        assertEquals(3L, stats.getTotalOrders());
        assertEquals(1L, stats.getOutOfStockProducts()); // Only 1 product is out of stock

        // Assert: Total Revenue (50 + 150 + 100 = 300)
        assertEquals(new BigDecimal("300.00"), stats.getTotalRevenue());

        // Assert: Chart Trends
        assertEquals(2, stats.getOrdersOverTime().size()); // 2 distinct days

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        String todayString = today.format(formatter);
        String yesterdayString = yesterday.format(formatter);

        // Verify today's aggregated stats (2 orders, $200 total)
        AdminStatsResponse.DailyOrderStat todayStat = stats
            .getOrdersOverTime()
            .stream()
            .filter(s -> s.getDate().equals(todayString))
            .findFirst()
            .orElseThrow();

        assertEquals(2, todayStat.getCount());
        assertEquals(new BigDecimal("200.00"), todayStat.getRevenue());

        // Verify yesterday's aggregated stats (1 order, $100 total)
        AdminStatsResponse.DailyOrderStat yesterdayStat = stats
            .getOrdersOverTime()
            .stream()
            .filter(s -> s.getDate().equals(yesterdayString))
            .findFirst()
            .orElseThrow();

        assertEquals(1, yesterdayStat.getCount());
        assertEquals(new BigDecimal("100.00"), yesterdayStat.getRevenue());
    }
}
