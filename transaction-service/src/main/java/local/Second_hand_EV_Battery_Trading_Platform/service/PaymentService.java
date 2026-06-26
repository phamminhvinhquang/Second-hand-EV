package local.Second_hand_EV_Battery_Trading_Platform.service;

import java.util.Map;

import local.Second_hand_EV_Battery_Trading_Platform.entity.Payment;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentInfoResponse;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentRequest;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentResponse;

/**
 * Service chịu trách nhiệm xử lý toàn bộ luồng thanh toán:
 *  - Tạo giao dịch mới (VNPay / MoMo / Ví EV)
 *  - Xử lý callback từ cổng thanh toán
 *  - Cập nhật trạng thái thanh toán nội bộ
 *  - Truy vấn thông tin chi tiết giao dịch
 */
public interface PaymentService {

    /**
     * 🧾 Tạo giao dịch thanh toán mới và lưu vào cơ sở dữ liệu.
     * Nếu là giao dịch online (VNPay / MoMo) → trả về URL redirect.
     * Nếu là thanh toán nội bộ (Ví EV) → xử lý trực tiếp và không có redirect URL.
     *
     * @param request Đối tượng PaymentRequest từ frontend (bao gồm cartIds, customer, paymentMethod, type)
     * @return PaymentResponse (chứa trạng thái giao dịch và URL redirect nếu có)
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * 📩 Xử lý callback trả về từ VNPay hoặc MoMo.
     * Dữ liệu có thể đến từ query params (VNPay) hoặc JSON body (MoMo).
     *
     * @param data Dữ liệu callback được parse thành Map
     */
    void handleCallback(Map<String, Object> data);

    /**
     * 🔁 Cập nhật trạng thái thanh toán (chủ yếu dùng cho thanh toán nội bộ — Ví EV).
     *
     * @param transactionId Mã giao dịch cần cập nhật
     * @param newStatus Trạng thái mới (ví dụ: "SUCCESS", "FAILED")
     */
    void updateStatus(String transactionId, String newStatus);

    /**
     * 🔍 Truy vấn chi tiết giao dịch dựa theo transactionId.
     *
     * @param transactionId Mã giao dịch duy nhất (UUID)
     * @return Đối tượng Payment (bao gồm thông tin khách hàng, sản phẩm, phương thức thanh toán)
     */
    Payment findByTransactionId(String transactionId);

    /**
     * 📦 Lấy thông tin chi tiết thanh toán để hiển thị ở frontend.
     * Kết hợp dữ liệu từ bảng Payment, Customer, và các service khác (cart-service).
     *
     * @param transactionId Mã giao dịch duy nhất (UUID)
     * @return PaymentInfoResponse (bao gồm customer info, method, amount, status, v.v.)
     */
    PaymentInfoResponse getPaymentInfo(String transactionId);
}
