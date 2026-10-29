package com.anish.e_commerce.controller;

import com.anish.e_commerce.dto.MyReviewResponse;
import com.anish.e_commerce.dto.ReviewRequest;
import com.anish.e_commerce.dto.ReviewResponse;
import com.anish.e_commerce.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * GET of the list is public (see SecurityConfig: only GETs under /api/product/**
 * are open, and /reviews/me is carved back out). Writes need a logged-in buyer.
 */
@RestController
@RequestMapping("/api/product/{productId}/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> list(
        @PathVariable int productId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(reviewService.list(productId, page, size));
    }

    @GetMapping("/me")
    public ResponseEntity<MyReviewResponse> mine(
        @PathVariable int productId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(reviewService.mine(productId, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> upsert(
        @PathVariable int productId,
        @Valid @RequestBody ReviewRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            reviewService.upsert(productId, authentication.getName(), request)
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMine(
        @PathVariable int productId,
        Authentication authentication
    ) {
        reviewService.deleteMine(productId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
