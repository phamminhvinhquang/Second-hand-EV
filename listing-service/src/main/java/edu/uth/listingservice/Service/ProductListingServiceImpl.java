
package edu.uth.listingservice.Service;

import org.springframework.data.domain.Page;
import java.util.Date;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import edu.uth.listingservice.Model.ProductImage;
import java.util.ArrayList; 
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.amqp.rabbit.core.RabbitTemplate; 
import edu.uth.listingservice.DTO.ListingEventDTO; 
import edu.uth.listingservice.DTO.ProductEventDTO; 
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 
import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Model.Product;
import edu.uth.listingservice.Model.ProductSpecification;
import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.Repository.ProductSpecificationRepository;
import edu.uth.listingservice.Repository.ProductRepository;
import edu.uth.listingservice.DTO.UpdateListingDTO;
import edu.uth.listingservice.DTO.AdminListingUpdateDTO;
import edu.uth.listingservice.Repository.ProductImageRepository;
import org.springframework.cache.annotation.Cacheable; 
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.hibernate.Hibernate; 
import org.springframework.cache.CacheManager; 
import edu.uth.listingservice.DTO.ProductDetailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.uth.listingservice.Config.RabbitMQConfig;
import edu.uth.listingservice.events.ListingNeedsUserDataDTO;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ProductListingServiceImpl implements ProductListingService {

    
    @Autowired
    private SimpMessagingTemplate messagingTemplate; 
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


    @Value("${app.rabbitmq.user.exchange}") 
    private String userExchange;
            
    private static final Logger log = LoggerFactory.getLogger(ProductListingServiceImpl.class);

    @Autowired
    private ProductListingRepository listingRepository;
    @Autowired 
    private ProductRepository productRepository;
    @Autowired 
    private ProductSpecificationRepository specRepository;
    @Autowired
    private ProductImageRepository productImageRepository;
    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private ProductDetailService productDetailService;
  
   
    @Override
    @Transactional
    public void deleteImageFromListing(Long listingId, Long imageId) {
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + listingId));
        
        Product product = listing.getProduct();
        Long productId = product.getProductId();

        ProductImage imageToRemove = product.getImages().stream()
                .filter(image -> image.getImageId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image with ID " + imageId + " not found in this product."));
        
        fileStorageService.delete(imageToRemove.getImageUrl());
        product.getImages().remove(imageToRemove);
        productRepository.save(product); 
        listing.setUpdatedAt(new Date());
        listingRepository.save(listing);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (productId != null) {
                    cacheManager.getCache("productDetails").evictIfPresent(productId);
                    cacheManager.getCache("productImages").evictIfPresent(productId); 
                }
                cacheManager.getCache("userListings").clear();
                cacheManager.getCache("userListingPage").clear();
                log.info("Caches evicted after deleting image {}.", imageId);
            }
        });
    }
    



   @Override
    @Transactional
    public void delete(Long id) {
        Optional<ProductListing> listingOpt = listingRepository.findById(id);
        if (listingOpt.isEmpty()) { return; }
        
        ProductListing listing = listingOpt.get();
        Product product = listing.getProduct();
        Long listingId = listing.getListingId();
        Long productId = (product != null) ? product.getProductId() : null; 
        
        if (product != null) {
            List<ProductImage> images = productImageRepository.findByProduct_ProductId(product.getProductId());
            for (ProductImage image : images) {
                fileStorageService.delete(image.getImageUrl());
            }
            
            listingRepository.delete(listing);
            productRepository.delete(product); 
        } else {
                listingRepository.delete(listing); 
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearAllListingCaches();
                if (productId != null) {
                    cacheManager.getCache("productDetails").evictIfPresent(productId);
                    cacheManager.getCache("relatedListings").clear(); 
                }

                java.util.Map<String, Object> deleteMessage = new java.util.HashMap<>();
                deleteMessage.put("action", "delete");
                deleteMessage.put("listingId", listingId);
                messagingTemplate.convertAndSend("/topic/admin/listingUpdate", deleteMessage);
                
                if (productId != null) {
                    ProductEventDTO event = new ProductEventDTO(
                        productId,
                        "LISTING_DELETED"
                    );
                    rabbitTemplate.convertAndSend(productEventsExchange, productEventsRoutingKey, event);
                }
            }
        });
    }

    @Override
    @Cacheable(value = "userListingPage", key = "#userId + '-' + #listingId + '-' + #pageSize")
    public int findPageForListing(Long userId, Long listingId, int pageSize) {
        
        Optional<Integer> indexOptional = listingRepository
                .findZeroBasedIndexByUserIdAndListingId(userId, listingId);

        if (indexOptional.isPresent()) {
            int index = indexOptional.get(); 
            return index / pageSize;
        }

        return 0;
    }
    @Override
    public List<ProductListing> getAll() { return listingRepository.findAll(); }
    @Override
    public ProductListing getById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found with ID: " + id));
    }

    //  Thêm lại vòng lặp để "giải phóng" proxy
    @Cacheable(value = "activeListings", key = "#type + '-' + #sortBy + '-' + #page + '-' + #size")
    @Override
    @Transactional(readOnly = true) 
    public Page<ProductListing> getActiveListings(String type, String sortBy, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "listingDate");
        if ("price".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "product.price");
        }
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ProductListing> listingPage;
        if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
            listingPage = listingRepository.findByStatusAndProductType(ListingStatus.ACTIVE, type, pageable);
        } else {
            listingPage = listingRepository.findByListingStatus(ListingStatus.ACTIVE, pageable);
        }

        // SỬA: Thêm lại vòng lặp này để "giải phóng" proxy, giúp cache an toàn
        listingPage.getContent().forEach(listing -> {
            if (listing.getProduct() != null && listing.getProduct().getImages() != null) {
                List<ProductImage> plainImages = new ArrayList<>(listing.getProduct().getImages());
                listing.getProduct().setImages(plainImages);
            }
        });
        
        return listingPage;
    }


    // SỬA: Thêm lại vòng lặp để "giải phóng" proxy
    @Override
    @Cacheable(value = "relatedListings", key = "#productType + '-' + #excludeProductId")
    @Transactional(readOnly = true) 
    public List<ProductListing> findRandomRelated(String productType, Long excludeProductId, int limit) {
        List<ProductListing> listings = listingRepository.findRandomRelatedProducts(productType, excludeProductId, limit);
        
        // SỬA: Thêm lại vòng lặp này để "giải phóng" proxy, giúp cache an toàn
        listings.forEach(listing -> {
            if (listing.getProduct() != null && listing.getProduct().getImages() != null) {
                List<ProductImage> plainImages = new ArrayList<>(listing.getProduct().getImages());
                listing.getProduct().setImages(plainImages);
            }
        });
        
        return listings;
    }

    @Override
    public ProductListing update(Long id, ProductListing updated) { 
        ProductListing existing = getById(id);
        existing.setListingStatus(updated.getListingStatus());
        existing.setProduct(updated.getProduct());
        existing.setUserId(updated.getUserId());
        existing.setUpdatedAt(new Date());
        return listingRepository.save(existing);
    }

    // SỬA: Thêm lại vòng lặp để "giải phóng" proxy (Đây là hàm gây lỗi trong log)
    @Override
    @Cacheable(value = "userListings", key = "#userId + '-' + #page + '-' + #size")
    @Transactional(readOnly = true) 
    public Page<ProductListing> getByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<ProductListing> listingPage = listingRepository.findByUserId(userId, pageable);
        
        // SỬA: Thêm lại vòng lặp này để "giải phóng" proxy, giúp cache an toàn
        listingPage.getContent().forEach(listing -> {
            if (listing.getProduct() != null && listing.getProduct().getImages() != null) {
                List<ProductImage> plainImages = new ArrayList<>(listing.getProduct().getImages());
                listing.getProduct().setImages(plainImages);
            }
        });
        
        return listingPage;
    }


    
   @Override
    @Transactional
    public ProductListing create(ProductListing listing) {
        
        if (listing.getUserId() != null) {
            log.warn("Using fallback seller name for user {}. Data will be fetched asynchronously.", listing.getUserId());
            listing.setSellerName("Unknown Seller (ID: " + listing.getUserId() + ")"); 
            listing.setSellerEmail("N/A");
        }
        
        listing.setListingDate(new Date());
        listing.setUpdatedAt(new Date());
        if (listing.getListingStatus() == null) {
            listing.setListingStatus(ListingStatus.PENDING);
        }
        
        ProductListing savedListing = listingRepository.save(listing);
        
        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearAllListingCaches();

                if (savedListing.getUserId() != null) {
                    ListingNeedsUserDataDTO eventPayload = new ListingNeedsUserDataDTO(
                        savedListing.getListingId(),
                        savedListing.getUserId()
                    );
                    try {
                        rabbitTemplate.convertAndSend(
                            userExchange,
                            RabbitMQConfig.ASYNC_REQUEST_KEY, 
                            eventPayload
                        );
                        log.info("Sent async request for user data for listingId: {}", savedListing.getListingId());
                    } catch (Exception e) {
                        log.error("Failed to send async request for listingId: {}. Error: {}", savedListing.getListingId(), e.getMessage());
                    }
                }

                AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
                messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);
            }
        });
        
        return savedListing;
    }
    
 @Override
    @Transactional
    public ProductListing updateListingDetails(Long listingId, UpdateListingDTO dto) {
        ProductListing listing = getById(listingId);
        Product product = listing.getProduct();
        ProductSpecification spec = product.getSpecification();

        if (listing.getListingStatus() == ListingStatus.SOLD) {
            throw new IllegalStateException("Không thể chỉnh sửa tin đã bán.");
        }
        
        if (listing.getListingStatus() == ListingStatus.ACTIVE) {
             product.setPrice(dto.getPrice());
             product.setDescription(dto.getDescription());
             listing.setPhone(dto.getPhone());
             listing.setLocation(dto.getLocation());
             spec.setWarrantyPolicy(dto.getWarrantyPolicy());
             
        } else { 
             product.setProductName(dto.getProductName());
             spec.setBrand(dto.getBrand());
             product.setPrice(dto.getPrice());
             product.setDescription(dto.getDescription());
             listing.setPhone(dto.getPhone());
             listing.setLocation(dto.getLocation());
             spec.setWarrantyPolicy(dto.getWarrantyPolicy());
             spec.setBatteryType(dto.getBatteryType());
             spec.setChargeTime(dto.getChargeTime());
             spec.setChargeCycles(dto.getChargeCycles());

             if (!"battery".equals(product.getProductType())) {
                spec.setRangePerCharge(dto.getRangePerCharge());
                spec.setMileage(dto.getMileage());
                spec.setBatteryCapacity(dto.getBatteryCapacity());
                spec.setColor(dto.getColor());
                spec.setMaxSpeed(dto.getMaxSpeed());
            } else {
                spec.setCompatibleVehicle(dto.getCompatibleVehicle());
                spec.setBatteryLifespan(dto.getBatteryLifespan());
                spec.setBatteryCapacity(dto.getBatteryCapacity());
            }
        }
        
        listing.setUpdatedOnce(true); 
        listing.setVerified(false); 
        listing.setListingStatus(ListingStatus.PENDING); 
        
        listing.setListingDate(new Date());
        listing.setUpdatedAt(new Date());
        product.setUpdatedAt(new Date());

        productRepository.save(product);
        specRepository.save(spec);
       ProductListing savedListing = listingRepository.save(listing);
        
        Long productId = savedListing.getProduct().getProductId();

        if (savedListing.getProduct() != null) {
            Hibernate.initialize(savedListing.getProduct());
        }
        
        ProductDetailDTO dtoToSend = productDetailService.getProductDetail(productId);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearAllListingCaches();
                evictProductCaches(savedListing); 

                AdminListingUpdateDTO updateDTO = new AdminListingUpdateDTO(savedListing);
                messagingTemplate.convertAndSend("/topic/admin/listingUpdate", updateDTO);
        
                rabbitTemplate.convertAndSend(productEventsExchange, productEventsRoutingKey, dtoToSend);
            }
        });
        return savedListing;
    }

    @Override
    @Transactional
    public ProductListing markAsSold(Long listingId) {
        ProductListing listing = getById(listingId);
        if (listing.getListingStatus() == ListingStatus.SOLD) {
            throw new IllegalStateException("Tin này đã ở trạng thái 'Đã Bán'.");
        }
        listing.setListingStatus(ListingStatus.SOLD);
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
                clearAllListingCaches();
                evictProductCaches(savedListing);

                ListingEventDTO event = new ListingEventDTO(
                    savedListing.getListingId(),
                    savedListing.getUserId(),
                    savedListing.getProduct().getProductName(),
                    "SOLD"
                );
                rabbitTemplate.convertAndSend(listingExchange, notificationRoutingKey, event);

                rabbitTemplate.convertAndSend(productEventsExchange, productEventsRoutingKey, dtoToSend);
            }
        });
        return savedListing;
    }

    @Override
    @Transactional
    public ProductListing addImagesToListing(Long listingId, List<MultipartFile> files) {
        ProductListing listing = getById(listingId);
        Product product = listing.getProduct();
        if (files != null && !files.isEmpty()) {
            List<ProductImage> newImages = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String imageUrl = fileStorageService.store(file);
                    ProductImage newImage = new ProductImage(null, product, imageUrl);
                    newImages.add(newImage);
                }
            }
            productImageRepository.saveAll(newImages);
        }
        listing.setUpdatedAt(new Date());
        ProductListing savedListing = listingRepository.save(listing); 

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearAllListingCaches();
                evictProductCaches(savedListing); 
            }
        });

        return savedListing;
    }

    // =======================================================
    // HÀM HELPER (Giữ nguyên)
    // =======================================================
    private void clearAllListingCaches() {
        try {
            cacheManager.getCache("activeListings").clear();
            cacheManager.getCache("userListings").clear();
            cacheManager.getCache("adminListings").clear();
            cacheManager.getCache("adminSearchListings").clear();
            cacheManager.getCache("userListingPage").clear();
            cacheManager.getCache("relatedListings").clear(); 
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
                cacheManager.getCache("productImages").evictIfPresent(productId); 
                
                cacheManager.getCache("productSpecs").evictIfPresent("prod-" + productId);
                if (listing.getProduct().getSpecification() != null) {
                    cacheManager.getCache("productSpecs").evictIfPresent(listing.getProduct().getSpecification().getSpecId());
                }
                log.info("Evicted product/spec/images caches for productId: {}", productId);
            }
        } catch (Exception e) {
             log.warn("Error evicting product-specific cache for {}: {}", listing.getListingId(), e.getMessage());
        }
    }
}