package com.anish.e_commerce.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.config.RateLimitFilter;
import com.anish.e_commerce.service.OrderExpiryJob;
import com.anish.e_commerce.service.RefreshTokenService;
import org.junit.jupiter.api.Test;

class InternalControllerTest {

    private final OrderExpiryJob job = mock(OrderExpiryJob.class);
    private final RefreshTokenService tokens = mock(RefreshTokenService.class);
    private final RateLimitFilter limits = mock(RateLimitFilter.class);

    private InternalController controller(String secret) {
        return new InternalController(job, tokens, limits, secret);
    }

    @Test
    void disabledWithoutSecret() {
        var controller = controller("");
        assertThat(controller.cleanup("Bearer ").getStatusCode().value()).isEqualTo(404);
        verifyNoInteractions(job, tokens, limits);
    }

    @Test
    void rejectsWrongOrMissingToken() {
        var controller = controller("s3cret");
        assertThat(controller.cleanup(null).getStatusCode().value()).isEqualTo(401);
        assertThat(controller.cleanup("Bearer nope").getStatusCode().value()).isEqualTo(401);
        assertThat(controller.cleanup("s3cret").getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(job, tokens, limits);
    }

    @Test
    void runsAllCleanupWithCorrectToken() {
        when(job.sweep()).thenReturn(2);
        when(tokens.deleteExpired()).thenReturn(5);
        when(limits.removeExpiredBuckets()).thenReturn(7);
        var controller = controller("s3cret");

        var response = controller.cleanup("Bearer s3cret");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody())
            .containsEntry("expiredOrders", 2)
            .containsEntry("deletedRefreshTokens", 5)
            .containsEntry("deletedRateLimitBuckets", 7);
    }
}
