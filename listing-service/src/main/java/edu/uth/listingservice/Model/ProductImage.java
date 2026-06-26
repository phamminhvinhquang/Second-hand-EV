package edu.uth.listingservice.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Product_Images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne
    @JoinColumn(name = "product_id")
     @JsonBackReference //  THÊM DÒNG NÀY (Đánh dấu đây là "con", sẽ không được render ra JSON)
    private Product product;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;


}

