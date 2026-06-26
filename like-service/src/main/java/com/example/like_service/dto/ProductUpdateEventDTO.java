package com.example.like_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Setter @NoArgsConstructor
public class ProductUpdateEventDTO {
    private Long productId;

    // new values (producer nên gửi ít nhất những field thay đổi)
    private Long newPrice;
    private Long oldPrice;
    private String newConditionName;
    private String oldConditionName;

    // optional: human friendly title/body
    private String title;
    private String message;
}
