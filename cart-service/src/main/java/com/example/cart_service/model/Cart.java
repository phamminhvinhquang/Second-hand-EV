package com.example.cart_service.model;

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
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor //entity cần constructor không tham số(public) để Hibernate/JPA tạo instance 
@AllArgsConstructor //constructor chứa tất cả các field(id, productName,...)
@Builder //builder API để khởi tạo đối tượng
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    private String productname;
    private String imgurl;
    private Double price;
    private Double total;
    private Integer yearOfManufacture;
    private String brand;
    private String conditionName;
    private Long mileage;
    //ID của người bán sản phẩm này
    private Long sellerId; 
    // ID của người dùng sở hữu giỏ hàng này (người mua)
    private Long userId; 
}
