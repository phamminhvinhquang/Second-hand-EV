// File: src/main/java/edu/uth/userservice/mq/ListingEventListener.java
package edu.uth.userservice.mq;

import edu.uth.userservice.events.ListingNeedsUserDataDTO;
import edu.uth.userservice.events.UserDataResponseDTO;
import edu.uth.userservice.config.RabbitMQConfig;
import edu.uth.userservice.model.User;
import edu.uth.userservice.service.UserService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ListingEventListener {

    @Autowired
    private UserService userService;

    // Dùng lại MQPublisher (File 2) của bạn để gửi phản hồi
    @Autowired
    private MQPublisher mqPublisher; 

    private static final Logger log = LoggerFactory.getLogger(ListingEventListener.class);

    /**
     * Lắng nghe event bất đồng bộ từ listing-service
     */
    @RabbitListener(queues = RabbitMQConfig.ASYNC_REQUEST_QUEUE)
    public void handleListingNeedsUserData(ListingNeedsUserDataDTO request) {
        if (request == null || request.getUserId() == null) {
            log.warn("Received null or invalid request in handleListingNeedsUserData");
            return;
        }

        log.info("Processing async request for listingId: {}, userId: {}", request.getListingId(), request.getUserId());
        
        Optional<User> userOpt = userService.findById(request.getUserId().intValue());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Tạo DTO phản hồi
            UserDataResponseDTO response = new UserDataResponseDTO(
                request.getListingId(), // Trả lại listingId
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress()
            );

            // Gửi event phản hồi bằng key ASYNC_RESPONSE_KEY
            mqPublisher.publish(RabbitMQConfig.ASYNC_RESPONSE_KEY, response);
            
            log.info("Sent user data response for listingId: {}", request.getListingId());
        } else {
            log.warn("User not found for userId: {}. No response sent.", request.getUserId());
        }
    }
}