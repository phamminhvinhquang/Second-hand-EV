package com.example.like_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "likes")
@Getter
@Setter
@NoArgsConstructor //entiry cần constructor không tham số (public) để hibernet/jpa tạo instance 
@AllArgsConstructor//constructor chứa tất cả các field(id, ...)
@Builder //để builder api để khởi tạo đối tượng
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    private String productname;
    private String imgurl;
    private Double price;
    private Integer yearOfManufacture;
    private String brand;
    private String conditionName;
    private Long mileage;
    
     private Long userId;   // người dùng  đã like
    private Long sellerId; // người bán sản phẩm
}
