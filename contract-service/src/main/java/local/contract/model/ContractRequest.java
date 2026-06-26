package local.contract.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng khi frontend gửi yêu cầu ký hợp đồng hoặc tạo hợp đồng tự động.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractRequest {

    // 🔁 Mã giao dịch từ transaction-service
    private String transactionId;

    // 📊 Trạng thái hợp đồng hoặc thanh toán (PENDING, SUCCESS, FAILED)
    private String status;

    // 💳 Phương thức thanh toán (MOMO, VNPAY, BANK, ...)
    private String method;

    // 👤 Thông tin khách hàng
    private String fullName;
    private String phone;
    private String email;
    private String address;

    // ✍️ Chữ ký điện tử (base64 PNG)
    private String signature;

    // 🧾 Dữ liệu PDF hợp đồng (base64 từ frontend gửi lên)
    private String pdfBase64;

    // 🧩 ID người mua và người bán
    private Long userId;     // người mua
    private Long sellerId;   // người bán

    // 📦 Thông tin sản phẩm
    private String productName;
    private Double totalAmount;
}
