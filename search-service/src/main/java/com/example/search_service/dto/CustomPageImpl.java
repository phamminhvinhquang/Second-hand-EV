package com.example.search_service.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

// Lớp này dùng để giải quyết lỗi deserialize PageImpl từ Spring Data JPA
public class CustomPageImpl<T> extends PageImpl<T> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CustomPageImpl(@JsonProperty("content") List<T> content,
                          @JsonProperty("number") int number,
                          @JsonProperty("size") int size,
                          @JsonProperty("totalElements") Long totalElements,
                          @JsonProperty("pageable") JsonNode pageable, // Ignore complex Pageable object
                          @JsonProperty("last") boolean last,
                          @JsonProperty("totalPages") int totalPages,
                          @JsonProperty("sort") JsonNode sort, // Ignore complex Sort object
                          @JsonProperty("first") boolean first,
                          @JsonProperty("numberOfElements") int numberOfElements) {
        
        super(content, PageRequest.of(number, size), totalElements);
    }

    public CustomPageImpl(List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public CustomPageImpl(List<T> content) {
        super(content);
    }
}