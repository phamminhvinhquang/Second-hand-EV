package edu.uth.listingservice.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uth.listingservice.DTO.ProductDetailDTO;
import edu.uth.listingservice.Service.ProductDetailService;

@RestController
@RequestMapping("/api/product-details")

public class ProductDetailController {

    @Autowired
    private ProductDetailService productDetailService;


    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailDTO> getProductDetail(@PathVariable Long productId) {
        ProductDetailDTO productDetail = productDetailService.getProductDetail(productId);
        return ResponseEntity.ok(productDetail);
    }
}
