package local.contract.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mọi URL /contracts/** sẽ map vào thư mục /app/contracts trong container
        registry.addResourceHandler("/contracts/**")
                .addResourceLocations("file:/app/contracts/");
    }
}
