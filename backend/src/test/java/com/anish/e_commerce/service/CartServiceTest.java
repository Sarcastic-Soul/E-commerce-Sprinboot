package com.anish.e_commerce.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.anish.e_commerce.dto.CartItemResponse;
import com.anish.e_commerce.model.Cart;
import com.anish.e_commerce.model.CartItem;
import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.CartItemRepo;
import com.anish.e_commerce.repo.CartRepo;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepo cartRepo;

    @Mock
    private CartItemRepo cartItemRepo;

    @Mock
    private ProductRepo productRepo;

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");

        testProduct = new Product();
        testProduct.setId(1);
        testProduct.setName("Test Duck");
        testProduct.setPrice(new BigDecimal("19.99"));

        testCart = new Cart();
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>());
    }

    @Test
    void testGetCartByUsername_CreatesNewCartIfNoneExists() {
        when(cartRepo.findByUser_Username("testuser")).thenReturn(
            Optional.empty()
        );
        when(userRepo.findByUsername("testuser")).thenReturn(
            Optional.of(testUser)
        );
        when(cartRepo.save(any(Cart.class))).thenAnswer(i ->
            i.getArguments()[0]
        );

        Cart cart = cartService.getCartByUsername("testuser");

        assertNotNull(cart);
        assertEquals(testUser, cart.getUser());
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepo, times(1)).save(any(Cart.class));
    }

    @Test
    void testAddOrUpdateItem_AddNewItemToCart() {
        // Arrange
        when(productRepo.findById(1)).thenReturn(Optional.of(testProduct));
        when(cartRepo.findByUser_Username("testuser")).thenReturn(
            Optional.of(testCart)
        );

        CartItemResponse mockResponse = new CartItemResponse(
            1,
            "Test Duck",
            new BigDecimal("19.99"),
            "url",
            2
        );
        when(cartItemRepo.findItemsByUsername("testuser")).thenReturn(
            List.of(mockResponse)
        );

        // Act
        List<CartItemResponse> result = cartService.addOrUpdateItem(
            "testuser",
            1,
            2
        );

        // Assert
        assertEquals(1, testCart.getItems().size()); // Cart should now have 1 item
        assertEquals(2, testCart.getItems().get(0).getQuantity()); // Quantity should be 2
        assertEquals(1, result.size());
        verify(cartRepo, times(1)).save(testCart);
    }

    @Test
    void testAddOrUpdateItem_UpdatesExistingItemQuantity() {
        // Arrange
        CartItem existingItem = new CartItem();
        existingItem.setProduct(testProduct);
        existingItem.setQuantity(1);
        existingItem.setCart(testCart);
        testCart.getItems().add(existingItem); // Cart already has 1 duck

        when(productRepo.findById(1)).thenReturn(Optional.of(testProduct));
        when(cartRepo.findByUser_Username("testuser")).thenReturn(
            Optional.of(testCart)
        );

        // Act
        cartService.addOrUpdateItem("testuser", 1, 5); // User changes quantity to 5

        // Assert
        assertEquals(1, testCart.getItems().size()); // Should still only have 1 item type
        assertEquals(5, testCart.getItems().get(0).getQuantity()); // But quantity should update to 5
        verify(cartRepo, times(1)).save(testCart);
    }

    @Test
    void testRemoveItem_RemovesProductFromCart() {
        // Arrange
        CartItem existingItem = new CartItem();
        existingItem.setProduct(testProduct);
        existingItem.setCart(testCart);
        testCart.getItems().add(existingItem);

        when(cartRepo.findByUser_Username("testuser")).thenReturn(
            Optional.of(testCart)
        );

        // Act
        cartService.removeItem("testuser", 1);

        // Assert
        assertTrue(testCart.getItems().isEmpty()); // Cart should be empty now
        verify(cartRepo, times(1)).save(testCart);
    }

    @Test
    void testClearCart_EmptiesAllItems() {
        // Arrange
        CartItem existingItem = new CartItem();
        existingItem.setProduct(testProduct);
        testCart.getItems().add(existingItem);

        when(cartRepo.findByUser_Username("testuser")).thenReturn(
            Optional.of(testCart)
        );

        // Act
        cartService.clearCart("testuser");

        // Assert
        assertTrue(testCart.getItems().isEmpty());
        verify(cartRepo, times(1)).save(testCart);
    }
}
