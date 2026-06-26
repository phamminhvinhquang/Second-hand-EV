package edu.uth.listingservice.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(MvcConfig.class);

    // Lấy đường dẫn từ cấu hình. Mặc định là "uploads" nếu không có biến môi trường
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Chuyển chuỗi đường dẫn thành Path tuyệt đối
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        // Chuyển sang định dạng URI (file:/...)
        String resourceLocation = path.toUri().toString();

        // QUAN TRỌNG: Spring Boot bắt buộc đường dẫn thư mục phải kết thúc bằng dấu "/"
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        logger.info("================== MVC CONFIG ==================");
        logger.info("Serving static content from: " + resourceLocation);
        logger.info("================================================");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}