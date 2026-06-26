
package edu.uth.listingservice.DTO;

import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;

import java.util.Date;

// DTO này được "san phẳng" để gửi qua WebSocket,
// giải quyết triệt để vấn đề Lazy Loading của 'product.productName'
public class AdminListingUpdateDTO {

    private Long listingId;
    private String productName;
    private Long userId;
    private Date listingDate;
    private ListingStatus listingStatus;
    private boolean verified;
    private String adminNotes;


    public AdminListingUpdateDTO() {
    }

    // Constructor để chuyển đổi từ Entity sang DTO
    public AdminListingUpdateDTO(ProductListing listing) {
        this.listingId = listing.getListingId();
        
        // Đây là mấu chốt: 
        // Chúng ta truy cập getProduct().getProductName() NGAY LẬP TỨC
        // khi đang ở trong Transaction, buộc nó phải tải dữ liệu
        if (listing.getProduct() != null) {
            this.productName = listing.getProduct().getProductName();
        } else {
            this.productName = "[Không có tiêu đề]"; 
        }
        
        this.userId = listing.getUserId();
        this.listingDate = listing.getListingDate();
        this.listingStatus = listing.getListingStatus();
        this.verified = listing.isVerified();
        this.adminNotes = listing.getAdminNotes();
    }


    public Long getListingId() { return listingId; }
    public void setListingId(Long listingId) { this.listingId = listingId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Date getListingDate() { return listingDate; }
    public void setListingDate(Date listingDate) { this.listingDate = listingDate; }

    public ListingStatus getListingStatus() { return listingStatus; }
    public void setListingStatus(ListingStatus listingStatus) { this.listingStatus = listingStatus; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}