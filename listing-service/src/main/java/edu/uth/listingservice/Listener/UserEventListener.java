
package edu.uth.listingservice.Listener;

import edu.uth.listingservice.DTO.UserDTO;
import edu.uth.listingservice.DTO.UserEventDTO;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Repository.ProductListingRepository;
import org.springframework.transaction.annotation.Transactional; 
import java.util.List; 

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);
    private static final String USER_CACHE_NAME = "users";
    private static final String PRODUCT_DETAIL_CACHE_NAME = "productDetails"; 

    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private ProductListingRepository listingRepository;

    @RabbitListener(queues = "${app.rabbitmq.user.queue.listener}")
    @Transactional
    public void handleUserEvent(@Payload UserEventDTO userEvent) { 
        log.info("Received user event from MQ: {}", userEvent.getEmail());

        if (userEvent == null || userEvent.getId() == null) {
            log.warn("Received invalid user event (null or no ID).");
            return;
        }

        // 1. CẬP NHẬT CACHE "users" (kho "tạm")
        try {
            updateCache(userEvent);
        } catch (Exception e) {
            log.error("Failed to update cache for user ID: {}. Error: {}", userEvent.getId(), e.getMessage());
        }

        // 2. CẬP NHẬT CSDL (kho "bản sao")
        //  SỬA ĐỔI: Hàm này bây giờ sẽ xóa cả cache "productDetails"
        try {
            updateExistingListingsInDb(userEvent);
        } catch (Exception e) {
            log.error("Failed to update DB listings for user ID: {}. Error: {}", userEvent.getId(), e.getMessage());
        }
    }
    
    /**
     * Helper: Cập nhật Cache "users"
     */
    private void updateCache(UserEventDTO userEvent) {
        Cache userCache = cacheManager.getCache(USER_CACHE_NAME);
        if (userCache == null) {
            log.error("Cache '{}' not found.", USER_CACHE_NAME);
            return;
        }
        
        UserDTO userToCache = new UserDTO(); 
        userToCache.setId(userEvent.getId());
        userToCache.setName(userEvent.getName());
        userToCache.setEmail(userEvent.getEmail());
        userToCache.setPhone(userEvent.getPhone());
        userToCache.setAddress(userEvent.getAddress());

        userCache.put(userEvent.getId().longValue(), userToCache);
        
        log.info("Successfully cached user data for ID: {}", userToCache.getId());
    }

    /**
     * Helper: Cập nhật CSDL VÀ XÓA CACHE "productDetails"
     */
    @Transactional
    private void updateExistingListingsInDb(UserEventDTO userEvent) {
        Long userId = userEvent.getId().longValue();
        
        List<ProductListing> listings = listingRepository.findByUserId(userId); 
        
        if (listings == null || listings.isEmpty()) {
            log.info("No existing listings found in DB for user ID: {}", userId);
            return; 
        }

        log.info("Found {} existing listings in DB to update for user ID: {}", listings.size(), userId);
        
        // Lấy cache "productDetails" một lần
        Cache productDetailsCache = cacheManager.getCache(PRODUCT_DETAIL_CACHE_NAME);
        if (productDetailsCache == null) {
            log.error("Cache '{}' not found. Cannot evict product details.", PRODUCT_DETAIL_CACHE_NAME);
            // (Vẫn tiếp tục cập nhật CSDL)
        }

        for (ProductListing listing : listings) {
            // 1. Cập nhật CSDL
            listing.setSellerName(userEvent.getName());
            listing.setSellerEmail(userEvent.getEmail());
            
           
            //  XÓA CACHE "productDetails" TƯƠNG ỨNG
            
            if (listing.getProduct() != null && productDetailsCache != null) {
                Long productId = listing.getProduct().getProductId();
                if (productId != null) {
                    productDetailsCache.evictIfPresent(productId);
                    log.info("Evicted productDetails cache for productId: {}", productId);
                }
            }
            
        }
        
        // 2. Lưu tất cả thay đổi CSDL
        listingRepository.saveAll(listings); 
        log.info("Successfully updated DB listings for user ID: {}", userId);

       
        try {
            cacheManager.getCache("userListings").clear();
            cacheManager.getCache("userListingPage").clear();
            cacheManager.getCache("adminListings").clear();
            cacheManager.getCache("adminSearchListings").clear();
            cacheManager.getCache("activeListings").clear();
            log.info("Evicted all listing caches after updating user info for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Error evicting listing caches: {}", e.getMessage());
        }
     
    }
}