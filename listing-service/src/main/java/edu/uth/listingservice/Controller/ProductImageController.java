package edu.uth.listingservice.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uth.listingservice.Model.ProductImage;
import edu.uth.listingservice.Service.ProductImageService;

@RestController
@RequestMapping("/api/images")

public class ProductImageController {

    @Autowired
    private ProductImageService productImageService;

    @GetMapping
    public List<ProductImage> getAllImages() {
        return productImageService.getAllImages();
    }

    @GetMapping("/{id}")
    public ProductImage getImageById(@PathVariable Long id) {
        return productImageService.getImageById(id);
    }

    @GetMapping("/product/{productId}")
    public List<ProductImage> getImagesByProductId(@PathVariable Long productId) {
        return productImageService.getImagesByProductId(productId);
    }

    @PostMapping
    public ProductImage createImage(@RequestBody ProductImage productImage) {
        return productImageService.createImage(productImage);
    }


    @DeleteMapping("/{id}")
    public void deleteImage(@PathVariable Long id) {
        productImageService.deleteImage(id);
    }
}
