package local.Second_hand_EV_Battery_Trading_Platform.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentInfoResponse;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentRequest;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentResponse;
import local.Second_hand_EV_Battery_Trading_Platform.service.PaymentService;
import local.Second_hand_EV_Battery_Trading_Platform.utils.VNPayUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;

    // ============================================================
    // ✅ 1️⃣ TẠO GIAO DỊCH (ORDER / DEPOSIT / EVWALLET)
    // ============================================================
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody PaymentRequest request) {
        log.info("🧾 [CREATE] Nhận yêu cầu tạo thanh toán: {}", request);

        if (request == null || request.getPaymentMethod() == null || request.getType() == null) {
            return ResponseEntity.badRequest().body("❌ Thiếu thông tin bắt buộc trong PaymentRequest!");
        }

        PaymentResponse response = paymentService.createPayment(request);
        log.info("✅ [PAYMENT] Tạo giao dịch thành công: {}", response);

        // 🔹 Nếu là EVWallet thì redirect trực tiếp sang trang thành công
        if ("evwallet".equalsIgnoreCase(request.getPaymentMethod())) {
            return ResponseEntity.ok(Map.of(
                    "transactionId", response.getTransactionId(),
                    "status", "SUCCESS",
                    "redirectUrl", "http://localhost:9000/payment_success.html?transactionId=" + response.getTransactionId()
            ));
        }

        // 🔹 Còn lại (MoMo, VNPay) thì trả URL thanh toán
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // ✅ 2️⃣ CALLBACK TỪ VNPAY / MOMO
    // ============================================================
    @RequestMapping(value = "/callback", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> handleCallback(
            @RequestParam(required = false) Map<String, String> params,
            @RequestBody(required = false) String rawBody) {

        Map<String, Object> data = new HashMap<>();
        String transactionId = null;
        String method = "UNKNOWN";

        try {
            // Ưu tiên JSON (MoMo IPN), fallback query (VNPay)
            if (rawBody != null && !rawBody.isEmpty()) {
                data.putAll(mapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {}));
            } else if (params != null && !params.isEmpty()) {
                data.putAll(params);
            }

            log.info("📩 [CALLBACK] Nhận dữ liệu callback: {}", data);

            // Nhận diện cổng
            if (data.containsKey("vnp_TxnRef")) {
                transactionId = String.valueOf(data.get("vnp_TxnRef"));
                method = "VNPAY";
            } else if (data.containsKey("orderId") || data.containsKey("orderid")) {
                transactionId = String.valueOf(data.getOrDefault("orderId", data.get("orderid")));
                method = "MOMO";
            }

            if (transactionId == null || transactionId.isEmpty())
                throw new IllegalArgumentException("Không tìm thấy transactionId trong callback!");

            // --- VNPay verify chữ ký ---
            if ("VNPAY".equalsIgnoreCase(method)) {
                String receivedHash = (String) data.get("vnp_SecureHash");
                if (receivedHash == null)
                    return redirectFail("missing_signature");

                Map<String, String> vnpParams = new HashMap<>();
                data.forEach((k, v) -> {
                    if (k.toLowerCase().startsWith("vnp_")
                            && !"vnp_securehash".equalsIgnoreCase(k)
                            && !"vnp_securehashtype".equalsIgnoreCase(k)) {
                        vnpParams.put(k, v.toString());
                    }
                });
                String recalculated = VNPayUtils.hmacSHA512(vnp_HashSecret, VNPayUtils.hashAllFields(vnpParams));
                if (!recalculated.equalsIgnoreCase(receivedHash))
                    return redirectFail("invalid_signature");
            }

            // --- Cập nhật DB + publish event tự động trong service ---
            paymentService.handleCallback(data);

            // --- Xác định kết quả từ gateway ---
            boolean isSuccess = false;
            boolean isCanceled = false;

            if ("MOMO".equalsIgnoreCase(method)) {
                String rc = String.valueOf(data.getOrDefault("resultCode", "-1"));
                isSuccess = "0".equals(rc);
                isCanceled = "1006".equals(rc); // người dùng hủy
            } else if ("VNPAY".equalsIgnoreCase(method)) {
                String rc = String.valueOf(data.getOrDefault("vnp_ResponseCode", "99"));
                isSuccess = "00".equals(rc);
                isCanceled = "24".equals(rc);   // người dùng hủy
            }

            // --- Lấy thông tin từ DB sau khi xử lý ---
            PaymentInfoResponse info = paymentService.getPaymentInfo(transactionId);
            String type = info.getType() != null ? info.getType().toLowerCase() : "order";

            // Không dựa vào info.getStatus() để chọn trang, chỉ hiển thị
            String finalStatus = isSuccess ? "SUCCESS" : (isCanceled ? "CANCELED" : "FAILED");

            // --- Xác định trang redirect ---
            String redirectUrl;
            boolean isDeposit = "deposit".equalsIgnoreCase(type);

            if (isDeposit) {
                // NẠP TIỀN (DEPOSIT): thành công -> deposit_success, thất bại/hủy -> payment_fail
                redirectUrl = isSuccess
                        ? "http://localhost:9000/deposit_success.html"
                        : "http://localhost:9000/payment_fail.html";
            } else {
                // ORDER / EVWALLET: luôn về payment_success.html
                // để trang hiển thị SUCCESS/FAILED/CANCELED theo finalStatus
                redirectUrl = "http://localhost:9000/payment_success.html";
            }

            // --- Redirect kèm query ---
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location",
                    redirectUrl
                            + "?transactionId=" + URLEncoder.encode(transactionId, StandardCharsets.UTF_8)
                            + "&method=" + URLEncoder.encode(method, StandardCharsets.UTF_8)
                            + "&status=" + URLEncoder.encode(finalStatus, StandardCharsets.UTF_8));

            log.info("🎯 [REDIRECT] {} → {} ({})", transactionId, redirectUrl, finalStatus);
            return ResponseEntity.status(302).headers(headers).build();



        } catch (Exception e) {
            log.error("❌ [CALLBACK] Lỗi xử lý callback", e);
            return redirectFail(e.getMessage());
        }
    }

    private ResponseEntity<?> redirectFail(String error) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "http://localhost:9000/payment_fail.html?error=" +
                URLEncoder.encode(error, StandardCharsets.UTF_8));
        return ResponseEntity.status(302).headers(headers).build();
    }

    // ============================================================
    // ✅ 3️⃣ LẤY THÔNG TIN THANH TOÁN
    // ============================================================
    @GetMapping("/info/{transactionId}")
    public ResponseEntity<?> getPaymentInfo(@PathVariable String transactionId) {
        try {
            PaymentInfoResponse info = paymentService.getPaymentInfo(transactionId);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
