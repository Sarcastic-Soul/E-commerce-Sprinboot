package com.anish.e_commerce.controller;

import com.anish.e_commerce.dto.ProductDTO;
import com.anish.e_commerce.exception.ResourceNotFoundException;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.service.ImageHandleService;
import com.anish.e_commerce.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;
    private final ImageHandleService imageHandleService;

    @GetMapping("/products")
    public ResponseEntity<Page<Product>> getAllProducts(
        @RequestParam(defaultValue = "0") @Min(0) int page, // Default to page 0 (Spring is 0-indexed)
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size, // Default to 12 items per page, capped so a caller cannot force a full-table load
        @RequestParam(defaultValue = "newest") String sort
    ) {
        Page<Product> productPage = productService.getAllProducts(
            page,
            size,
            ProductService.canonicalSort(sort)
        );
        return new ResponseEntity<>(productPage, HttpStatus.OK);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> getProductById(
        @PathVariable int id,
        Authentication authentication
    ) {
        if (id <= 0) return ResponseEntity.badRequest().build();

        // Missing ids surface as ResourceNotFoundException -> 404
        Product product = productService.getProductById(id);
        // Soft-deleted products are invisible to everyone but admins.
        if (!product.isActive() && !OrderController.isAdmin(authentication)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        return ResponseEntity.ok(product);
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
                ProductService.canonicalSort(sort)
            )
        );
    }

    @PostMapping("/product")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> addProduct(
        @Valid @RequestBody ProductDTO productDto
    ) throws IOException {
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
        product.setImages(ProductService.cleanImages(productDto.getImages()));
        product.setCreatedAt(new Date()); // Set creation date

        return ResponseEntity.status(HttpStatus.CREATED).body(
            productService.addProduct(product)
        );
    }

    @PutMapping("/product/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updateProduct(
        @PathVariable int id,
        @Valid @RequestBody ProductDTO productDto
    ) throws IOException {
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
        // null (not sent) keeps the current gallery; see ProductService.updateProduct.
        product.setImages(productDto.getImages());

        return ResponseEntity.ok(productService.updateProduct(product));
    }

    @DeleteMapping("/product/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable int id) {
        if (id <= 0) return ResponseEntity.badRequest().build();

        // Missing ids surface as ResourceNotFoundException -> 404
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> searchProducts(
        @RequestParam("keyword") String keyword
    ) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    // Only the admin product forms upload images; nobody else needs Cloudinary quota.
    @PostMapping("/upload-image")
    @PreAuthorize("hasRole('ADMIN')")
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
