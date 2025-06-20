package com.anish.e_commerce.service;

import java.util.List;
import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.repo.ProductRepo;

@Service
public class ProductService {

    @Autowired
    private ProductRepo productRepo;

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public Product getProductById(int id) {
        return productRepo.findById(id).orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    public Product addProduct(Product product, MultipartFile image) throws Exception {
        product.setImageName(image.getOriginalFilename());
        product.setImageType(image.getContentType());
        product.setCreatedAt(new Date()); // Set current date and time

        try {
            product.setImageData(image.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to store image data: " + e.getMessage());
        }

        return productRepo.save(product);
    }

    public Product updateProduct(int id, Product updatedProduct, MultipartFile image) {
        Product existingProduct = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Update product fields
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setBrand(updatedProduct.getBrand());
        existingProduct.setPrice(updatedProduct.getPrice());
        existingProduct.setCategory(updatedProduct.getCategory());
//        existingProduct.setAvailable(updatedProduct.getAvailable());
        existingProduct.setQuantity(updatedProduct.getQuantity());

        // Update image if provided
        if (image != null && !image.isEmpty()) {
            existingProduct.setImageName(image.getOriginalFilename());
            existingProduct.setImageType(image.getContentType());
            try {
                existingProduct.setImageData(image.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Failed to update image data: " + e.getMessage());
            }
        }

        // Preserve original creation date
        existingProduct.setCreatedAt(existingProduct.getCreatedAt());

        return productRepo.save(existingProduct);
    }

    public void deleteProduct(int id) {
        productRepo.deleteById(id);
    }

    public List<Product> searchProducts(String Keyword) {
        return productRepo.searchProducts(Keyword);
    }

}
