

package edu.uth.listingservice.DTO; 

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Thêm ignoreUnknown để an toàn,
// lỡ transaction-service gửi thêm trường (như transactionId)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OrderCompletedEventDTO {
    // transaction-service sẽ gửi các trường này
    private Long sellerId;
    private Long buyerId;
    private String productName;
    private Long price;
    
    // Đây là trường quan trọng nhất cho cả 2 service
    private Long productId;
}