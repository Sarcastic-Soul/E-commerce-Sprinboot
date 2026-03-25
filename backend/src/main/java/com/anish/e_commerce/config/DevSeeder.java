package com.anish.e_commerce.config;

import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.model.Product.ProductCategory;
import com.anish.e_commerce.model.User;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.repo.UserRepo;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DevSeeder implements CommandLineRunner {

    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedProducts();
    }

    private void seedUsers() {
        // Only run if the users table is empty
        if (userRepo.count() > 0) return;

        // 1. Create Admin User
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRoles(Set.of("ADMIN"));
        userRepo.save(admin);

        // 2. Create Regular User
        User regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setPassword(passwordEncoder.encode("user123"));
        regularUser.setRoles(Set.of("USER"));
        userRepo.save(regularUser);

        System.out.println("✅ DevSeeder inserted 'admin' and 'user' accounts");
    }

    private void seedProducts() {
        if (productRepo.count() > 0) return;

        productRepo.save(
            new Product(
                0,
                "CyberBackpack",
                "Tech-friendly waterproof backpack.",
                "NeoGear",
                new BigDecimal("69.99"),
                ProductCategory.FASHION,
                new Date(),
                true,
                10,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491563/backpack_vcbd8g.jpg"
            )
        );

        productRepo.save(
            new Product(
                0,
                "Duckboard Keyboard",
                "Custom RGB mechanical keyboard with ducky keys.",
                "Quacktronics",
                new BigDecimal("149.99"),
                ProductCategory.ELECTRONICS,
                new Date(),
                true,
                8,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491563/keyboard_fk5yo0.jpg"
            )
        );

        productRepo.save(
            new Product(
                0,
                "Duck Plushie",
                "Cute and terrifying. Comes with fabric knife.",
                "ChaosToys",
                new BigDecimal("29.99"),
                ProductCategory.TOYS_GAMES,
                new Date(),
                true,
                20,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491560/plushie_pkjwfl.jpg"
            )
        );

        productRepo.save(
            new Product(
                0,
                "T-Shirt",
                "Minimalist hacker duck tee in black & neon green.",
                "NullWear",
                new BigDecimal("24.99"),
                ProductCategory.FASHION,
                new Date(),
                true,
                50,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491562/tshirt_jngcar.jpg"
            )
        );

        productRepo.save(
            new Product(
                0,
                "Earphones",
                "Wireless earbuds with noise-cancellation & duck sounds.",
                "FeatherTech",
                new BigDecimal("89.99"),
                ProductCategory.ELECTRONICS,
                new Date(),
                true,
                15,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491838/earphones_lkhors.jpg"
            )
        );

        System.out.println("✅ DevSeeder inserted 5 products");
    }
}
