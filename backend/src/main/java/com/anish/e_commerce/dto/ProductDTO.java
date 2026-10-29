package com.anish.e_commerce.dto;

import com.anish.e_commerce.model.Product.ProductCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ProductDTO {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Category is required")
    private ProductCategory category;

    private boolean available;

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;

    private String imageUrl;

    // Extra gallery images; null leaves the current gallery untouched on update.
    @Size(max = 6, message = "At most 6 extra images")
    private List<String> images;
}
