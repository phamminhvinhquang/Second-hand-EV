
package edu.uth.example.review_service.Model;

import java.util.ArrayList; 
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviewable_transactions")
@Data
@NoArgsConstructor
public class ReviewableTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long productId; 

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private Long buyerId;

    @Column
    private String productName;
    
    @Column
    private Long price;

    @Column(nullable = false)
    private boolean isSellerReviewed = false;

    @Column(nullable = false)
    private boolean isBuyerReviewed = false;

    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt; 

    
    @JsonManagedReference 
    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    public ReviewableTransaction(Long productId, Long sellerId, Long buyerId, Date expiresAt, String productName, Long price) {
        this.productId = productId; 
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.expiresAt = expiresAt;
        this.productName = productName;
        this.price = price;
    }
}