package com.anish.e_commerce.service;

import com.anish.e_commerce.model.Notification;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.model.Wishlist;
import com.anish.e_commerce.repo.NotificationRepo;
import com.anish.e_commerce.repo.WishlistRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    public void markAsRead(Long notificationId) {
        notificationRepo
            .findById(notificationId)
            .ifPresent(notification -> {
                notification.setRead(true);
                notificationRepo.save(notification);
            });
    }
}
