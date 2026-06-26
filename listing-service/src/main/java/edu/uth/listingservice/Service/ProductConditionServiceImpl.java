
package edu.uth.listingservice.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Caching;
import edu.uth.listingservice.Model.ProductCondition;
import edu.uth.listingservice.Repository.ProductConditionRepository;
import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.cache.CacheManager;

// [SỬA] Thêm 2 import
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ProductConditionServiceImpl implements ProductConditionService {
    
    @Autowired
    private CacheManager cacheManager;

    private final ProductConditionRepository conditionRepository;

    public ProductConditionServiceImpl(ProductConditionRepository conditionRepository) {
        this.conditionRepository = conditionRepository;
    }

    // (Các hàm Read-Only giữ nguyên)
    @Override
    @Cacheable("productConditions") 
    public List<ProductCondition> getAll() {
        return conditionRepository.findAll();
    }

    @Override
    @Cacheable(value = "productCondition", key = "#id")
    public ProductCondition getById(Long id) {
        return conditionRepository.findById(id).orElse(null);
    }

    // [SỬA] HÀM NÀY ĐÃ ĐƯỢC CẬP NHẬT
    @Override
    @Transactional // [SỬA] Thêm @Transactional
    // [SỬA] Bỏ @Caching
    public ProductCondition create(ProductCondition condition) {
        ProductCondition savedCondition = conditionRepository.save(condition);

        // [SỬA] Dời logic cache vào afterCommit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheManager.getCache("productConditions").clear();
                cacheManager.getCache("productCondition").clear();
            }
        });
        
        return savedCondition;
    }

    // [SỬA] HÀM NÀY ĐÃ ĐƯỢC CẬP NHẬT
    @Override
    @Transactional // [SỬA] Thêm @Transactional
    // [SỬA] Bỏ @Caching
    public ProductCondition update(Long id, ProductCondition condition) {
        ProductCondition existing = getById(id);
        if (existing != null) {
            existing.setConditionName(condition.getConditionName());
            ProductCondition savedCondition = conditionRepository.save(existing);
            
            // [SỬA] Dời logic cache vào afterCommit
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheManager.getCache("productConditions").clear();
                    cacheManager.getCache("productCondition").clear();
                    cacheManager.getCache("productDetails").clear();
                    cacheManager.getCache("userListings").clear();
                    cacheManager.getCache("userListingPage").clear();
                }
            });

            return savedCondition;
        }
        return null;
    }

    // [SỬA] HÀM NÀY ĐÃ ĐƯỢC CẬP NHẬT
    @Override
    @Transactional // [SỬA] Thêm @Transactional
    // [SỬA] Bỏ @Caching
    public void delete(Long id) {
        conditionRepository.deleteById(id);

        // [SỬA] Dời logic cache vào afterCommit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheManager.getCache("productConditions").clear();
                cacheManager.getCache("productCondition").clear();
                cacheManager.getCache("productDetails").clear();
                cacheManager.getCache("userListings").clear();
                cacheManager.getCache("userListingPage").clear();
            }
        });
    }
}