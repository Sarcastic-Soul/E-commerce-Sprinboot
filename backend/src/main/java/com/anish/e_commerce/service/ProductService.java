package com.anish.e_commerce.service;

import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.repo.ProductRepo;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ImageHandleService imageHandleService;

    @Autowired
    private NotificationService notificationService;

    @Cacheable(value = "products", key = "'all'")
    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    @Cacheable(value = "product", key = "#id")
    public Product getProductById(int id) {
        return productRepo
            .findById(id)
            .orElseThrow(() ->
                new RuntimeException("Product not found with id: " + id)
            );
    }

    @Cacheable(
        value = "products",
        key = "{#keyword, #category, #minPrice, #maxPrice, #available, #sortDir}"
    )
    public List<Product> getFilteredProducts(
        String keyword,
        Product.ProductCategory category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean available,
        String sortDir
    ) {
        Sort sort = "desc".equalsIgnoreCase(sortDir)
            ? Sort.by("price").descending()
            : Sort.by("price").ascending();

        return productRepo.filterProducts(
            keyword,
            category,
            minPrice,
            maxPrice,
            available,
            sort
        );
    }

    private void enforceStockRules(Product product) {
        // Rule 1: No negative inventory
        if (product.getQuantity() < 0) {
            throw new IllegalArgumentException(
                "Product quantity cannot be negative."
            );
        }

        // Rule 2: If manually marked unavailable, wipe the stock
        if (!product.isAvailable()) {
            product.setQuantity(0);
        }

        // Rule 3: If stock is 0, it CANNOT be available
        if (product.getQuantity() == 0) {
            product.setAvailable(false);
        }
    }

    @CacheEvict(value = "products", allEntries = true)
    public Product addProduct(Product product) throws IOException {
        enforceStockRules(product);
        return productRepo.save(product);
    }

    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#updatedProduct.id"),
        }
    )
    public Product updateProduct(Product updatedProduct) throws IOException {
        Product existing = productRepo
            .findById(updatedProduct.getId())
            .orElseThrow(() ->
                new RuntimeException(
                    "Product not found with id: " + updatedProduct.getId()
                )
            );

        boolean isRestocked =
            existing.getQuantity() == 0 && updatedProduct.getQuantity() > 0;

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setBrand(updatedProduct.getBrand());
        existing.setPrice(updatedProduct.getPrice());
        existing.setCategory(updatedProduct.getCategory());
        existing.setAvailable(updatedProduct.isAvailable());
        existing.setQuantity(updatedProduct.getQuantity());
        enforceStockRules(existing);

        if (isRestocked && updatedProduct.isAvailable()) {
            notificationService.notifyUsersOnRestock(existing);
        }
        if (!updatedProduct.isAvailable()) {
            existing.setQuantity(0);
        }

        if (
            existing.getImageUrl() != null &&
            !Objects.equals(
                existing.getImageUrl(),
                updatedProduct.getImageUrl()
            )
        ) {
            imageHandleService.deleteImageByUrl(existing.getImageUrl());
        }
        if (updatedProduct.getImageUrl() != null) {
            existing.setImageUrl(updatedProduct.getImageUrl());
        }
        return productRepo.save(existing);
    }

    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#id"),
        }
    )
    public void deleteProduct(int id) {
        Product existing = productRepo
            .findById(id)
            .orElseThrow(() ->
                new RuntimeException("Product not found with id: " + id)
            );
        if (existing.getImageUrl() != null) {
            imageHandleService.deleteImageByUrl(existing.getImageUrl());
        }
        productRepo.deleteById(id);
    }

    public List<Product> searchProducts(String keyword) {
        return productRepo.searchProducts(keyword);
    }
}
