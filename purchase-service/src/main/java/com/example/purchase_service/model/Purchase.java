package com.example.purchase_service.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", length = 100, unique = true)
    private String transactionId;
    
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 500)
    private String productName;

    @Column(name = "product_image", length = 1000)
    private String productImage;

    @Column(name = "price")
    private Double price;

    @Column(length = 50)
    private String status = "waiting_delivery"; // waiting_delivery | completed | returned

    @Column(name = "customer_full_name", length = 200)
    private String fullName;

    @Column(length = 60)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
