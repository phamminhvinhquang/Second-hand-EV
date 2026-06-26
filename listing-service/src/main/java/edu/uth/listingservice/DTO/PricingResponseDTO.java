package edu.uth.listingservice.DTO;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingResponseDTO {
    private Long suggestedPrice;
}