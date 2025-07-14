package com.anish.e_commerce.dto;

import com.anish.e_commerce.model.Product.ProductCategory;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartItemResponse {
    private int id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private int quantity;
}
