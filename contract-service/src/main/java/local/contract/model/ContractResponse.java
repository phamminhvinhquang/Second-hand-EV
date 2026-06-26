package local.contract.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi cho hợp đồng — dùng được cả khi ký thành công hoặc khi xem lịch sử hợp đồng.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponse {

    // 🆔 ID hợp đồng (để frontend hiển thị danh sách)
    private Long id;

    // 🔁 Mã giao dịch liên kết
    private String transactionId;

    // 👤 Thông tin khách hàng (có thể rút gọn hoặc bỏ nếu không cần)
    private Long userId;
    private String customerName;

    // 📦 Thông tin sản phẩm
    private String productName;
    private BigDecimal totalPrice;

    // 📄 Link tới file hợp đồng PDF
    private String pdfUrl;

    // 🕒 Thời gian ký hợp đồng
    private String signedAt;

    // 💬 Thông điệp phản hồi (chỉ dùng khi ký xong)
    private String message;
}
