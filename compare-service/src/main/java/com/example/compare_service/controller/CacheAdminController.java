package com.example.compare_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/cache")
public class CacheAdminController {

    @Autowired
    private CacheManager cacheManager;

    @DeleteMapping("/compare-listings")
    public ResponseEntity<?> clearCompareCache() {
        if (cacheManager.getCache("compareListings") != null) {
            cacheManager.getCache("compareListings").clear();
        }
        return ResponseEntity.ok().build();
    }
}
