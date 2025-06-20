package com.anish.e_commerce.config;

import com.anish.e_commerce.model.Product;
import com.anish.e_commerce.repo.ProductRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

@Configuration
@RequiredArgsConstructor
public class DevSeeder implements CommandLineRunner {

    private final ProductRepo productRepo;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (productRepo.count() > 0) return;

        Product p1 = new Product(0, "CyberDuck Backpack", "Tech-friendly waterproof backpack.",
                "NeoGear", new BigDecimal("69.99"), "Accessories", new Date(),
                true, 10, "backpack.jpg", "image/jpeg", readImage("static/backpack.jpg"));

        Product p2 = new Product(0, "Duckboard Keyboard", "Custom RGB mechanical keyboard with ducky keys.",
                "Quacktronics", new BigDecimal("149.99"), "Electronics", new Date(),
                true, 8, "keyboard.jpg", "image/jpeg", readImage("static/keyboard.jpg"));

        Product p3 = new Product(0, "Duck Plushie", "Cute and terrifying. Comes with fabric knife.",
                "ChaosToys", new BigDecimal("29.99"), "Toys", new Date(),
                true, 20, "plushie.jpg", "image/jpeg", readImage("static/plushie.jpg"));

        Product p4 = new Product(0, "DuckOS T-Shirt", "Minimalist hacker duck tee in black & neon green.",
                "NullWear", new BigDecimal("24.99"), "Clothing", new Date(),
                true, 50, "tshirt.jpg", "image/jpeg", readImage("static/tshirt.jpg"));

        Product p5 = new Product(0, "Quackbuds Earphones", "Wireless earbuds with noise-cancellation & duck sounds.",
                "FeatherTech", new BigDecimal("89.99"), "Audio", new Date(),
                true, 15, "earphones.jpg", "image/jpeg", readImage("static/earphones.jpg"));

        productRepo.save(p1);
        productRepo.save(p2);
        productRepo.save(p3);
        productRepo.save(p4);
        productRepo.save(p5);

        System.out.println("✅ DevSeeder inserted 5 products");
    }

    private byte[] readImage(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            System.out.println("⚠️ Could not load image from " + classpathLocation + " - using empty byte array.");
            return new byte[0];
        }
    }
}
