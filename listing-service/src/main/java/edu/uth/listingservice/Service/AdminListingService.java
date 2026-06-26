
package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.ProductListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; 
public interface AdminListingService {

    Page<ProductListing> getListingsByStatus(ListingStatus status, Pageable pageable);
    ProductListing approveListing(Long listingId);
    ProductListing rejectListing(Long listingId, String reason);
    ProductListing verifyListing(Long listingId);

    Page<ProductListing> searchListings(String query, Pageable pageable);
}