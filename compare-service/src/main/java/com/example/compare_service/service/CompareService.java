package com.example.compare_service.service;

import com.example.compare_service.client.ListingClient;
import com.example.compare_service.dto.ProductListingDTO;
import com.example.compare_service.dto.CustomPageImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.example.compare_service.config.CacheConfig;

import java.util.Collections;
import java.util.List;

@Service
public class CompareService {

    @Autowired
    private ListingClient listingClient;

    /**
     * Trả về danh sách listing đã lọc (server-side + fallback client-side if needed).
     * Cached with Redis using cache name defined in CacheConfig.
     */
    @Cacheable(value = CacheConfig.COMPARE_CACHE, 
               key = "#root.methodName + ':' + ( #type == null ? '' : #type ) + ':' + #limit")
    public List<ProductListingDTO> getListings(String type, int limit) {
        try {
            CustomPageImpl<ProductListingDTO> pageResult = listingClient.getAllListings(
                    null, // we rely on compare-service to filter
                    "date",
                    0,
                    limit
            );

            List<ProductListingDTO> all = pageResult.getContent();
            if (all == null) return Collections.emptyList();
            return all;
        } catch (Exception ex) {
            // log and return empty if listing service unreachable
            System.err.println("lỗi gọi LISTING SERVICE từ COMPARE: " + ex.getMessage());
            return Collections.emptyList();
        }
    }
}
