package com.anish.e_commerce.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.anish.e_commerce.jwt.JwtCookieProperties;
import com.anish.e_commerce.jwt.JwtUtil;
import com.anish.e_commerce.model.RefreshToken;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.UserRepo;
import com.anish.e_commerce.service.RefreshTokenService;
import com.anish.e_commerce.service.UserDetailsServiceImpl;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers the auth flows added by the cookie rework: tokens now travel as httpOnly
 * Set-Cookie headers instead of the response body, and /me lets the SPA re-derive
 * the session from the cookie alone. Signature/permission tests for the plain
 * request-response shape (signup/login/refresh/logout wiring) that predate that
 * rework, since this controller previously had no test coverage at all.
 */
@WebMvcTest(AuthController.class)
@Import({
    AuthControllerTest.PermitAllSecurityConfig.class,
    JwtCookieProperties.class,
})
class AuthControllerTest {

    @TestConfiguration
    static class PermitAllSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepo userRepo;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthenticationManager authManager;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    // JwtUtil/UserDetailsServiceImpl are also required by the real JwtAuthFilter,
    // which @WebMvcTest constructs as a registered Filter bean.
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private User user(String username, Set<String> roles) {
        User u = new User();
        u.setId(1L);
        u.setUsername(username);
        u.setPassword("hashed");
        u.setRoles(roles);
        return u;
    }

    @Test
    void signup_RegistersNewUser() throws Exception {
        when(userRepo.findByUsername("newbie")).thenReturn(Optional.empty());
        when(userRepo.count()).thenReturn(3L);

        AuthController.AuthRequest request = new AuthController.AuthRequest();
        request.setUsername("newbie");
        request.setPassword("password1");

        mockMvc
            .perform(
                post("/api/auth/signup")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("User registered successfully"));

        verify(userRepo, times(1)).save(any(User.class));
    }

    @Test
    void signup_RejectsDuplicateUsernameWithConsistentErrorShape() throws Exception {
        when(userRepo.findByUsername("alice")).thenReturn(
            Optional.of(user("alice", Set.of("USER")))
        );

        mockMvc
            .perform(
                post("/api/auth/signup")
                    .contentType("application/json")
                    .content("{\"username\":\"alice\",\"password\":\"password1\"}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Username already exists"));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void login_SetsHttpOnlyCookiesAndReturnsUserInfoWithoutTokens() throws Exception {
        when(
            authManager.authenticate(any())
        ).thenReturn(null); // authentication succeeding is enough; result is unused
        when(userRepo.findByUsername("alice")).thenReturn(
            Optional.of(user("alice", Set.of("USER")))
        );
        when(jwtUtil.generateToken(eq("alice"), any())).thenReturn("access.jwt.token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(
            RefreshToken.builder()
                .token("refresh-token-value")
                .userId(1L)
                .expiration(604800L)
                .build()
        );

        var result = mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType("application/json")
                    .content("{\"username\":\"alice\",\"password\":\"password1\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.roles[0]").value("USER"))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertCookiePresent(setCookies, "access_token=access.jwt.token");
        assertCookiePresent(setCookies, "refresh_token=refresh-token-value");
        assertCookiePresent(setCookies, "HttpOnly");
    }

    @Test
    void login_RejectsBadCredentials() throws Exception {
        when(authManager.authenticate(any())).thenThrow(
            new BadCredentialsException("bad creds")
        );

        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType("application/json")
                    .content("{\"username\":\"alice\",\"password\":\"wrongpass\"}")
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void refresh_RejectsRequestWithNoRefreshCookie() throws Exception {
        mockMvc
            .perform(post("/api/auth/refresh"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("No refresh token present."));
    }

    @Test
    void refresh_IssuesNewAccessTokenForAValidRefreshCookie() throws Exception {
        when(refreshTokenService.findByToken("refresh-token-value")).thenReturn(
            Optional.of(
                RefreshToken.builder()
                    .token("refresh-token-value")
                    .userId(1L)
                    .expiration(604800L)
                    .build()
            )
        );
        when(refreshTokenService.verifyExpiration(any())).thenAnswer(
            i -> i.getArgument(0)
        );
        when(userRepo.findById(1L)).thenReturn(
            Optional.of(user("alice", Set.of("USER")))
        );
        when(jwtUtil.generateToken(eq("alice"), any())).thenReturn("new.access.token");

        var result = mockMvc
            .perform(
                post("/api/auth/refresh")
                    .cookie(
                        new jakarta.servlet.http.Cookie(
                            "refresh_token",
                            "refresh-token-value"
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("alice"))
            .andReturn();

        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertCookiePresent(setCookies, "access_token=new.access.token");
    }

    @Test
    void refresh_RejectsUnknownOrExpiredToken() throws Exception {
        when(refreshTokenService.findByToken("stale")).thenReturn(Optional.empty());

        mockMvc
            .perform(
                post("/api/auth/refresh")
                    .cookie(new jakarta.servlet.http.Cookie("refresh_token", "stale"))
            )
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.message").value(
                    "Refresh token is not in database or expired!"
                )
            );
    }

    @Test
    void logout_ClearsCookiesAndRevokesRefreshToken() throws Exception {
        var result = mockMvc
            .perform(
                post("/api/auth/logout")
                    .cookie(new jakarta.servlet.http.Cookie("refresh_token", "abc"))
            )
            .andExpect(status().isOk())
            .andReturn();

        verify(refreshTokenService, times(1)).deleteByToken("abc");
        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        assertCookiePresent(setCookies, "access_token=");
        assertCookiePresent(setCookies, "Max-Age=0");
    }

    @Test
    void me_RejectsAnUnauthenticatedCaller() throws Exception {
        mockMvc
            .perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void me_ReturnsTheAuthenticatedUsersUsernameAndRoles() throws Exception {
        mockMvc
            .perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    private static void assertCookiePresent(List<String> setCookies, String fragment) {
        org.junit.jupiter.api.Assertions.assertTrue(
            setCookies.stream().anyMatch(c -> c.contains(fragment)),
            () -> "Expected a Set-Cookie header containing '" + fragment + "' in " + setCookies
        );
    }
}
