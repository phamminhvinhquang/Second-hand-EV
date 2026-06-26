package com.example.search_service.client;

import com.example.search_service.dto.ProductListingDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.search_service.dto.CustomPageImpl; // Phải dùng lớp này

@FeignClient(name = "listing-service", url = "${product.service.url}")
public interface ListingClient {

    // SỬA: Đổi kiểu trả về thành CustomPageImpl
    @GetMapping("/api/listings")
    CustomPageImpl<ProductListingDTO> getFilteredListings(
                                           @RequestParam(required = false) String type,
                                           @RequestParam(defaultValue = "date") String sortBy,
                                           @RequestParam(defaultValue = "0") int page, 
                                           @RequestParam(defaultValue = "100") int size); 
}