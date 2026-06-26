package edu.uth.listingservice.DTO; 

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import com.fasterxml.jackson.annotation.JsonTypeInfo;


import java.util.ArrayList;
import java.util.List;


 

// Báo cho Jackson không cần tìm trường "@class" cho riêng class này,
// ngay cả khi default typing đang được bật.
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)

public class RestPage<T> extends PageImpl<T> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPage(@JsonProperty("content") List<T> content,
                    @JsonProperty("number") int number,
                    @JsonProperty("size") int size,
                    @JsonProperty("totalElements") Long totalElements,
                    @JsonProperty("pageable") JsonNode pageable,
                    @JsonProperty("last") boolean last,
                    @JsonProperty("totalPages") int totalPages,
                    @JsonProperty("sort") JsonNode sort,
                    @JsonProperty("first") boolean first,
                    @JsonProperty("numberOfElements") int numberOfElements,
                    @JsonProperty("empty") boolean empty) {

        // Tạo Pageable tối thiểu từ thông tin 'number' và 'size'
        super(content, PageRequest.of(number, (size == 0 ? 10 : size)), totalElements);
    }

    // Constructor để tạo từ một Page thật
    public RestPage(List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    // Constructor rỗng
    public RestPage() {
        super(new ArrayList<>(), PageRequest.of(0, 10), 0);
    }
}