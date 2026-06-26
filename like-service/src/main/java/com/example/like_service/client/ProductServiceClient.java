package com.example.like_service.client;


import com.example.like_service.dto.ProductDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Nếu bạn có Eureka, dùng name = "product-service" và không cần url.
// Nếu không dùng Eureka, set url tới product-service (ví dụ http://localhost:8080)



//ProductServiceClient là Feign client để gọi product-service
//Gọi HTTP như gọi phương thức Java
//Trả về ProductDetailDTO (JSON -> DTO)
@FeignClient(name = "product-service", url = "${product.service.url:http://localhost:8080}")
public interface ProductServiceClient {

    // gọi API ProductDetailController: /api/product-details/{productId}
    @GetMapping("/api/product-details/{productId}")
    ProductDetailDTO getProductDetail(@PathVariable("productId") Long productId);

    // Nếu API product bên kia khác đường dẫn, chỉ cần chỉnh @GetMapping tương ứng
}
