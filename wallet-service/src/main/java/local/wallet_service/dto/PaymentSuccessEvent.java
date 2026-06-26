package local.wallet_service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ✅ PaymentSuccessEvent
 * Dùng để truyền thông tin giao dịch thành công giữa transaction-service ↔ wallet-service qua RabbitMQ.
 * Bản chuẩn hóa 1:1 với bên transaction-service để tránh lỗi deserialization.
 */
public class PaymentSuccessEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Mã giao dịch duy nhất */
    private String transactionId;

    /** ID người bán */
    private Long sellerId;

    /** ID sản phẩm (để phân biệt nhiều item cùng seller) */
    private Long productId;   // 🆕 Thêm dòng này

    /** Số tiền thanh toán */
    private BigDecimal price;

    /** Phương thức thanh toán: MOMO / VNPAY / EVWALLET */
    private String method;

    /** ID người mua (người thực hiện thanh toán) */
    private Long userId;

    /** Trạng thái giao dịch: SUCCESS / FAILED */
    private String status;

    /** Loại giao dịch: order / deposit / withdraw */
    private String type;

    // ========================= CONSTRUCTORS =========================

    public PaymentSuccessEvent() {
    }

    public PaymentSuccessEvent(String transactionId, Long sellerId, Long productId, BigDecimal price,
                               String method, Long userId, String status, String type) {
        this.transactionId = transactionId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.price = price;
        this.method = method;
        this.userId = userId;
        this.status = status;
        this.type = type;
    }

    // ========================= GETTERS / SETTERS =========================

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getProductId() { return productId; }          // 🆕
    public void setProductId(Long productId) { this.productId = productId; } // 🆕

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // ========================= DEBUG LOG =========================

    @Override
    public String toString() {
        return "PaymentSuccessEvent{" +
                "transactionId='" + transactionId + '\'' +
                ", sellerId=" + sellerId +
                ", productId=" + productId +
                ", price=" + price +
                ", method='" + method + '\'' +
                ", userId=" + userId +
                ", status='" + status + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
