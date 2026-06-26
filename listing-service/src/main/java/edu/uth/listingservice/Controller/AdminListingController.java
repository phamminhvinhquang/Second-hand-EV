

package edu.uth.listingservice.Controller;

import edu.uth.listingservice.DTO.AdminRejectDTO;
import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Service.AdminListingService; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/admin/listings") // URL base cho các API của admin
public class AdminListingController {

    @Autowired
    private AdminListingService adminListingService;

  //   Xóa page, size và dùng Pageable
    @GetMapping
    public Page<ProductListing> getListingsByStatus(
            @RequestParam(defaultValue = "PENDING") String status,
            Pageable pageable // Spring sẽ tự động tạo đối tượng này từ URL
    ) {
        ListingStatus listingStatus = ListingStatus.valueOf(status.toUpperCase());
        // Truyền Pageable (đã bao gồm thông tin sort từ frontend) xuống service
        return adminListingService.getListingsByStatus(listingStatus, pageable);
    }

    // 2. API để duyệt (approve) một tin đăng
    @PostMapping("/{id}/approve")
    public ResponseEntity<ProductListing> approveListing(@PathVariable Long id) {
        ProductListing approvedListing = adminListingService.approveListing(id);
        return ResponseEntity.ok(approvedListing);
    }

    // 3. API để từ chối (reject) một tin đăng
    @PostMapping("/{id}/reject")
    public ResponseEntity<ProductListing> rejectListing(@PathVariable Long id, @RequestBody AdminRejectDTO payload) {
        ProductListing rejectedListing = adminListingService.rejectListing(id, payload.getReason());
        return ResponseEntity.ok(rejectedListing);
    }

    // 4. API để gắn nhãn "Đã kiểm định"
    @PostMapping("/{id}/verify")
    public ResponseEntity<ProductListing> verifyListing(@PathVariable Long id) {
        ProductListing verifiedListing = adminListingService.verifyListing(id);
        return ResponseEntity.ok(verifiedListing);
    }
    //  THAY ĐỔI: Xóa page, size và dùng Pageable
    @GetMapping("/search")
    public Page<ProductListing> searchListings(
            @RequestParam String query,
            Pageable pageable
    ) {
        return adminListingService.searchListings(query, pageable);
    }
}