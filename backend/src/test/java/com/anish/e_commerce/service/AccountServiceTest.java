package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.dto.PasswordChangeRequest;
import com.anish.e_commerce.dto.ProfileResponse;
import com.anish.e_commerce.exception.BusinessException;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.AddressRepo;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ReviewRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private ReviewRepo reviewRepo;

    @Mock
    private AddressRepo addressRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AccountService accountService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(4L);
        user.setUsername("bob");
        user.setPassword("old-hash");
        user.setRoles(Set.of("USER"));
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(user));
    }

    private static PasswordChangeRequest req(String current, String next) {
        PasswordChangeRequest r = new PasswordChangeRequest();
        r.setCurrentPassword(current);
        r.setNewPassword(next);
        return r;
    }

    @Test
    void wrongCurrentPasswordIsRejected() {
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        BusinessException e = assertThrows(
            BusinessException.class,
            () -> accountService.changePassword("bob", req("wrong", "newpassword1"))
        );

        assertEquals("Current password is incorrect", e.getMessage());
        verify(userRepo, never()).save(any());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void tooShortNewPasswordIsRejected() {
        when(passwordEncoder.matches("right", "old-hash")).thenReturn(true);

        assertThrows(
            BusinessException.class,
            () -> accountService.changePassword("bob", req("right", "short"))
        );
        verify(userRepo, never()).save(any());
    }

    @Test
    void successStoresNewHashAndRevokesRefreshTokens() {
        when(passwordEncoder.matches("right", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("newpassword1")).thenReturn("new-hash");

        accountService.changePassword("bob", req("right", "newpassword1"));

        assertEquals("new-hash", user.getPassword());
        verify(userRepo).save(user);
        verify(refreshTokenService).deleteByUserId(4L);
    }

    @Test
    void profileAggregatesCounts() {
        when(orderRepo.countByUser_Username("bob")).thenReturn(3L);
        when(reviewRepo.countByUser_Username("bob")).thenReturn(1L);
        when(addressRepo.countByUser_Username("bob")).thenReturn(2L);

        ProfileResponse p = accountService.profile("bob");

        assertEquals("bob", p.username());
        assertEquals(Set.of("USER"), p.roles());
        assertEquals(3L, p.orderCount());
        assertEquals(1L, p.reviewCount());
        assertEquals(2L, p.addressCount());
    }
}
