// File: src/main/java/edu/uth/chat_service/Model/BlockUser.java
package edu.uth.chat_service.Model;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "block_users")
@Data
@NoArgsConstructor
public class BlockUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long blockerId; // Người chặn
    private Long blockedId; // Người bị chặn
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    public BlockUser(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }
}