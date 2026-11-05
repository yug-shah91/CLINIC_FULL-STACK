package com.clinic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Lets Spring Boot serve the React app (built into src/main/resources/static).
 *
 * React Router creates URLs like /admin/patients/3 inside the browser. Those are not real
 * files, so if the user refreshes the page Spring would answer 404. This resolver fixes that:
 *   1. a real file exists (index.html, assets/app.js ...)  -> serve that file
 *   2. the path starts with api/                           -> return nothing, so API errors stay API errors
 *   3. anything else                                       -> serve index.html and let React Router decide
 *
 * If the frontend has not been built yet, index.html does not exist and everything non-API is a 404.
 * That is fine during development, when the frontend runs on its own Vite server.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(false) // no caching, so a rebuilt frontend shows up immediately
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
