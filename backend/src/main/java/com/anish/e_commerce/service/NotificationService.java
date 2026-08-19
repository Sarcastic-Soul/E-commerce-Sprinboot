package com.anish.e_commerce.service;

import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.Notification;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.model.Wishlist;
import com.anish.e_commerce.repo.NotificationRepo;
import com.anish.e_commerce.repo.WishlistRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepo notificationRepo;
    private final WishlistRepo wishlistRepo;

    public void notifyUsersOnRestock(Product product) {
        // Find all users who have this product in their wishlist
        List<Wishlist> wishlists = wishlistRepo.findByProductId(
            product.getId()
        );

        for (Wishlist item : wishlists) {
            Notification notification = new Notification();
            notification.setUser(item.getUser());
            notification.setMessage(
                "Good news! " + product.getName() + " is back in stock."
            );
            notificationRepo.save(notification);
        }
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepo
            .findById(notificationId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Notification not found with id: " + notificationId
                )
            );

        // Without this check any authenticated user could mark anyone's notification read.
        if (!notification.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                "Notification does not belong to the current user"
            );
        }

        notification.setRead(true);
        notificationRepo.save(notification);
    }
}
