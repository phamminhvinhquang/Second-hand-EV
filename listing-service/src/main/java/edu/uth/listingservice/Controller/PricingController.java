package edu.uth.listingservice.Controller;

import edu.uth.listingservice.DTO.PricingRequestDTO;
import edu.uth.listingservice.DTO.PricingResponseDTO;
import edu.uth.listingservice.Service.PricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    @Autowired
    private PricingService pricingService;

    @PostMapping("/suggest")
    public ResponseEntity<PricingResponseDTO> suggestPrice(@RequestBody PricingRequestDTO dto) {
        if (dto.getProductType() == null || dto.getConditionId() == null || dto.getYearOfManufacture() == null) {
            // Trả về lỗi nếu thiếu các trường tối thiểu
            return ResponseEntity.badRequest().build();
        }
        PricingResponseDTO response = pricingService.getSuggestedPrice(dto);
        return ResponseEntity.ok(response);
    }
}