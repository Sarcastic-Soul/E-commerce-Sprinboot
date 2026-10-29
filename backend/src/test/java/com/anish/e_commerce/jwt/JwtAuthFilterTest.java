package com.anish.e_commerce.jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private JwtAuthFilter newFilter() {
        return new JwtAuthFilter(jwtUtil, userDetailsService);
    }

    @Test
    void authenticatesFromBearerHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.extractUsername("good-token")).thenReturn("alice");
        when(jwtUtil.isTokenValid("good-token")).thenReturn(true);
        UserDetails alice = new User(
            "alice",
            "unused",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(alice);

        newFilter().doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(
            "alice",
            SecurityContextHolder.getContext().getAuthentication().getName()
        );
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void authenticatesFromAccessTokenCookieWhenNoHeaderPresent()
        throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
            new Cookie(JwtCookieProperties.ACCESS_COOKIE, "cookie-token")
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.extractUsername("cookie-token")).thenReturn("bob");
        when(jwtUtil.isTokenValid("cookie-token")).thenReturn(true);
        UserDetails bob = new User(
            "bob",
            "unused",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(userDetailsService.loadUserByUsername("bob")).thenReturn(bob);

        newFilter().doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(
            "bob",
            SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }

    @Test
    void leavesContextEmptyWhenNoTokenPresentAnywhere() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        newFilter().doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void leavesContextEmptyForAnInvalidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.extractUsername("bad-token")).thenThrow(
            new io.jsonwebtoken.security.SignatureException("bad signature")
        );

        newFilter().doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }
}
