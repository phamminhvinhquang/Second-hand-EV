package edu.uth.userservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
@Data 
public class TransactionHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "user_id", nullable = false)
    private Integer userId; // ⭐️ Khớp với kiểu Integer của User.java

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "product_name")
private String productName;

@Column(name = "product_img")
private String productImg;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "method", length = 50)
    private String method;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}