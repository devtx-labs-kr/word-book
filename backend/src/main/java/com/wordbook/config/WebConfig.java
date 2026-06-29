package com.wordbook.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the bundled React build from {@code classpath:/static/} and falls back to {@code
 * index.html} for client-side (SPA) routes, while leaving {@code /api/**} and {@code /actuator/**}
 * to their controllers (deployment-architecture §2).
 */
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
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                Resource requested = location.createRelative(resourcePath);
                if (requested.exists() && requested.isReadable()) {
                  return requested;
                }
                // Do not hijack API or actuator paths — let them 404 normally.
                if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
                  return null;
                }
                // SPA fallback: serve index.html for client-side routes.
                return new ClassPathResource("/static/index.html");
              }
            });
  }
}
