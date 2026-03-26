package com.anish.e_commerce.dto;

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

        public DailyOrderStat(String date, long count) {
            this.date = date;
            this.count = count;
        }
    }
}
