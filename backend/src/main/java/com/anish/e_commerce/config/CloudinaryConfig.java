package com.anish.e_commerce.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
        @Value("${cloudinary.cloud-name:}") String cloudName,
        @Value("${cloudinary.api-key:}") String apiKey,
        @Value("${cloudinary.api-secret:}") String apiSecret
    ) {
        if (
            cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()
        ) {
            log.warn(
                "⚠️ Cloudinary credentials are not configured. Set CLOUDINARY_CLOUD_NAME, " +
                    "CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET — image upload/delete will fail without them."
            );
        }

        return new Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
            )
        );
    }
}
