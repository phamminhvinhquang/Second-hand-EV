
package edu.uth.chat_service.Model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String chatId; 
    private Long senderId;
    private Long recipientId;
    
    @Column(length = 1000)
    private String content;
    
    private String senderName;
    private Long productId;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp = new Date();
    
    private boolean isRead = false;
    private boolean deletedBySender = false;
    private boolean deletedByRecipient = false;

   
    private boolean isRecalled = false;

    private String msgType = "TEXT"; 
}