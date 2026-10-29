package com.anish.e_commerce.service;

import com.anish.e_commerce.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    // Neither /products/filter nor /products/search paginates on the frontend (a
    // real pagination UI for the filtered view is a separate, deliberately deferred
    // change) — this cap just stops either from becoming an unbounded full-table
    // scan as the catalog grows, without changing the response shape.
    private static final int MAX_UNPAGINATED_RESULTS = 200;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ImageHandleService imageHandleService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Collapses any client value to one of the known sort keys before it reaches a
     * cache key, so junk values cannot mint unlimited cache entries.
     */
    public static String canonicalSort(String sort) {
        String key = sort == null ? "" : sort.trim().toLowerCase();
        return switch (key) {
            case "price_asc", "asc" -> "price_asc";
            case "price_desc", "desc" -> "price_desc";
            case "rating", "name" -> key;
            default -> "newest";
        };
    }

    /**
     * Maps the contract's sort values to a Sort. Unknown values fall back to newest;
     * the legacy "asc"/"desc" from the filter endpoint still mean price.
     */
    static Sort toSort(String sort) {
        return switch (canonicalSort(sort)) {
            case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            case "rating" -> Sort.by(
                Sort.Order.desc("averageRating"),
                Sort.Order.desc("reviewCount"),
                Sort.Order.asc("id")
            );
            case "name" -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        };
    }

    // Sort is part of the key, otherwise one ordering would be served for all.
    @Cacheable(
        value = "products",
        key = "'page-' + #page + '-size-' + #size + '-sort-' + #sort"
    )
    public Page<Product> getAllProducts(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, toSort(sort));
        return productRepo.findByActiveTrue(pageable);
    }

    /**
     * Returns inactive (soft-deleted) products too; the controller hides those from
     * everyone but admins. Cached as-is so both audiences share one entry.
     */
    @Cacheable(value = "product", key = "#id")
    public Product getProductById(int id) {
        return productRepo
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Product not found with id: " + id)
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
        Pageable pageable = PageRequest.of(
            0,
            MAX_UNPAGINATED_RESULTS,
            toSort(sortDir)
        );

        return productRepo.filterProducts(
            keyword,
            category,
            minPrice,
            maxPrice,
            available,
            pageable
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

    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#result.id"),
        }
    )
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
                new ResourceNotFoundException(
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
        // null = the client did not send a gallery; leave it alone. Mutated in place
        // so Hibernate keeps tracking the same collection instance.
        if (updatedProduct.getImages() != null) {
            List<String> images = cleanImages(updatedProduct.getImages());
            existing.getImages().clear();
            existing.getImages().addAll(images);
        }
        return productRepo.save(existing);
    }

    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product", key = "#id"),
        }
    )
    /**
     * Soft delete. Order history still references the row (and shows its image),
     * so the product is only hidden from the catalog, never removed.
     */
    public void deleteProduct(int id) {
        Product existing = productRepo
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Product not found with id: " + id)
            );
        existing.setActive(false);
        productRepo.save(existing);
    }

    /** Drops blanks and duplicates, keeps order. */
    public static List<String> cleanImages(List<String> images) {
        if (images == null) return new java.util.ArrayList<>();
        return images
            .stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    public List<Product> searchProducts(String keyword) {
        return productRepo.searchProducts(
            keyword,
            PageRequest.of(0, MAX_UNPAGINATED_RESULTS)
        );
    }
}
