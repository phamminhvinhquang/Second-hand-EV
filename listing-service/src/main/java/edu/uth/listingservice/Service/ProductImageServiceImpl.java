package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ProductImage;
import edu.uth.listingservice.Repository.ProductImageRepository;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

// Import đã có từ lần trước
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ProductImageServiceImpl implements ProductImageService {

    @Autowired
    private ProductImageRepository productImageRepository;
    @Autowired
    private CacheManager cacheManager;
    
    @Override
    public List<ProductImage> getAllImages() {
        return productImageRepository.findAll();
    }

    @Override
    @Cacheable(value = "productImage", key = "#id") 
    public ProductImage getImageById(Long id) {
        return productImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found with ID: " + id));
    }

    @Override
    @Cacheable(value = "productImages", key = "#productId")
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProduct_ProductId(productId);
    }

    // [SỬA] HÀM NÀY ĐÃ ĐƯỢC CẬP NHẬT
    @Override
    @Transactional // [SỬA] Thêm @Transactional
    // [SỬA] Bỏ @Caching
    public ProductImage createImage(ProductImage productImage) {
        ProductImage savedImage = productImageRepository.save(productImage);

        // [SỬA] Dời logic cache vào afterCommit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (savedImage.getProduct() != null) {
                    Long productId = savedImage.getProduct().getProductId();
                    if (productId != null) {
                        cacheManager.getCache("productImages").evictIfPresent(productId);
                        cacheManager.getCache("productDetails").evictIfPresent(productId);
                    }
                }
                cacheManager.getCache("userListings").clear();
                cacheManager.getCache("userListingPage").clear();
            }
        });
        
        return savedImage;
    }

    // (Hàm deleteImage đã được refactor ở lần trước)
    @Override
    @Transactional
    public void deleteImage(Long id) {
        ProductImage image = productImageRepository.findById(id).orElse(null);
        if (image == null) { return; }
        Long productId = image.getProduct().getProductId();

        productImageRepository.deleteById(id);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (productId != null) {
                    cacheManager.getCache("productDetails").evictIfPresent(productId);
                    cacheManager.getCache("productImages").evictIfPresent(productId); 
                }
                cacheManager.getCache("productImage").evictIfPresent(id); 
                cacheManager.getCache("userListings").clear();
                cacheManager.getCache("userListingPage").clear();
            }
        });
    }
}