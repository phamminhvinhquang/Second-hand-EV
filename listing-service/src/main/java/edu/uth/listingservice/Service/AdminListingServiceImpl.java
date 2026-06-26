
package edu.uth.listingservice.Service;
import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.DTO.AdminListingUpdateDTO;
import edu.uth.listingservice.DTO.ListingEventDTO; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.amqp.rabbit.core.RabbitTemplate; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Date;
import org.hibernate.Hibernate; 
import org.springframework.cache.annotation.Cacheable;
import edu.uth.listingservice.DTO.ProductDetailDTO;
import edu.uth.listingservice.Service.ProductDetailService;
import java.util.List;
import java.util.ArrayList;
import edu.uth.listingservice.Model.ProductImage;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// SỬA: Thêm 2 import
import org.springframework.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AdminListingServiceImpl implements AdminListingService {
    
    // SỬA: Thêm Logger
    private static final Logger log = LoggerFactory.getLogger(AdminListingServiceImpl.class);

    // SỬA: Thêm CacheManager
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private ProductDetailService productDetailService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ProductListingRepository listingRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate; 
    @Value("${app.rabbitmq.exchange}")
    private String listingExchange;
    @Value("${app.rabbitmq.routing-key}")
    private String notificationRoutingKey;
    @Value("${app.rabbitmq.product-events.exchange}")
    private String productEventsExchange;
    @Value("${app.rabbitmq.product-events.routing-key}")
    private String productEventsRoutingKey;

    @Override
    @Cacheable(value = "adminListings", key = "#status.name() + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    @Transactional(readOnly = true) 
    public Page<ProductListing> getListingsByStatus(ListingStatus status, Pageable pageable) {
        
        // SỬA LỖI: Tên hàm là "findByListingStatus", không phải "findByStatus"
        Page<ProductListing> listingPage = listingRepository.findByListingStatus(status, pageable);

        listingPage.getContent().forEach(listing -> {
            if (listing.getProduct() != null) {
                Hibernate.initialize(listing.getProduct().getImages());
                List<ProductImage> plainImages = new ArrayList<>(listing.getProduct().getImages());
                listing.getProduct().setImages(plainImages);
            }
        });

        return listingPage;
    }
    @Override
    @Cacheable(value = "adminSearchListings", key = "#query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    @Transactional(readOnly = true) 
    public Page<ProductListing> searchListings(String query, Pageable pageable) {
        
        Page<ProductListing> listingPage = listingRepository.searchByProductNameOrUserId(query, pageable);

        listingPage.getContent().forEach(listing -> {
            if (listing.getProduct() != null) {
                Hibernate.initialize(listing.getProduct().getImages());
                List<ProductImage> plainImages = new ArrayList<>(listing.getProduct().getImages());
                listing.getProduct().setImages(plainImages);
            }
        });
        
        return listingPage;
    }
    
 
    // SỬA: BỎ ANNOTATION @Caching
    @Override
    @Transactional
    public ProductListing approveListing(Long listingId) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (listing.getListingStatus() != ListingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING listings can be approved.");
        }

        listing.setListingStatus(ListingStatus.ACTIVE);
        listing.setAdminNotes(null);
        listing.setUpdatedAt(new Date());
        listing.setListingDate(new Date());

        ProductListing savedListing = listingRepository.save(listing);
        
        Long productId = savedListing.getProduct().getProductId();

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }
        ProductDetailDTO dtoToSend = productDetailService.getProductDetail(productId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // BƯỚC 1: XÓA CACHE THỦ CÔNG
                clearAllListingCaches();
                evictProductCaches(savedListing);

                // BƯỚC 2: GỬI TIN NHẮN
                ListingEventDTO event = new ListingEventDTO(
                    savedListing.getListingId(),
                    savedListing.getUserId(),
                    savedListing.getProduct().getProductName(),
                    "APPROVED"
                );
                rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

                AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
                messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);
                
                rabbitTemplate.convertAndSend(productEventsExchange, productEventsRoutingKey, dtoToSend);
            }
        });
        
        return savedListing;
    }

    // SỬA: BỎ ANNOTATION @Caching
    @Override
    @Transactional
    public ProductListing rejectListing(Long listingId, String reason) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (listing.getListingStatus() != ListingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING listings can be rejected.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason cannot be empty.");
        }

        listing.setListingStatus(ListingStatus.REJECTED);
        listing.setAdminNotes(reason);
        listing.setUpdatedAt(new Date());

        ProductListing savedListing = listingRepository.save(listing);
        Long productId = savedListing.getProduct().getProductId();

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }
        ProductDetailDTO dtoToSend = productDetailService.getProductDetail(productId);
         
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // BƯỚC 1: XÓA CACHE THỦ CÔNG
                clearAllListingCaches();
                evictProductCaches(savedListing);
                
                // BƯỚC 2: GỬI TIN NHẮN
                ListingEventDTO event = new ListingEventDTO(
                    savedListing.getListingId(),
                    savedListing.getUserId(),
                    savedListing.getProduct().getProductName(),
                    "REJECTED" 
                );
                rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

                AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
                messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);
                
                rabbitTemplate.convertAndSend(productEventsExchange, productEventsRoutingKey, dtoToSend);
            }
        });
        
        return savedListing;
    }

    // SỬA: BỎ ANNOTATION @Caching
    @Override
    @Transactional
    public ProductListing verifyListing(Long listingId) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));

        if (listing.getListingStatus() != ListingStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE listings can be verified.");
        }

        listing.setVerified(true);
        listing.setUpdatedAt(new Date());
        
        ProductListing savedListing = listingRepository.save(listing);
        Long productId = savedListing.getProduct().getProductId();

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }
        ProductDetailDTO dtoToSend = productDetailService.getProductDetail(productId);
        
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // BƯỚC 1: XÓA CACHE THỦ CÔNG
                clearAllListingCaches();
                evictProductCaches(savedListing);
                
                // BƯỚC 2: GỬI TIN NHẮN
                ListingEventDTO event = new ListingEventDTO(
                    savedListing.getListingId(),
                    savedListing.getUserId(),
                    savedListing.getProduct().getProductName(),
                    "VERIFIED"
                );
                rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

                AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
                messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);

                rabbitTemplate.convertAndSend(productEventsExchange, productEventsRoutingKey, dtoToSend);
            }
        });
        
        return savedListing;
    }

    // =======================================================
    // SỬA: THÊM 2 HÀM HELPER ĐỂ XÓA CACHE
    // =======================================================
    private void clearAllListingCaches() {
        try {
            cacheManager.getCache("activeListings").clear();
            cacheManager.getCache("userListings").clear();
            cacheManager.getCache("adminListings").clear();
            cacheManager.getCache("adminSearchListings").clear();
            cacheManager.getCache("userListingPage").clear();
            cacheManager.getCache("relatedListings").clear(); // <--- THÊM DÒNG NÀ
            log.info("Cleared all general listing caches.");
        } catch (Exception e) {
            log.warn("Error clearing all listing caches: {}", e.getMessage());
        }
    }
    
    private void evictProductCaches(ProductListing listing) {
        if (listing == null || listing.getProduct() == null) return;
        try {
            Long productId = listing.getProduct().getProductId();
            if (productId != null) {
                cacheManager.getCache("productDetails").evictIfPresent(productId);
                log.info("Evicted productDetails cache for productId: {}", productId);
            }
        } catch (Exception e) {
             log.warn("Error evicting product-specific cache for {}: {}", listing.getListingId(), e.getMessage());
        }
    }
}