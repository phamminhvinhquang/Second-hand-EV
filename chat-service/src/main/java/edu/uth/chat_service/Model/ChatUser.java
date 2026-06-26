package edu.uth.chat_service.Model;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatUser {
    @Id
    private Long id; 
    
    private String fullName;
    private String email;
    private String phone;

 
    private boolean isOnline = false;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActiveAt = new Date();
}