package edu.uth.listingservice.Controller;
// Thêm import
import org.springframework.data.domain.Page;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Service.ProductListingService;
import edu.uth.listingservice.DTO.UpdateListingDTO;
@RestController
@RequestMapping("/api/listings")

public class ProductListingController {

    @Autowired
    private ProductListingService listingService;

   
    @GetMapping
    public Page<ProductListing> getFilteredListings(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return listingService.getActiveListings(type, sortBy, page, size);
    }
    
    @GetMapping("/related")
    public List<ProductListing> getRelatedListings(
            @RequestParam String type,
            @RequestParam Long excludeId,
            @RequestParam(defaultValue = "6") int limit) {
        return listingService.findRandomRelated(type, excludeId, limit);
    }
  

    @GetMapping("/{id}")
    public ProductListing getById(@PathVariable Long id) {
        return listingService.getById(id);
    }

 

    @PostMapping
    public ProductListing create(@RequestBody ProductListing listing) {
        return listingService.create(listing);
    }

    @PutMapping("/{id}")
    public ProductListing update(@PathVariable Long id, @RequestBody ProductListing listing) {
        return listingService.update(id, listing);
    }
   @PutMapping("/{id}/update-details")
    public ProductListing updateDetails(@PathVariable Long id, @RequestBody UpdateListingDTO dto) {
        return listingService.updateListingDetails(id, dto);
    }

@PostMapping("/{listingId}/add-images")
public ResponseEntity<ProductListing> addImages(
        @PathVariable Long listingId,
        @RequestParam("images") List<MultipartFile> files) {
            
    if (files == null || files.isEmpty() || files.get(0).isEmpty()) {
        return ResponseEntity.badRequest().build();
    }
    
    ProductListing updatedListing = listingService.addImagesToListing(listingId, files);
    return ResponseEntity.ok(updatedListing);
}




@PostMapping("/{listingId}/delete-image/{imageId}")
public ResponseEntity<Void> deleteImage(
        @PathVariable Long listingId,
        @PathVariable Long imageId) {
            
    listingService.deleteImageFromListing(listingId, imageId);
    return ResponseEntity.noContent().build(); // Trả về 204 No Content khi thành công
}
    @PutMapping("/{id}/mark-as-sold")
    public ProductListing markAsSold(@PathVariable Long id) {
        return listingService.markAsSold(id);
    }

@GetMapping("/user/{userId}")
public Page<ProductListing> getByUserId(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size) {
    return listingService.getByUserId(userId, page, size);
}


@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
    listingService.delete(id);
    return ResponseEntity.noContent().build();
}

    @GetMapping("/user/{userId}/find-page")
    public int getPageForListing(
            @PathVariable Long userId,
            @RequestParam Long listingId,
            @RequestParam(defaultValue = "12") int size) {
        return listingService.findPageForListing(userId, listingId, size);
    }
}
