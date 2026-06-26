package com.example.purchase_service.dto;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Import DTO người dùng mới mà chúng ta vừa tạo
import com.example.purchase_service.dto.UserDTO; 

/**
 * DTO này dùng để hứng dữ liệu từ Feign Client (listing-service).
 * Chúng ta SỬA LẠI trường 'sellerId' thành 'seller' để khớp với JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Rất quan trọng
@Data
public class ProductDetailDTO {
    
    private Long productId;
    private String productName;
    private Long price;
    private List<String> imageUrls;
    
    // (Giữ nguyên) Dùng để tương thích nếu listing-service trả về 'images'
    private java.util.List<com.example.purchase_service.dto.ProductImageDTO> images;

    // === 🛑 THAY ĐỔI QUAN TRỌNG ===
    // Đổi từ: private Integer sellerId;
    // Thành: private UserDTO seller;
    // Tên 'seller' phải khớp với key trong JSON trả về từ listing-service
    private UserDTO seller; 
}