package com.example.search_service.controller;

import com.example.search_service.dto.SearchResultDTO;
import com.example.search_service.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/searchs")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<List<SearchResultDTO>> search(
            @RequestParam(name="type", required=false, defaultValue="all") String type,
            @RequestParam(name="q", required=false) String q,
            @RequestParam(name="location", required=false) String location,
            @RequestParam(name="brand", required=false) String brand,
            @RequestParam(name="batteryType", required=false) String batteryType,
            @RequestParam(name="yearOfManufacture", required=false) Integer yearOfManufacture,
            @RequestParam(name="priceMin", required=false) Long priceMin,
            @RequestParam(name="priceMax", required=false) Long priceMax,
            @RequestParam(name="batteryCapacity", required=false) String batteryCapacity,
            @RequestParam(name="mileageRange", required=false) String mileageRange,
            @RequestParam(name="conditionName", required=false) String conditionName
    ) {
        List<SearchResultDTO> results = searchService.search(type, q, location, brand, batteryType,
                yearOfManufacture, priceMin, priceMax, batteryCapacity, mileageRange, conditionName);
        return ResponseEntity.ok(results);
    }
}
