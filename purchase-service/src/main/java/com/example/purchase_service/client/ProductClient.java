package com.example.purchase_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.purchase_service.dto.ProductDetailDTO;

@FeignClient(name = "listing-service", url = "${product.service.url}")
public interface ProductClient {
    @GetMapping("/api/product-details/{productId}")
    ProductDetailDTO getProductDetail(@PathVariable("productId") Long productId);
}
