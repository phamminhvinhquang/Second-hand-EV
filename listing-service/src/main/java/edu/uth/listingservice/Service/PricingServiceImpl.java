
package edu.uth.listingservice.Service;
import org.springframework.cache.annotation.Cacheable; 

import edu.uth.listingservice.DTO.PricingRequestDTO;
import edu.uth.listingservice.DTO.PricingResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate; 
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference; 

@Service
public class PricingServiceImpl implements PricingService {
    private final Logger logger = LoggerFactory.getLogger(PricingServiceImpl.class);
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.ai.exchange}")
    private String aiExchange;

    @Value("${app.rabbitmq.ai.routing-key}")
    private String aiRoutingKey;

    private static final long MINIMUM_PRICE = 200_000L;
    private static final long FALLBACK_PRICE = 400_000L;


    @Override
   @Cacheable(value = "aiSuggestions", 
           key = "#dto.productType + '-' + #dto.brand + '-' + #dto.conditionId + '-' + #dto.yearOfManufacture + '-' + " +
                 "#dto.mileage + '-' + #dto.batteryCapacity + '-' + #dto.batteryType + '-' + #dto.batteryLifespan + '-' + " +
                 "#dto.compatibleVehicle + '-' + #dto.warrantyPolicy + '-' + #dto.maxSpeed + '-' + #dto.rangePerCharge + '-' + " +
                 "#dto.color + '-' + #dto.chargeTime + '-' + #dto.chargeCycles")
    public PricingResponseDTO getSuggestedPrice(PricingRequestDTO dto) {
        try {
            logger.info("Sending price request to MQ: {}", dto);

            // === Dùng 'convertSendAndReceiveAsType' ===
            // Gửi DTO và báo cho Spring biết kiểu dữ liệu JSON trả về
            PricingResponseDTO response = rabbitTemplate.convertSendAndReceiveAsType(
                aiExchange,
                aiRoutingKey,
                dto,
                new ParameterizedTypeReference<PricingResponseDTO>() {} 
            );
            

            if (response != null && response.getSuggestedPrice() != null) {
                if (response.getSuggestedPrice() < MINIMUM_PRICE) {
                    logger.warn("AI service trả về giá quá thấp ({}). Điều chỉnh về giá tối thiểu.", response.getSuggestedPrice());
                    return new PricingResponseDTO(MINIMUM_PRICE);
                }
                logger.info("Received price from MQ: {}", response.getSuggestedPrice());
                return response;
            } else {
                logger.warn("AI service (MQ) không trả về dữ liệu. Dùng giá tạm.");
                return new PricingResponseDTO(FALLBACK_PRICE);
            }

        } catch (Exception e) {
            // Nếu service Python không chạy, nó sẽ báo lỗi (thường là timeout)
            // Lỗi ClassCastException (LinkedHashMap) cũng sẽ bị bắt ở đây
            logger.error("LỖI khi gọi AI service (MQ): {}", e.getMessage());
            return new PricingResponseDTO(FALLBACK_PRICE); 
        }
    }
}