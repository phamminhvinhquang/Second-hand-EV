package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.DTO.UpdateListingDTO;
import java.util.List;
import org.springframework.data.domain.Page; 
import org.springframework.web.multipart.MultipartFile;
public interface ProductListingService {
    List<ProductListing> getAll();
    ProductListing getById(Long id);
  
    ProductListing create(ProductListing listing);
    ProductListing update(Long id, ProductListing listing);
    void delete(Long id);
    Page<ProductListing> getActiveListings(String type, String sortBy, int page, int size);
    List<ProductListing> findRandomRelated(String productType, Long excludeProductId, int limit);
    ProductListing updateListingDetails(Long listingId, UpdateListingDTO dto);
    ProductListing markAsSold(Long listingId);
    // Phương thức để thêm ảnh mới vào một listing đã có
    ProductListing addImagesToListing(Long listingId, List<MultipartFile> files);
    
    // Phương thức để xóa một ảnh cụ thể bằng ID của ảnh
    void deleteImageFromListing(Long listingId, Long imageId);
    Page<ProductListing> getByUserId(Long userId, int page, int size);
 
    int findPageForListing(Long userId, Long listingId, int pageSize);


}
