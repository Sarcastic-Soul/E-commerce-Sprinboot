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

        // 1
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

        // 2
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

        // 3
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

        // 4
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

        // 5
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

        // 6
        productRepo.save(
            new Product(
                0,
                "Neon Hoodie",
                "Heavyweight glow-in-the-dark developer hoodie.",
                "NullWear",
                new BigDecimal("45.99"),
                ProductCategory.FASHION,
                new Date(),
                true,
                30,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491562/tshirt_jngcar.jpg"
            )
        );

        // 7
        productRepo.save(
            new Product(
                0,
                "QuackPad Mouse",
                "Ergonomic gaming mouse with ultra-fast switches.",
                "Quacktronics",
                new BigDecimal("59.99"),
                ProductCategory.ELECTRONICS,
                new Date(),
                true,
                25,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491563/keyboard_fk5yo0.jpg"
            )
        );

        // 8
        productRepo.save(
            new Product(
                0,
                "CyberMug",
                "Self-heating smart coffee mug for long coding nights.",
                "NeoGear",
                new BigDecimal("35.00"),
                ProductCategory.HOME_KITCHEN,
                new Date(),
                true,
                40,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491563/backpack_vcbd8g.jpg"
            )
        );

        // 9
        productRepo.save(
            new Product(
                0,
                "Classic Debugger",
                "The legendary yellow rubber debugging duck.",
                "ChaosToys",
                new BigDecimal("9.99"),
                ProductCategory.TOYS_GAMES,
                new Date(),
                true,
                100,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491560/plushie_pkjwfl.jpg"
            )
        );

        // 10
        productRepo.save(
            new Product(
                0,
                "Hacker Shades",
                "Polarized anti-blue light glasses for screen protection.",
                "NullWear",
                new BigDecimal("22.50"),
                ProductCategory.FASHION,
                new Date(),
                true,
                50,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491838/earphones_lkhors.jpg"
            )
        );

        // 11
        productRepo.save(
            new Product(
                0,
                "Tactile Switches Pack",
                "Box of 90 custom tactile switches for your mech.",
                "Quacktronics",
                new BigDecimal("39.99"),
                ProductCategory.ELECTRONICS,
                new Date(),
                true,
                200,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491563/keyboard_fk5yo0.jpg"
            )
        );

        // 12
        productRepo.save(
            new Product(
                0,
                "Smart Desk Lamp",
                "App-controlled RGB desk lamp to set the mood.",
                "NeoGear",
                new BigDecimal("49.99"),
                ProductCategory.HOME_KITCHEN,
                new Date(),
                true,
                15,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491838/earphones_lkhors.jpg"
            )
        );

        // 13
        productRepo.save(
            new Product(
                0,
                "Debug Duck XL",
                "Giant 2-foot debugging duck for monumental bugs.",
                "ChaosToys",
                new BigDecimal("79.99"),
                ProductCategory.TOYS_GAMES,
                new Date(),
                true,
                5,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491560/plushie_pkjwfl.jpg"
            )
        );

        // 14
        productRepo.save(
            new Product(
                0,
                "Tech Cargo Pants",
                "Durable cargo pants with extreme pocket capacity.",
                "NullWear",
                new BigDecimal("55.00"),
                ProductCategory.FASHION,
                new Date(),
                true,
                45,
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491562/tshirt_jngcar.jpg"
            )
        );

        // 15 - OUT OF STOCK EXAMPLE
        productRepo.save(
            new Product(
                0,
                "Wireless Charger",
                "Fast charging pad for all your devices. Currently backordered.",
                "Quacktronics",
                new BigDecimal("29.99"),
                ProductCategory.ELECTRONICS,
                new Date(),
                false, // Set to false to test out-of-stock logic!
                0, // 0 quantity
                "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491563/backpack_vcbd8g.jpg"
            )
        );

        System.out.println("✅ DevSeeder inserted 15 products");
    }
}
