package edu.uth.listingservice.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager; 
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import edu.uth.listingservice.Model.Product;
import edu.uth.listingservice.Repository.ProductRepository;

import org.springframework.transaction.annotation.Transactional; 

// [SỬA] Thêm 2 import
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


@Service
public class ProductServiceImpl implements ProductService {


    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager; 

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }


    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }


    @Override
    @Transactional
    public Product createProduct(Product product) {
        // (Không cần xóa cache khi chỉ tạo product, vì nó chưa gắn với listing)
        return productRepository.save(product);
    }


   @Override
    @Transactional
    // [SỬA] Bỏ @Caching
    public Product updateProduct(Long id, Product product) {
        product.setProductId(id);
        Product savedProduct = productRepository.save(product);

        // [SỬA] Dời logic cache vào afterCommit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheManager.getCache("productDetails").evictIfPresent(id);
                cacheManager.getCache("productSpecs").evictIfPresent("prod-" + id);
                cacheManager.getCache("userListings").clear();
                cacheManager.getCache("userListingPage").clear();
            }
        });
        
        return savedProduct;
    }


   @Override
    @Transactional
    // [SỬA] Bỏ @Caching
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);

        // [SỬA] Dời logic cache vào afterCommit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheManager.getCache("productDetails").evictIfPresent(id);
                cacheManager.getCache("productSpecs").evictIfPresent("prod-" + id);
                cacheManager.getCache("userListings").clear();
                cacheManager.getCache("userListingPage").clear();
            }
        });
    }
}