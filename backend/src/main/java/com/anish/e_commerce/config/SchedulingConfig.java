package com.anish.e_commerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Off the main class for the same reason as CacheConfig: @WebMvcTest slices
// should not start the order-expiry job.
@Configuration
@EnableScheduling
public class SchedulingConfig {}
