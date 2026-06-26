package com.example.compare_service.controller;

import com.example.compare_service.client.ListingClient;
import com.example.compare_service.dto.ProductDTO;
import com.example.compare_service.dto.ProductDetailDTO;
import com.example.compare_service.dto.ProductListingDTO;
import com.example.compare_service.service.CompareService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compares")
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;
    private final ListingClient listingClient;

    @GetMapping("/listings")
    public List<ProductListingDTO> getListings(@RequestParam(required = false) String type,
                                               @RequestParam(defaultValue = "100") int limit) {
        // Lấy danh sách từ cache/service
        List<ProductListingDTO> all = compareService.getListings(type, limit);

        if (type == null || type.trim().isEmpty()) return all;

        final String canonicalType = mapRequestedTypeToCanonical(type);
        if (canonicalType == null) return all;

        // Lọc Server-side: Kiểm tra cả Tên và Loại sản phẩm
        return all.stream()
                .filter(l -> {
                    if (l.getProduct() == null) return false;
                    return matchesType(l.getProduct(), canonicalType);
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/listings/{id}")
    public ProductListingDTO getListing(@PathVariable Long id) {
        return listingClient.getListingById(id);
    }

    @GetMapping("/product-detail/{productId}")
    public ProductDetailDTO getProductDetail(@PathVariable Long productId) {
        return listingClient.getProductDetail(productId);
    }

    // ---------- Helper Methods ----------

    private boolean matchesType(ProductDTO product, String canonicalType) {
        if (canonicalType == null) return true;
        
        // Kết hợp cả ProductType và ProductName để kiểm tra
        String typeStr = (product.getProductType() != null) ? product.getProductType().toLowerCase() : "";
        String nameStr = (product.getProductName() != null) ? product.getProductName().toLowerCase() : "";
        
        // Logic kiểm tra: (Có từ khóa mong muốn) VÀ (KHÔNG có từ khóa cấm)
        switch (canonicalType) {
            case "car":
                boolean isCar = typeStr.contains("car") || typeStr.contains("oto") || typeStr.contains("ô tô") || 
                                nameStr.contains("car") || nameStr.contains("oto") || nameStr.contains("ô tô") ||
                                nameStr.contains("sedan") || nameStr.contains("suv") || nameStr.contains("vf"); // VF thường là ô tô

                // Từ khóa cấm cho Ô tô: motor, xe máy, scooter, bike (tránh motorbike)
                boolean isNotCar = typeStr.contains("motor") || typeStr.contains("xemay") || typeStr.contains("xe máy") || typeStr.contains("bike") ||
                                   nameStr.contains("motor") || nameStr.contains("xemay") || nameStr.contains("xe máy") || nameStr.contains("bike") || nameStr.contains("scooter");
                
                return isCar && !isNotCar;

            case "motorbike":
                boolean isMotor = typeStr.contains("motor") || typeStr.contains("xemay") || typeStr.contains("xe máy") ||
                                  nameStr.contains("motor") || nameStr.contains("xemay") || nameStr.contains("xe máy") || nameStr.contains("scooter");
                return isMotor;

            case "bike":
                boolean isBike = typeStr.contains("bike") || typeStr.contains("xedap") || typeStr.contains("xe đạp") || typeStr.contains("bicycle") ||
                                 nameStr.contains("bike") || nameStr.contains("xedap") || nameStr.contains("xe đạp") || nameStr.contains("bicycle");
                
                // Từ khóa cấm cho Xe đạp: motor, máy (để tránh xe máy/motorbike)
                boolean isNotBike = typeStr.contains("motor") || typeStr.contains("may") ||
                                    nameStr.contains("motor") || nameStr.contains("may");
                
                return isBike && !isNotBike;

            case "battery":
                return typeStr.contains("pin") || typeStr.contains("battery") ||
                       nameStr.contains("pin") || nameStr.contains("battery");

            default:
                return false;
        }
    }

    private String mapRequestedTypeToCanonical(String requestedType) {
        if (requestedType == null) return null;
        String r = requestedType.trim().toLowerCase();
        r = r.replaceAll("[ôóòỏõộồở]+", "o");
        r = r.replaceAll("[\\s_]+", " ");
        
        if (r.contains("oto") || r.contains("ô tô") || r.contains("car")) return "car";
        if (r.contains("xemay") || r.contains("xe máy") || r.contains("motor")) return "motorbike";
        if (r.contains("xedap") || r.contains("xe đạp") || r.contains("bike")) return "bike";
        if (r.contains("pin") || r.contains("battery")) return "battery";
        return null;
    }
}