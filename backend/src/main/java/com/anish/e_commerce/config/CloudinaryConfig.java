package com.anish.e_commerce.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dhagorcpe",
                "api_key", "464593384979863",
                "api_secret", "asB-HWcBx-xGKdb-T9AG8fNy2iE"
        ));
    }
}
