package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.dto.AdminStatsResponse;
import com.anish.e_commerce.model.OrderStatus;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.math.BigDecimal;
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
        when(userRepo.count()).thenReturn(5L);
        when(productRepo.countByActiveTrue()).thenReturn(10L);
        when(productRepo.countOutOfStock()).thenReturn(1L);
        when(orderRepo.count()).thenReturn(3L);
        when(orderRepo.sumRevenue(OrderStatus.PURCHASED)).thenReturn(new BigDecimal("300.00"));

        // year, month, day, count, revenue — as the aggregate query returns them.
        when(orderRepo.findDailyOrderStats(OrderStatus.PURCHASED)).thenReturn(
            List.of(
                new Object[] { 2026, 3, 25, 1L, new BigDecimal("100.00") },
                new Object[] { 2026, 3, 26, 2L, new BigDecimal("200.00") }
            )
        );

        AdminStatsResponse stats = adminService.getStats();

        assertEquals(5L, stats.getTotalUsers());
        assertEquals(10L, stats.getTotalProducts());
        assertEquals(3L, stats.getTotalOrders());
        assertEquals(1L, stats.getOutOfStockProducts());
        assertEquals(new BigDecimal("300.00"), stats.getTotalRevenue());

        assertEquals(2, stats.getOrdersOverTime().size());

        AdminStatsResponse.DailyOrderStat first = stats
            .getOrdersOverTime()
            .get(0);
        assertEquals("Mar 25", first.getDate());
        assertEquals(1L, first.getCount());
        assertEquals(new BigDecimal("100.00"), first.getRevenue());

        AdminStatsResponse.DailyOrderStat second = stats
            .getOrdersOverTime()
            .get(1);
        assertEquals("Mar 26", second.getDate());
        assertEquals(2L, second.getCount());
        assertEquals(new BigDecimal("200.00"), second.getRevenue());
    }

    /**
     * Regression: the old implementation grouped with Collectors.groupingBy, which
     * returns a HashMap, so the chart's X-axis came back in arbitrary order. The
     * database now does the ordering — this asserts the service preserves it.
     */
    @Test
    void testGetStats_OrdersOverTimeIsChronological() {
        when(userRepo.count()).thenReturn(0L);
        when(productRepo.countByActiveTrue()).thenReturn(0L);
        when(productRepo.countOutOfStock()).thenReturn(0L);
        when(orderRepo.count()).thenReturn(4L);
        when(orderRepo.sumRevenue(OrderStatus.PURCHASED)).thenReturn(BigDecimal.ZERO);

        // Spans a year boundary — dates that "MMM dd" alone would collapse together.
        when(orderRepo.findDailyOrderStats(OrderStatus.PURCHASED)).thenReturn(
            List.of(
                new Object[] { 2025, 12, 30, 1L, new BigDecimal("10.00") },
                new Object[] { 2026, 1, 2, 1L, new BigDecimal("20.00") },
                new Object[] { 2026, 3, 25, 1L, new BigDecimal("30.00") },
                new Object[] { 2026, 12, 30, 1L, new BigDecimal("40.00") }
            )
        );

        List<AdminStatsResponse.DailyOrderStat> trends = adminService
            .getStats()
            .getOrdersOverTime();

        assertEquals(
            List.of("Dec 30", "Jan 02", "Mar 25", "Dec 30"),
            trends.stream().map(AdminStatsResponse.DailyOrderStat::getDate).toList()
        );

        // Same "MMM dd" label, different years — kept as distinct buckets.
        assertEquals(4, trends.size());
        assertEquals(new BigDecimal("10.00"), trends.get(0).getRevenue());
        assertEquals(new BigDecimal("40.00"), trends.get(3).getRevenue());
    }

    @Test
    void testExportOrdersCsv_EscapesQuotesCommasAndNewlines() {
        com.anish.e_commerce.model.User user = new com.anish.e_commerce.model.User();
        user.setUsername("bob");
        com.anish.e_commerce.model.Product product = new com.anish.e_commerce.model.Product();
        product.setName("Duck, \"Deluxe\"\nEdition");
        com.anish.e_commerce.model.Order order = new com.anish.e_commerce.model.Order();
        order.setId(1L);
        order.setUser(user);
        order.setStatus(OrderStatus.PAID);
        order.setTotalAmount(new BigDecimal("10.00"));
        com.anish.e_commerce.model.OrderItem item = new com.anish.e_commerce.model.OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        order.getItems().add(item);
        order.setShippingAddress(
            new com.anish.e_commerce.model.ShippingAddress(
                "B", "123", "l1", null, "=cmd", "S", "123", "India"
            )
        );
        when(orderRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        String csv = adminService.exportOrdersCsv();

        String[] lines = csv.split("\r\n", 2);
        assertEquals(
            "id,date,username,status,items,subtotal,discount,total,coupon,city",
            lines[0]
        );
        // Quoted, inner quotes doubled, newline kept inside the quotes.
        assertTrue(csv.contains("\"Duck, \"\"Deluxe\"\"\nEdition x2\""));
        // Legacy order without subtotal falls back to the total, discount 0.
        assertTrue(csv.contains(",10.00,0,10.00,,"));
        // Formula-looking cells are neutralised.
        assertTrue(csv.contains("'=cmd"));
    }
}
