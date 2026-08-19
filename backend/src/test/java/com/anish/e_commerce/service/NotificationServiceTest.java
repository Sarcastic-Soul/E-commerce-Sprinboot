package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.Notification;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.NotificationRepo;
import com.anish.e_commerce.repo.WishlistRepo;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepo notificationRepo;

    @Mock
    private WishlistRepo wishlistRepo;

    @InjectMocks
    private NotificationService notificationService;

    private Notification ownedByAlice;

    @BeforeEach
    void setUp() {
        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");

        ownedByAlice = new Notification();
        ownedByAlice.setId(100L);
        ownedByAlice.setUser(alice);
        ownedByAlice.setMessage("Back in stock");
    }

    @Test
    void testMarkAsRead_OwnerCanMarkTheirOwnNotification() {
        when(notificationRepo.findById(100L)).thenReturn(
            Optional.of(ownedByAlice)
        );

        notificationService.markAsRead(100L, 1L);

        assertTrue(ownedByAlice.isRead());
        verify(notificationRepo, times(1)).save(ownedByAlice);
    }

    @Test
    void testMarkAsRead_RejectsAnotherUsersNotification() {
        when(notificationRepo.findById(100L)).thenReturn(
            Optional.of(ownedByAlice)
        );

        // Mallory (id 2) tries to mark Alice's notification read
        assertThrows(AccessDeniedException.class, () ->
            notificationService.markAsRead(100L, 2L)
        );

        assertFalse(ownedByAlice.isRead());
        verify(notificationRepo, never()).save(any(Notification.class));
    }

    @Test
    void testMarkAsRead_UnknownNotificationIsNotFound() {
        when(notificationRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            notificationService.markAsRead(999L, 1L)
        );

        verify(notificationRepo, never()).save(any(Notification.class));
    }
}
