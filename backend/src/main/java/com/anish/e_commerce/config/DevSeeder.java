package com.anish.e_commerce.config;

import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.repo.ProductRepo;
import com.anish.e_commerce.model.Product.ProductCategory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Date;

@Configuration
@RequiredArgsConstructor
public class DevSeeder implements CommandLineRunner {

    private final ProductRepo productRepo;

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepo.count() > 0) return;

        productRepo.save(new Product(0, "CyberBackpack", "Tech-friendly waterproof backpack.",
                "NeoGear", new BigDecimal("69.99"), ProductCategory.FASHION, new Date(),
                true, 10, "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491563/backpack_vcbd8g.jpg"));

        productRepo.save(new Product(0, "Duckboard Keyboard", "Custom RGB mechanical keyboard with ducky keys.",
                "Quacktronics", new BigDecimal("149.99"), ProductCategory.ELECTRONICS, new Date(),
                true, 8, "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491563/keyboard_fk5yo0.jpg"));

        productRepo.save(new Product(0, "Duck Plushie", "Cute and terrifying. Comes with fabric knife.",
                "ChaosToys", new BigDecimal("29.99"), ProductCategory.TOYS_GAMES, new Date(),
                true, 20, "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491560/plushie_pkjwfl.jpg"));

        productRepo.save(new Product(0, "T-Shirt", "Minimalist hacker duck tee in black & neon green.",
                "NullWear", new BigDecimal("24.99"), ProductCategory.FASHION, new Date(),
                true, 50, "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491562/tshirt_jngcar.jpg"));

        productRepo.save(new Product(0, "Earphones", "Wireless earbuds with noise-cancellation & duck sounds.",
                "FeatherTech", new BigDecimal("89.99"), ProductCategory.ELECTRONICS, new Date(),
                true, 15, "https://res.cloudinary.com/dhagorcpe/image/upload/v1752491838/earphones_lkhors.jpg"));

        System.out.println("✅ DevSeeder inserted 5 products");
    }
}