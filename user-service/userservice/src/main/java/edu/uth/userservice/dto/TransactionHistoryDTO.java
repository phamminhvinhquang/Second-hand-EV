package edu.uth.userservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionHistoryDTO {
    private Long id;
    private String transactionId;
    private BigDecimal amount;
    private String method;
    private String type;
    private String status;
    private LocalDateTime createdAt;
    
    // ⭐️ Thông tin người bán (được map thêm vào)
    private Long sellerId;
    private String sellerName; 
    private Integer userId;
    private String productName;
    private String productImg;
}