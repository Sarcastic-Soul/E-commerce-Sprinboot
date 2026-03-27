package com.anish.e_commerce.controller;

import com.anish.e_commerce.dto.ProductDTO;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.service.ImageHandleService;
import com.anish.e_commerce.service.ProductService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ImageHandleService imageHandleService;

    @GetMapping("/products")
    public ResponseEntity<Page<Product>> getAllProducts(
        @RequestParam(defaultValue = "0") int page, // Default to page 0 (Spring is 0-indexed)
        @RequestParam(defaultValue = "12") int size // Default to 12 items per page
    ) {
        Page<Product> productPage = productService.getAllProducts(page, size);
        return new ResponseEntity<>(productPage, HttpStatus.OK);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable int id) {
        if (id <= 0) return ResponseEntity.badRequest().build();

        Product product = productService.getProductById(id);
        return product != null
            ? ResponseEntity.ok(product)
            : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/products/filter")
    public ResponseEntity<List<Product>> filterProducts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Product.ProductCategory category,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Boolean available,
        @RequestParam(defaultValue = "asc") String sort
    ) {
        return ResponseEntity.ok(
            productService.getFilteredProducts(
                keyword,
                category,
                minPrice,
                maxPrice,
                available,
                sort
            )
        );
    }

    @PostMapping("/product")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addProduct(
        @Valid @RequestBody ProductDTO productDto
    ) {
        try {
            // Map DTO to Entity
            Product product = new Product();
            product.setName(productDto.getName());
            product.setDescription(productDto.getDescription());
            product.setBrand(productDto.getBrand());
            product.setPrice(productDto.getPrice());
            product.setCategory(productDto.getCategory());
            product.setAvailable(productDto.isAvailable());
            product.setQuantity(productDto.getQuantity());
            product.setImageUrl(productDto.getImageUrl());
            product.setCreatedAt(new Date()); // Set creation date

            return ResponseEntity.status(HttpStatus.CREATED).body(
                productService.addProduct(product)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                "Failed to add product: " + e.getMessage()
            );
        }
    }

    @PutMapping("/product/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(
        @PathVariable int id,
        @Valid @RequestBody ProductDTO productDto
    ) {
        try {
            // Map DTO to Entity
            Product product = new Product();
            product.setId(id); // Ensure the ID is set for the update
            product.setName(productDto.getName());
            product.setDescription(productDto.getDescription());
            product.setBrand(productDto.getBrand());
            product.setPrice(productDto.getPrice());
            product.setCategory(productDto.getCategory());
            product.setAvailable(productDto.isAvailable());
            product.setQuantity(productDto.getQuantity());
            product.setImageUrl(productDto.getImageUrl());

            Product updated = productService.updateProduct(product);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                e.getMessage()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                "Error updating product"
            );
        }
    }

    @DeleteMapping("/product/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable int id) {
        if (id <= 0) return ResponseEntity.badRequest().build();

        if (productService.getProductById(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> searchProducts(
        @RequestParam("keyword") String keyword
    ) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(
        @RequestParam("image") MultipartFile image
    ) {
        try {
            String url = imageHandleService.uploadFile(image);
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                "Upload failed"
            );
        }
    }
}
