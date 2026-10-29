package com.anish.e_commerce.dto;

import com.anish.e_commerce.model.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
    Long id,
    int productId,
    String username,
    int rating,
    String comment,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
            r.getId(),
            r.getProduct().getId(),
            r.getUser().getUsername(),
            r.getRating(),
            r.getComment(),
            r.getCreatedAt(),
            r.getUpdatedAt()
        );
    }
}
