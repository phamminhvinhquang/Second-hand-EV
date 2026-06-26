package local.contract.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;

    // ======= Thông tin khách hàng =======
    private Long userId; // dùng để truy hợp đồng theo người dùng
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String paymentMethod;

    // ======= Thông tin sản phẩm =======
    private String productName;
    private BigDecimal totalPrice;

    // ======= Chữ ký & PDF =======
    @Lob
    private String signature;  // base64 PNG
    private String pdfUrl;     // đường dẫn file PDF lưu trên server

    private LocalDateTime signedAt;  // ngày ký (chỉ set khi user ký thành công)

    // ======= Trạng thái & audit =======
    private String status;           // PENDING, SIGNED, AUTO_CREATED,...
    private LocalDateTime createdAt; // thời điểm tạo hợp đồng
    private LocalDateTime updatedAt; // thời điểm cập nhật gần nhất

    // ======= Tự động gán thời gian khi tạo mới =======
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    // ======= Tự động cập nhật thời gian khi chỉnh sửa =======
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
