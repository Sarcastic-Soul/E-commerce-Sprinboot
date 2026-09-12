package com.anish.e_commerce.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "cart_item",
    indexes = { @Index(name = "idx_cart_item_cart_id", columnList = "cart_id") }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private int quantity;
}
