package edu.uth.listingservice.Model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_type", nullable = false)
    private String productType;

   @Column(name = "price", nullable = false)
private Long price;

    @Column(name = "ai_suggested_price")
    private Long aiSuggestedPrice;

    private String description;



    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Column(name = "updated_at")
    private Date updatedAt = new Date();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL,orphanRemoval = true)
     @JsonManagedReference
    private List<ProductImage> images;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
     @JsonManagedReference 
    private ProductSpecification specification;
}
