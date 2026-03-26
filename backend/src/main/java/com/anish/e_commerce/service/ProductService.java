package com.anish.e_commerce.service;

import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.repo.ProductRepo;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ImageHandleService imageHandleService;

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public Product getProductById(int id) {
        return productRepo
            .findById(id)
            .orElseThrow(() ->
                new RuntimeException("Product not found with id: " + id)
            );
    }

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

    public Product addProduct(Product product) throws IOException {
        return productRepo.save(product);
    }

    public Product updateProduct(Product updatedProduct) throws IOException {
        Product existing = productRepo
            .findById(updatedProduct.getId())
            .orElseThrow(() ->
                new RuntimeException(
                    "Product not found with id: " + updatedProduct.getId()
                )
            );

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setBrand(updatedProduct.getBrand());
        existing.setPrice(updatedProduct.getPrice());
        existing.setCategory(updatedProduct.getCategory());
        existing.setAvailable(updatedProduct.isAvailable());
        existing.setQuantity(updatedProduct.getQuantity());

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
