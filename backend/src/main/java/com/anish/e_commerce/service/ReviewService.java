package com.anish.e_commerce.service;

import com.anish.e_commerce.dto.MyReviewResponse;
import com.anish.e_commerce.dto.ReviewRequest;
import com.anish.e_commerce.dto.ReviewResponse;
import com.anish.e_commerce.exception.BusinessException;
import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.OrderStatus;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.model.Review;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.OrderRepo;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.ReviewRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepo reviewRepo;
    private final ProductRepo productRepo;
    private final OrderRepo orderRepo;
    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> list(int productId, int page, int size) {
        return reviewRepo
            .findByProduct_IdOrderByCreatedAtDescIdDesc(productId, PageRequest.of(page, size))
            .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public MyReviewResponse mine(int productId, String username) {
        ReviewResponse review = reviewRepo
            .findByUser_UsernameAndProduct_Id(username, productId)
            .map(ReviewResponse::from)
            .orElse(null);
        return new MyReviewResponse(canReview(username, productId), review);
    }

    public boolean canReview(String username, int productId) {
        return orderRepo.hasPurchased(username, productId, OrderStatus.PURCHASED);
    }

    /** Creates or updates the caller's single review of this product. */
    @Transactional
    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#productId"),
        }
    )
    public ReviewResponse upsert(int productId, String username, ReviewRequest request) {
        Product product = productRepo
            .findById(productId)
            .filter(Product::isActive)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!canReview(username, productId)) {
            throw new BusinessException("Only buyers can review");
        }

        LocalDateTime now = LocalDateTime.now();
        Review review = reviewRepo
            .findByUser_UsernameAndProduct_Id(username, productId)
            .orElseGet(() -> {
                User user = userRepo
                    .findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
                Review r = new Review();
                r.setUser(user);
                r.setProduct(product);
                r.setCreatedAt(now);
                return r;
            });
        review.setRating(request.getRating());
        String comment = request.getComment() == null ? "" : request.getComment().trim();
        review.setComment(comment);
        review.setUpdatedAt(now);

        Review saved = reviewRepo.saveAndFlush(review);
        recomputeRating(productId);
        return ReviewResponse.from(saved);
    }

    @Transactional
    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#productId"),
        }
    )
    public void deleteMine(int productId, String username) {
        Review review = reviewRepo
            .findByUser_UsernameAndProduct_Id(username, productId)
            .orElseThrow(() -> new ResourceNotFoundException("You have not reviewed this product"));
        reviewRepo.delete(review);
        reviewRepo.flush();
        recomputeRating(productId);
    }

    /**
     * Recomputed from an aggregate rather than adjusted incrementally, so the stored
     * figures can never drift from the reviews table.
     */
    void recomputeRating(int productId) {
        List<Object[]> rows = reviewRepo.ratingSummary(productId);
        Object[] row = rows.isEmpty() ? new Object[] { 0, 0L } : rows.get(0);
        double avg = BigDecimal.valueOf(((Number) row[0]).doubleValue())
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
        int count = ((Number) row[1]).intValue();
        productRepo.updateRating(productId, avg, count);
    }
}
