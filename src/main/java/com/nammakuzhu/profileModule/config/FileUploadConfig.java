package com.nammakuzhu.profileModule.config;

import java.io.File;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadRoot = System.getProperty("user.home") + File.separator + "nammakuzhu_uploads" + File.separator;

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadRoot);
    }
}
