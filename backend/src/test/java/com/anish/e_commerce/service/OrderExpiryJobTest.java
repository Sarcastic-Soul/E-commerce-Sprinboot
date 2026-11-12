package com.anish.e_commerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderExpiryJobTest {

    @Mock
    OrderService orderService;

    @InjectMocks
    OrderExpiryJob job;

    @Test
    void sweep_countsOnlyOrdersActuallyExpired_andSurvivesOneFailure() {
        when(orderService.findStalePendingOrderIds()).thenReturn(List.of(1L, 2L, 3L));
        when(orderService.expireOrder(1L)).thenReturn(true);
        when(orderService.expireOrder(2L)).thenThrow(new RuntimeException("optimistic lock"));
        when(orderService.expireOrder(3L)).thenReturn(false);

        assertThat(job.sweep()).isEqualTo(1);
        verify(orderService).expireOrder(3L);
    }

    @Test
    void sweepIfDue_runsOncePerInterval() {
        when(orderService.findStalePendingOrderIds()).thenReturn(List.of());

        job.sweepIfDue();
        job.sweepIfDue();

        verify(orderService, times(1)).findStalePendingOrderIds();
    }

    @Test
    void sweepIfDue_neverThrows() {
        when(orderService.findStalePendingOrderIds()).thenThrow(new RuntimeException("db down"));

        job.sweepIfDue();
    }
}
