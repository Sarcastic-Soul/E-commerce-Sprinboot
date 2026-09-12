package com.anish.e_commerce.service;

import com.anish.e_commerce.dto.AdminStatsResponse;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
     * extra query per order.
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        stats.setTotalUsers(userRepo.count());
        stats.setTotalProducts(productRepo.count());
        stats.setTotalOrders(orderRepo.count());
        stats.setTotalRevenue(orderRepo.sumTotalRevenue());
        stats.setOutOfStockProducts(productRepo.countOutOfStock());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        // Already grouped and ordered by the database — no in-memory sort needed.
        List<AdminStatsResponse.DailyOrderStat> trends = orderRepo
            .findDailyOrderStats()
            .stream()
            .map(row ->
                new AdminStatsResponse.DailyOrderStat(
                    LocalDate.of(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue()
                    ).format(formatter),
                    ((Number) row[3]).longValue(),
                    (BigDecimal) row[4]
                )
            )
            .toList();

        stats.setOrdersOverTime(trends);
        return stats;
    }
}
