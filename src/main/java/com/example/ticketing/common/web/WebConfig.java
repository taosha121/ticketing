package com.example.ticketing.common.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.local-dir}")
    private String localDir;

    @Value("${app.storage.base-url}")
    private String baseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteDir = Path.of(localDir).toAbsolutePath().toString();
        registry.addResourceHandler(baseUrl + "/**")
                .addResourceLocations("file:" + absoluteDir + "/");
    }
}
