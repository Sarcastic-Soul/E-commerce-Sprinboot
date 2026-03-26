package com.anish.e_commerce.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
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
