package edu.uth.listingservice.Model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Product_Listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Long listingId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "user_id")
    private Long userId; // Không FK vì lấy từ User service

    @Column(name = "phone", nullable = false)
    private String phone; //  Thêm số điện thoại liên hệ

    @Column(name = "location", nullable = false)
    private String location; //  Thêm khu vực giao dịch

    //  SỬA ĐỔI TẠI ĐÂY
    @Enumerated(EnumType.STRING) // Báo cho JPA lưu tên của enum (VD: 'PENDING')
    @Column(name = "listing_status", nullable = false)
    private ListingStatus listingStatus = ListingStatus.PENDING; // Gán giá trị mặc định

    @Column(name = "listing_date")
    private Date listingDate = new Date();

    @Column(name = "updated_at")
    private Date updatedAt = new Date();
    @Column(name = "updated_once", columnDefinition = "BOOLEAN DEFAULT FALSE")
private boolean updatedOnce = false;
@Column(name = "is_verified", nullable = false)
    private boolean verified = false; // Mặc định là false (chưa kiểm định)

    @Column(name = "admin_notes")
    private String adminNotes; // Ghi chú của admin, ví dụ: lý do từ chối
    
    @Column(name = "seller_name", length = 255)
    private String sellerName;
    
    @Column(name = "seller_email", length = 255)
    private String sellerEmail;
}