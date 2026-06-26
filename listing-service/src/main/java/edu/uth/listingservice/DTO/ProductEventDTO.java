
package edu.uth.listingservice.DTO;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO này được sử dụng để thông báo các sự kiện quan trọng (như DELETED) 
 * mà không thể gửi ProductDetailDTO đầy đủ.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEventDTO {
    
    // ID của sản phẩm đã thay đổi
    private Long productId; 
    
    // Loại sự kiện (ví dụ: "LISTING_DELETED")
    private String eventType; 
}