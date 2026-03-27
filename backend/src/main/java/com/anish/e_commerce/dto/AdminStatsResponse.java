package com.anish.e_commerce.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class AdminStatsResponse {

    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private List<DailyOrderStat> ordersOverTime;
    private java.math.BigDecimal totalRevenue;
    private long outOfStockProducts;

    @Data
    public static class DailyOrderStat {

        private String date;
        private long count;
        private BigDecimal revenue;

        public DailyOrderStat(String date, long count, BigDecimal revenue) {
            this.date = date;
            this.count = count;
            this.revenue = revenue;
        }
    }
}
