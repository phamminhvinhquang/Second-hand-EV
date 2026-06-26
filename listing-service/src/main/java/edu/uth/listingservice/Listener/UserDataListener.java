
package edu.uth.listingservice.Listener;

import edu.uth.listingservice.events.UserDataResponseDTO;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.Config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

@Component
public class UserDataListener {
    
    private static final Logger log = LoggerFactory.getLogger(UserDataListener.class);

    @Autowired
    private ProductListingRepository listingRepository;

    /**
     * Lắng nghe phản hồi từ user-service
     */
    @RabbitListener(queues = RabbitMQConfig.ASYNC_RESPONSE_QUEUE)
    public void handleUserDataResponse(UserDataResponseDTO response) {
        if (response == null || response.getListingId() == null) {
            log.warn("Received null or invalid user data response");
            return;
        }

        Optional<ProductListing> listingOpt = listingRepository.findById(response.getListingId());

        if (listingOpt.isPresent()) {
            ProductListing listing = listingOpt.get();
            
            // Cập nhật thông tin
            listing.setSellerName(response.getName());
            listing.setSellerEmail(response.getEmail());
           
        
            
            listingRepository.save(listing);
            log.info("Successfully updated seller info for listingId: {}", response.getListingId());
        } else {
            log.warn("Received user data for non-existent listingId: {}", response.getListingId());
        }
    }
}