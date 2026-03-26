package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.repo.ProductRepo;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepo productRepo; // We mock the database so tests run instantly

    @Mock
    private ImageHandleService imageHandleService;

    @InjectMocks
    private ProductService productService; // Injects the mocked repo into the service

    private Product dummyProduct;

    @BeforeEach
    void setUp() {
        dummyProduct = new Product(
            1,
            "Test Duck",
            "A cool duck",
            "DuckCorp",
            new BigDecimal("19.99"),
            Product.ProductCategory.TOYS_GAMES,
            new Date(),
            true,
            10,
            "url"
        );
    }

    @Test
    void testGetAllProducts() {
        // Arrange: Tell the mock database what to return
        when(productRepo.findAll()).thenReturn(List.of(dummyProduct));

        // Act: Call the actual service method
        List<Product> products = productService.getAllProducts();

        // Assert: Verify the results
        assertEquals(1, products.size());
        assertEquals("Test Duck", products.get(0).getName());
        verify(productRepo, times(1)).findAll(); // Verify the repo was called exactly once
    }

    @Test
    void testGetProductById_ThrowsExceptionWhenNotFound() {
        // Arrange: Tell the mock database to return empty for ID 99
        when(productRepo.findById(99)).thenReturn(Optional.empty());

        // Act & Assert: Verify that asking for ID 99 throws a RuntimeException
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> {
                productService.getProductById(99);
            }
        );

        assertEquals("Product not found with id: 99", exception.getMessage());
    }

    @Test
    void testUpdateProduct_WhenSetToUnavailable_QuantityIsZeroed()
        throws IOException {
        // Arrange
        // 1. The existing product in the DB has quantity 10 and is available
        when(productRepo.findById(1)).thenReturn(Optional.of(dummyProduct));

        // 2. The new incoming request sets available to FALSE
        Product updateRequest = new Product(
            1,
            "Test Duck",
            "A cool duck",
            "DuckCorp",
            new BigDecimal("19.99"),
            Product.ProductCategory.TOYS_GAMES,
            new Date(),
            false,
            50,
            "url" // User maliciously tries to set quantity to 50 while unavailable
        );

        // 3. Mock the save method to just return whatever is passed to it
        when(productRepo.save(any(Product.class))).thenAnswer(i ->
            i.getArguments()[0]
        );

        // Act
        Product result = productService.updateProduct(updateRequest);

        // Assert: The business logic should force the quantity to 0
        assertFalse(result.isAvailable());
        assertEquals(
            0,
            result.getQuantity(),
            "Quantity must be 0 if product is unavailable"
        );
        verify(productRepo, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdateProduct_WhenImageUrlChanges_OldImageIsDeleted()
        throws IOException {
        // Arrange
        String oldImageUrl = "http://old-image.com/duck.jpg";
        dummyProduct.setImageUrl(oldImageUrl);
        when(productRepo.findById(1)).thenReturn(Optional.of(dummyProduct));

        // The update request comes in with a completely NEW image URL
        Product updateRequest = new Product(
            1,
            "Test Duck",
            "A cool duck",
            "DuckCorp",
            new BigDecimal("19.99"),
            Product.ProductCategory.TOYS_GAMES,
            new Date(),
            true,
            10,
            "http://new-image.com/duck.jpg"
        );

        when(productRepo.save(any(Product.class))).thenAnswer(i ->
            i.getArguments()[0]
        );

        // Act
        productService.updateProduct(updateRequest);

        // Assert: Verify the ImageHandleService was told to delete the OLD image
        verify(imageHandleService, times(1)).deleteImageByUrl(oldImageUrl);
        verify(productRepo, times(1)).save(any(Product.class));
    }

    @Test
    void testDeleteProduct_WithImage_DeletesImageAndProduct() {
        // Arrange
        String imageUrl = "http://image-to-delete.com/duck.jpg";
        dummyProduct.setImageUrl(imageUrl);
        when(productRepo.findById(1)).thenReturn(Optional.of(dummyProduct));

        // Act
        productService.deleteProduct(1);

        // Assert: Ensure both the image service and the DB delete methods were triggered
        verify(imageHandleService, times(1)).deleteImageByUrl(imageUrl);
        verify(productRepo, times(1)).deleteById(1);
    }
}
