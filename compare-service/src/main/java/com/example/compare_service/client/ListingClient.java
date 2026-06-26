package com.example.compare_service.client;

import com.example.compare_service.dto.ProductListingDTO;
import com.example.compare_service.dto.CustomPageImpl;
import com.example.compare_service.dto.ProductDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "listing-service", url = "${product.service.url}")
public interface ListingClient {

    // ProductListingController chấp nhận type, sortBy, page, size
    @GetMapping("/api/listings")
    CustomPageImpl<ProductListingDTO> getAllListings(@RequestParam(required = false) String type,
                                           @RequestParam(defaultValue = "date") String sortBy,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size);
    // lấy listing theo id
    @GetMapping("/api/listings/{id}")
    ProductListingDTO getListingById(@PathVariable("id") Long id);

    // lấy chi tiết product (product-details endpoint)
    @GetMapping("/api/product-details/{productId}")
    ProductDetailDTO getProductDetail(@PathVariable("productId") Long productId);
}
