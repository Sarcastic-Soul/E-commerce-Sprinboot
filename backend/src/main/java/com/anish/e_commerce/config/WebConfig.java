package com.anish.e_commerce.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(
                new PathResourceResolver() {
                    @Override
                    protected Resource getResource(
                        String resourcePath,
                        Resource location
                    ) throws IOException {
                        Resource requestedResource = location.createRelative(
                            resourcePath
                        );

                        // 1. If the file exists, serve it.
                        if (
                            requestedResource.exists() &&
                            requestedResource.isReadable()
                        ) {
                            return requestedResource;
                        }

                        // 2. IMPORTANT: Do NOT route API or Swagger requests to the React index.html
                        if (
                            resourcePath.startsWith("api/") ||
                            resourcePath.startsWith("swagger-ui/") ||
                            resourcePath.startsWith("v3/api-docs")
                        ) {
                            return null;
                        }

                        // 3. Otherwise, forward everything else to index.html for React Router!
                        return new ClassPathResource("/static/index.html");
                    }
                }
            );
    }
}
