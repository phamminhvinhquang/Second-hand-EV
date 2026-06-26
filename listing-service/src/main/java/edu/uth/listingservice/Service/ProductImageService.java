package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ProductImage;

import java.util.List;

public interface ProductImageService {
    List<ProductImage> getAllImages();
    ProductImage getImageById(Long id);
    List<ProductImage> getImagesByProductId(Long productId);
    ProductImage createImage(ProductImage productImage);

    void deleteImage(Long id);
}

