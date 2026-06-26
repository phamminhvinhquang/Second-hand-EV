package edu.uth.listingservice.Service;

import edu.uth.listingservice.DTO.PricingRequestDTO;
import edu.uth.listingservice.DTO.PricingResponseDTO;

public interface PricingService {
    PricingResponseDTO getSuggestedPrice(PricingRequestDTO dto);
}