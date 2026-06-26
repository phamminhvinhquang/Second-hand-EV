
package edu.uth.listingservice.Service;

import java.util.List;
import java.util.Optional; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uth.listingservice.Model.ProductCondition;
import edu.uth.listingservice.Model.ProductSpecification;
import edu.uth.listingservice.Repository.ProductConditionRepository;
import edu.uth.listingservice.Repository.ProductSpecificationRepository;


import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;

// Import đã có từ lần trước
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ProductSpecificationServiceImpl implements ProductSpecificationService {

    @Autowired
    private ProductSpecificationRepository specificationRepository;
    @Autowired
    private CacheManager cacheManager; 
    @Autowired
    private ProductConditionRepository conditionRepository;

    // (Các hàm Read-Only giữ nguyên)
    @Override
    public List<ProductSpecification> getAll() {
        return specificationRepository.findAll();
    }
    @Override
    @Cacheable(value = "productSpecs", key = "#id")
    public ProductSpecification getById(Long id) {
        return specificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Specification not found with ID: ".concat(id.toString())));
    }
    @Override
    @Cacheable(value = "productSpecs", key = "'prod-' + #productId")
    public ProductSpecification getByProductId(Long productId) {
        return specificationRepository.findByProduct_ProductId(productId);
    }


    // [SỬA] HÀM NÀY ĐÃ ĐƯỢC CẬP NHẬT
    @Override
    @Transactional
    // [SỬA] Bỏ @Caching
    public ProductSpecification create(ProductSpecification specification) {
        if (specification.getCondition() != null && specification.getCondition().getConditionId() != null) {
            Long conditionId = specification.getCondition().getConditionId();
            ProductCondition managedCondition = conditionRepository.findById(conditionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Condition với ID: ".concat(conditionId.toString())));
            specification.setCondition(managedCondition);
        } else {
            specification.setCondition(null);
        }
        
        ProductSpecification savedSpec = specificationRepository.save(specification);

        // [SỬA] Dời logic cache vào afterCommit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Xóa cache danh sách
                cacheManager.getCache("productSpecs").clear(); 
                
                if (savedSpec.getProduct() != null) {
                    Long productId = savedSpec.getProduct().getProductId();
                    if (productId != null) {
                        cacheManager.getCache("productDetails").evictIfPresent(productId);
                    }
                }
            }
        });

        return savedSpec;
    }

    // [SỬA] HÀM NÀY ĐÃ ĐƯỢC CẬP NHẬT
    @Override
    @Transactional
    // [SỬA] Bỏ @Caching
    public ProductSpecification update(Long id, ProductSpecification updatedSpec) {
        ProductSpecification existingSpec = specificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Specification not found with ID: ".concat(id.toString())));

        if (updatedSpec.getCondition() != null && updatedSpec.getCondition().getConditionId() != null) {
            Long conditionId = updatedSpec.getCondition().getConditionId();
            ProductCondition managedCondition = conditionRepository.findById(conditionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Condition với ID: ".concat(conditionId.toString())));
            existingSpec.setCondition(managedCondition);
        } else {
            existingSpec.setCondition(null);
        }

        existingSpec.setYearOfManufacture(updatedSpec.getYearOfManufacture());
        existingSpec.setBrand(updatedSpec.getBrand());
        existingSpec.setMileage(updatedSpec.getMileage());
        existingSpec.setBatteryCapacity(updatedSpec.getBatteryCapacity());
        existingSpec.setBatteryType(updatedSpec.getBatteryType());
        existingSpec.setBatteryLifespan(updatedSpec.getBatteryLifespan());
        existingSpec.setCompatibleVehicle(updatedSpec.getCompatibleVehicle());
        existingSpec.setWarrantyPolicy(updatedSpec.getWarrantyPolicy());
        existingSpec.setMaxSpeed(updatedSpec.getMaxSpeed());
        existingSpec.setRangePerCharge(updatedSpec.getRangePerCharge());
        existingSpec.setColor(updatedSpec.getColor());
        existingSpec.setChargeTime(updatedSpec.getChargeTime());
        existingSpec.setChargeCycles(updatedSpec.getChargeCycles());

        ProductSpecification savedSpec = specificationRepository.save(existingSpec);

        // [SỬA] Dời logic cache vào afterCommit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (savedSpec.getProduct() != null) {
                    Long productId = savedSpec.getProduct().getProductId();
                    if (productId != null) {
                        cacheManager.getCache("productDetails").evictIfPresent(productId);
                        cacheManager.getCache("productSpecs").evictIfPresent("prod-" + productId);
                    }
                }
                cacheManager.getCache("productSpecs").evictIfPresent(id);
                cacheManager.getCache("userListings").clear();
                cacheManager.getCache("userListingPage").clear();
            }
        });

        return savedSpec;
    }

    // (Hàm delete đã được refactor ở lần trước)
    @Override
    @Transactional
    public void delete(Long id) {
        Optional<ProductSpecification> specOpt = specificationRepository.findById(id);
        if (specOpt.isEmpty()) { return; }
        
        ProductSpecification existingSpec = specOpt.get();
        Long productId = existingSpec.getProduct().getProductId();

        specificationRepository.deleteById(id);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheManager.getCache("userListings").clear();
                cacheManager.getCache("userListingPage").clear();
                
                if (productId != null) {
                    cacheManager.getCache("productDetails").evictIfPresent(productId);
                    cacheManager.getCache("productSpecs").evictIfPresent("prod-" + productId);
                }
                cacheManager.getCache("productSpecs").evictIfPresent(id);
            }
        });
    }
}