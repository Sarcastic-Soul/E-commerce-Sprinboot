package com.anish.e_commerce.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Data
@NoArgsConstructor
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String description;
    private String brand;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Column(name = "created_at")
    private Date createdAt;

    private boolean available;
    private int quantity;
    private String imageUrl;

    // Soft delete: order history still points at the row, so it is hidden, not removed.
    // The column defaults let `ddl-auto=update` add these to a table that has rows.
    @Column(columnDefinition = "boolean default true not null")
    private boolean active = true;

    // Denormalised from reviews so listing/sorting by rating needs no join.
    @Column(columnDefinition = "double precision default 0 not null")
    private double averageRating;

    @Column(columnDefinition = "integer default 0 not null")
    private int reviewCount;

    // Extra gallery images; the main one stays in imageUrl. EAGER because the
    // entity is cached and serialized outside any session.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "product_images",
        joinColumns = @JoinColumn(name = "product_id")
    )
    @OrderColumn(name = "image_order")
    @Column(name = "url", length = 1000)
    @BatchSize(size = 50)
    private List<String> images = new ArrayList<>();

    /** The original all-args shape, kept for the seeder and tests. */
    public Product(
        int id,
        String name,
        String description,
        String brand,
        BigDecimal price,
        ProductCategory category,
        Date createdAt,
        boolean available,
        int quantity,
        String imageUrl
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.price = price;
        this.category = category;
        this.createdAt = createdAt;
        this.available = available;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    public enum ProductCategory {
        ELECTRONICS,
        FASHION,
        HOME_KITCHEN,
        BEAUTY_PERSONAL_CARE,
        BOOKS_STATIONERY,
        HEALTH_WELLNESS,
        TOYS_GAMES,
        SPORTS_OUTDOORS,
        AUTOMOTIVE,
        GROCERIES_GOURMET_FOOD,
    }
}
