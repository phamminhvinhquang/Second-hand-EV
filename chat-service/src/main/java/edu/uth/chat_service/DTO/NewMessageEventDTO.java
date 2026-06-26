// File: src/main/java/edu/uth/chat_service/DTO/NewMessageEventDTO.java
package edu.uth.chat_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewMessageEventDTO {
    private Long senderId;
    private String senderName;
    private Long recipientId;
    private String content;
    private Long productId;
}