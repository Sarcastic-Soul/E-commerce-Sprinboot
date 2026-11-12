package com.anish.e_commerce.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Off the main class for the same reason as CacheConfig: @WebMvcTest slices
// should not start the order-expiry job. Serverless hosts set
// app.scheduling.enabled=false: instances freeze between requests, so a timer
// would fire at random or never. OrderExpiryJob has on-demand triggers instead.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {}
