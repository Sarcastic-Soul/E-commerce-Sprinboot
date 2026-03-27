package com.anish.e_commerce.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anish.e_commerce.repo.ProductRepo; // Make sure to import this
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // 👇 ADD THIS: Tricks Spring into bypassing the real database connection
    @MockBean
    private ProductRepo productRepo;

    @Test
    void testPublicApiRateLimiting() throws Exception {
        // 1. Make 30 successful requests
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/api/products")).andExpect(status().isOk());
        }

        // 2. The 31st request should be blocked!
        mockMvc
            .perform(get("/api/products"))
            .andExpect(status().isTooManyRequests());
    }
}
