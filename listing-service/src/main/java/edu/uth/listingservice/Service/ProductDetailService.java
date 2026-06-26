
package edu.uth.listingservice.Service;
import edu.uth.listingservice.Exception.ResourceNotFoundException;
import java.util.List;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import edu.uth.listingservice.DTO.ProductDetailDTO;
import edu.uth.listingservice.DTO.UserDTO;
import edu.uth.listingservice.Model.Product;
import edu.uth.listingservice.Model.ProductImage;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Model.ProductSpecification;
import edu.uth.listingservice.Repository.ProductImageRepository;
import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.Repository.ProductRepository;
import edu.uth.listingservice.Repository.ProductSpecificationRepository;
import org.springframework.cache.annotation.Cacheable; 
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductDetailService {

    @Autowired private ProductRepository productRepository;
    @Autowired private ProductImageRepository imageRepository;
    @Autowired private ProductSpecificationRepository specificationRepository;
    @Autowired private ProductListingRepository listingRepository;


    @Cacheable(value = "users", key = "#userId")
    public UserDTO getSellerDetails(Long userId) {
        return createFallbackUser(userId);
    }

    /**
     * SỬA LẠI HÀM NÀY
     */
    @Cacheable(value = "productDetails", key = "#productId") 
    @Transactional
    public ProductDetailDTO getProductDetail(Long productId) {

        Product product = productRepository.findById(productId)
               .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin đăng này. Tin có thể đã bị xóa hoặc không tồn tại."));

        List<ProductImage> images = imageRepository.findByProduct_ProductId(productId);
        ProductSpecification spec = specificationRepository.findByProduct_ProductId(productId);
        ProductListing listing = listingRepository.findByProduct_ProductId(productId);

        UserDTO seller = null;
        if (listing != null && listing.getUserId() != null) {
            
            seller = new UserDTO(); // DTO này từ File 6
            seller.setId(listing.getUserId().intValue());
            
            // Đọc trực tiếp từ các trường "bản sao" trong CSDL
            seller.setName(listing.getSellerName());
            seller.setEmail(listing.getSellerEmail());
            
            
            // Sao chép Phone/Location của BÀI ĐĂNG vào object SELLER
            // =======================================================
            seller.setPhone(listing.getPhone());
            seller.setAddress(listing.getLocation());
           
        }

        // Truyền listing và seller DTO (đã có phone/address) vào constructor
        return new ProductDetailDTO(product, images, spec, listing, seller);
    }

    /**
     * GIỮ NGUYÊN HÀM NÀY.
     */
    private UserDTO createFallbackUser(Long userId) { 
        UserDTO fallback = new UserDTO();
        fallback.setId(userId.intValue()); 
        fallback.setName("Unknown Seller (ID: " + userId + ")");
        fallback.setEmail("N/A");
        fallback.setPhone("N/A"); 
        fallback.setAddress("N/A");
        return fallback;
    }
}