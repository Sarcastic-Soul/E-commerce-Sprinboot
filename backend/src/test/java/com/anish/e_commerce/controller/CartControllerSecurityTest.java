package com.anish.e_commerce.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anish.e_commerce.jwt.JwtUtil;
import com.anish.e_commerce.service.CartService;
import com.anish.e_commerce.service.UserDetailsServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The cart is addressed by {username} in the URL. These tests pin that a caller can
 * only ever reach their own cart — otherwise any logged-in user could read, edit or
 * empty someone else's cart just by changing the path.
 */
@WebMvcTest(CartController.class)
@Import(CartControllerSecurityTest.MethodSecurityOnlyConfig.class)
class CartControllerSecurityTest {

    /**
     * Authorization here comes from @PreAuthorize, not from URL rules, so the test
     * chain permits every request and leaves method security to do the work.
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityOnlyConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http)
            throws Exception {
            return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    // @WebMvcTest registers Filter beans, so the real JwtAuthFilter is constructed
    // and needs these. Requests here carry no bearer token, so it just passes through
    // and leaves the @WithMockUser principal in place.
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser(username = "alice")
    void ownerCanReadTheirOwnCart() throws Exception {
        org.mockito.Mockito.when(
            cartService.getCartItemsByUsername("alice")
        ).thenReturn(List.of());

        mockMvc.perform(get("/api/cart/alice")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "mallory")
    void otherUsersCartIsForbidden() throws Exception {
        mockMvc
            .perform(get("/api/cart/alice"))
            .andExpect(status().isForbidden());

        verify(cartService, never()).getCartItemsByUsername(anyString());
    }

    @Test
    @WithMockUser(username = "mallory")
    void cannotAddToAnotherUsersCart() throws Exception {
        mockMvc
            .perform(
                post("/api/cart/alice/add")
                    .param("productId", "1")
                    .param("quantity", "1")
            )
            .andExpect(status().isForbidden());

        verify(cartService, never()).addOrUpdateItem(
            anyString(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    @WithMockUser(username = "mallory")
    void cannotRemoveFromAnotherUsersCart() throws Exception {
        mockMvc
            .perform(delete("/api/cart/alice/remove").param("productId", "1"))
            .andExpect(status().isForbidden());

        verify(cartService, never()).removeItem(
            anyString(),
            org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    @WithMockUser(username = "mallory")
    void cannotClearAnotherUsersCart() throws Exception {
        mockMvc
            .perform(delete("/api/cart/alice/clear"))
            .andExpect(status().isForbidden());

        verify(cartService, never()).clearCart(anyString());
    }
}
