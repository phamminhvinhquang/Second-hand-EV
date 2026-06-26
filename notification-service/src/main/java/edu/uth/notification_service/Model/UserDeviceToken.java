
package edu.uth.notification_service.Model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint; // <-- THÊM IMPORT
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
//  NÂNG CẤP: Dùng "UniqueConstraint" để đảm bảo 1 user không đăng ký
// cùng 1 token 2 lần.
@Table(name = "user_device_tokens", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "device_token"})
})
@Data
@NoArgsConstructor
public class UserDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //  NÂNG CẤP: Xóa 'unique = true'
    @Column(name = "user_id", nullable = false) 
    private Long userId;

    @Column(name = "device_token", nullable = false, length = 512)
    private String deviceToken;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    public UserDeviceToken(Long userId, String deviceToken) {
        this.userId = userId;
        this.deviceToken = deviceToken;
        this.updatedAt = new Date();
    }
}