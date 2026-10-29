package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepo reviewRepo;

    @Mock
    private ProductRepo productRepo;

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private ReviewService reviewService;

    private Product product;
    private User user;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(3);
        product.setName("CyberBackpack");
        user = new User();
        user.setId(1L);
        user.setUsername("bob");
    }

    private static ReviewRequest req(int rating, String comment) {
        ReviewRequest r = new ReviewRequest();
        r.setRating(rating);
        r.setComment(comment);
        return r;
    }

    @Test
    void nonBuyerCannotReview() {
        when(productRepo.findById(3)).thenReturn(Optional.of(product));
        when(orderRepo.hasPurchased("bob", 3, OrderStatus.PURCHASED)).thenReturn(false);

        BusinessException e = assertThrows(
            BusinessException.class,
            () -> reviewService.upsert(3, "bob", req(5, "great"))
        );
        assertEquals("Only buyers can review", e.getMessage());
        verify(reviewRepo, never()).saveAndFlush(any());
        verify(productRepo, never()).updateRating(anyInt(), anyDouble(), anyInt());
    }

    @Test
    void eligibilityOnlyCountsPaidShippedDelivered() {
        when(orderRepo.hasPurchased(eq("bob"), eq(3), any())).thenReturn(true);
        reviewService.canReview("bob", 3);
        verify(orderRepo).hasPurchased(
            eq("bob"),
            eq(3),
            argThat(s ->
                s.containsAll(List.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED)) &&
                s.size() == 3
            )
        );
    }

    @Test
    void buyerCreatesReviewAndRatingIsRecomputed() {
        when(productRepo.findById(3)).thenReturn(Optional.of(product));
        when(orderRepo.hasPurchased("bob", 3, OrderStatus.PURCHASED)).thenReturn(true);
        when(reviewRepo.findByUser_UsernameAndProduct_Id("bob", 3)).thenReturn(Optional.empty());
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(user));
        when(reviewRepo.saveAndFlush(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            r.setId(9L);
            return r;
        });
        // AVG comes back as a Double from the database.
        when(reviewRepo.ratingSummary(3)).thenReturn(
            List.<Object[]>of(new Object[] { 4.333333, 3L })
        );

        ReviewResponse r = reviewService.upsert(3, "bob", req(4, "  solid  "));

        assertEquals(9L, r.id());
        assertEquals(4, r.rating());
        assertEquals("solid", r.comment());
        assertEquals("bob", r.username());
        verify(productRepo).updateRating(3, 4.33, 3);
    }

    @Test
    void secondPostUpdatesTheExistingReview() {
        Review existing = new Review();
        existing.setId(9L);
        existing.setUser(user);
        existing.setProduct(product);
        existing.setRating(2);
        existing.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(productRepo.findById(3)).thenReturn(Optional.of(product));
        when(orderRepo.hasPurchased("bob", 3, OrderStatus.PURCHASED)).thenReturn(true);
        when(reviewRepo.findByUser_UsernameAndProduct_Id("bob", 3)).thenReturn(Optional.of(existing));
        when(reviewRepo.saveAndFlush(existing)).thenReturn(existing);
        when(reviewRepo.ratingSummary(3)).thenReturn(List.<Object[]>of(new Object[] { 5.0, 1L }));

        ReviewResponse r = reviewService.upsert(3, "bob", req(5, null));

        assertEquals(9L, r.id());
        assertEquals(5, r.rating());
        assertEquals("", r.comment());
        verify(userRepo, never()).findByUsername(anyString());
        verify(productRepo).updateRating(3, 5.0, 1);
    }

    @Test
    void deletingLastReviewResetsRatingToZero() {
        Review existing = new Review();
        existing.setUser(user);
        existing.setProduct(product);
        when(reviewRepo.findByUser_UsernameAndProduct_Id("bob", 3)).thenReturn(Optional.of(existing));
        when(reviewRepo.ratingSummary(3)).thenReturn(List.<Object[]>of(new Object[] { 0, 0L }));

        reviewService.deleteMine(3, "bob");

        verify(reviewRepo).delete(existing);
        verify(productRepo).updateRating(3, 0.0, 0);
    }

    @Test
    void reviewingAnInactiveProductIs404() {
        product.setActive(false);
        when(productRepo.findById(3)).thenReturn(Optional.of(product));

        assertThrows(ResourceNotFoundException.class, () -> reviewService.upsert(3, "bob", req(5, "x")));
    }

    @Test
    void mine_ReportsEligibilityAndExistingReview() {
        when(reviewRepo.findByUser_UsernameAndProduct_Id("bob", 3)).thenReturn(Optional.empty());
        when(orderRepo.hasPurchased("bob", 3, OrderStatus.PURCHASED)).thenReturn(true);

        MyReviewResponse mine = reviewService.mine(3, "bob");

        assertTrue(mine.canReview());
        assertNull(mine.review());
    }
}
