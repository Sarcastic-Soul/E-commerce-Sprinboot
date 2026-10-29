package com.anish.e_commerce.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

// Kept off the main application class so @WebMvcTest slices, which do not
// auto-configure a CacheManager, don't pick up @EnableCaching and fail to start.
@Configuration
@EnableCaching
public class CacheConfig {}
