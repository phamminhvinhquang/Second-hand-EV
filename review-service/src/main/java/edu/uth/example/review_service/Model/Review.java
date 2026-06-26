
package edu.uth.example.review_service.Model;

import java.util.Date; 

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   
    @JsonBackReference 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private ReviewableTransaction transaction;

    @Column(nullable = false)
    private Long reviewerId; 

    @Column(nullable = false)
    private Long reviewedPartyId; 

    @Column
    private String reviewerName;

    @Column(nullable = false)
    private int rating; 

    @Column(length = 1000)
    private String comment;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt; 

    public Review(ReviewableTransaction transaction, Long reviewerId, Long reviewedPartyId, int rating, String comment, String reviewerName) {
        this.transaction = transaction;
        this.reviewerId = reviewerId;
        this.reviewedPartyId = reviewedPartyId;
        this.rating = rating;
        this.comment = comment;
        this.reviewerName = reviewerName;
    }
}