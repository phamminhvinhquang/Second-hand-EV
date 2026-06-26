// File: local/Second_hand_EV_Battery_Trading_Platform/service/PaymentServiceImpl.java
package local.Second_hand_EV_Battery_Trading_Platform.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections; // ⭐️ Đảm bảo import này
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import local.Second_hand_EV_Battery_Trading_Platform.entity.Customer;
import local.Second_hand_EV_Battery_Trading_Platform.entity.Payment;
import local.Second_hand_EV_Battery_Trading_Platform.model.CustomerDTO;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentInfoResponse;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentRequest;
import local.Second_hand_EV_Battery_Trading_Platform.model.PaymentResponse;
import local.Second_hand_EV_Battery_Trading_Platform.mq.CartResponseListener;
import local.Second_hand_EV_Battery_Trading_Platform.mq.MQPublisher;
import local.Second_hand_EV_Battery_Trading_Platform.repository.CustomerRepository;
import local.Second_hand_EV_Battery_Trading_Platform.repository.PaymentRepository;
import local.Second_hand_EV_Battery_Trading_Platform.utils.VNPayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepo;
    private final CustomerRepository customerRepo;
    private final MQPublisher mqPublisher;
    private final CartResponseListener cartResponseListener;

    // ... (Tất cả các @Value config của bạn giữ nguyên) ...
    // ===== VNPay Config =====
    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnp_Url;
    @Value("${vnpay.returnUrl:http://localhost:9000/api/payments/callback}")
    private String vnp_ReturnUrl;
    @Value("${vnpay.tmnCode:3DWSNIT9}")
    private String vnp_TmnCode;
    @Value("${vnpay.hashSecret:99W5Z4HEK24U9ONIE4BYMU6GWT6TBS7B}")
    private String vnp_HashSecret;
    // ===== MoMo Config =====
    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;
    @Value("${momo.partnerCode:MOMOQTST20251020_TEST}")
    private String momoPartnerCode;
    @Value("${momo.accessKey:Cy4ZAIlh0TwMJtMP}")
    private String momoAccessKey;
    @Value("${momo.secretKey:eSAqVmVyvDwzcj2uZxkwRjAdz3nrtNpo}")
    private String momoSecretKey;
    // Redirect URLs
    @Value("${momo.returnUrl.deposit:http://localhost:9000/deposit_success.html}")
    private String momoReturnUrlDeposit;
    @Value("${momo.returnUrl.order:http://localhost:9000/payment_success.html}")
    private String momoReturnUrlOrder;
    @Value("${momo.notifyUrl:http://localhost:9000/api/payments/callback}")
    private String momoNotifyUrl;
    @Value("${momo.requestType:captureWallet}")
    private String momoRequestType;
    // Wallet service URL
    @Value("${wallet.api.url:http://localhost:9000/api/wallet/pay}")
    private String walletApiUrl;


    // ============================================================
    // ======================= TẠO GIAO DỊCH ======================
    // ============================================================
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("🔹 [API] Nhận PaymentRequest: {}", request);

        if (request == null || request.getPaymentMethod() == null)
            throw new RuntimeException("Thiếu dữ liệu PaymentRequest");

        // ==================== 🟢 1️⃣ NẠP TIỀN ====================
        if ("deposit".equalsIgnoreCase(request.getType())) {
            // ... (Logic nạp tiền của bạn giữ nguyên, không thay đổi) ...
            Payment p = Payment.builder()
                    .transactionId(UUID.randomUUID().toString())
                    .userId(request.getUserId()) // 🟢 Thêm dòng này
                    .amount(request.getAmount())
                    .totalAmount(request.getAmount())
                    .method(request.getPaymentMethod().toUpperCase())
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .productNames("Nạp tiền vào ví người dùng #" + request.getUserId())
                    .cartIdList(Collections.emptyList())
                    .build();
            paymentRepo.save(p);

            String redirectUrl = switch (request.getPaymentMethod().toLowerCase()) {
                case "vnpay" -> buildVNPayUrl(p);
                case "momo" -> buildMoMoUrl(p);
                case "evwallet" -> null;
                default -> throw new RuntimeException("Phương thức thanh toán không hợp lệ!");
            };

            if ("evwallet".equalsIgnoreCase(request.getPaymentMethod())) {
                p.setStatus("SUCCESS");
                p.setUpdatedAt(LocalDateTime.now());
                paymentRepo.save(p);

                Map<String, Object> event = Map.of(
                        "transactionId", p.getTransactionId(),
                        "status", "SUCCESS",
                        "method", "EVWALLET",
                        "amount", p.getAmount(),
                        "type", "deposit",
                        "userId", request.getUserId(),
                        "time", LocalDateTime.now().toString()
                );
                mqPublisher.publish("wallet.deposit.success", event);
                log.info("💳 [EV Wallet] Đã publish event nạp tiền thành công: {}", event);
            }

            return new PaymentResponse(p.getStatus(),
                    "Thanh toán qua " + request.getPaymentMethod(),
                    p.getTransactionId(),
                    redirectUrl);
        }

        // ==================== 🟣 2️⃣ MUA HÀNG ====================
        if (request.getCustomer() == null)
            throw new RuntimeException("Thiếu thông tin khách hàng!");
        if (request.getCartIds() == null || request.getCartIds().isEmpty())
            throw new RuntimeException("Danh sách cart trống!");

        CustomerDTO dto = request.getCustomer();
        Customer customer = customerRepo.save(Customer.builder()
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .build());

        // ========== 📨 LẤY CHI TIẾT GIỎ HÀNG TỪ CART-SERVICE ==========
        List<Map<String, Object>> items = fetchCartItems(request.getCartIds(), "createPayment-userId:" + request.getUserId());
        if (items.isEmpty()) {
            throw new RuntimeException("❌ Không nhận được phản hồi giỏ hàng hoặc giỏ hàng trống");
        }
        
        // ========== 💰 TÍNH TỔNG TIỀN ==========
        double totalPrice = 0d;
        StringBuilder productList = new StringBuilder();
        for (Map<String, Object> item : items) {
            double price = ((Number) item.get("price")).doubleValue();
            totalPrice += price;
            productList.append(item.get("productName")).append(", ");
        }

        String products = productList.length() > 0
                ? productList.substring(0, productList.length() - 2)
                : "Không có sản phẩm";

        Payment p = Payment.builder()
                .customer(customer)
                .cartIdList(request.getCartIds())
                .userId(request.getUserId()) 
                .amount(BigDecimal.valueOf(totalPrice))
                .totalAmount(BigDecimal.valueOf(totalPrice))
                .productNames(products)
                .method(request.getPaymentMethod().toUpperCase())
                .status("PENDING")
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        paymentRepo.save(p);

        String redirectUrl = switch (request.getPaymentMethod().toLowerCase()) {
            case "vnpay" -> buildVNPayUrl(p);
            case "momo" -> buildMoMoUrl(p);
            case "evwallet" -> null;
            default -> throw new RuntimeException("Phương thức thanh toán không hợp lệ!");
        };

        // ✅ Xử lý thanh toán bằng ví EV
        if ("evwallet".equalsIgnoreCase(request.getPaymentMethod())) {
        // ✅ Kiểm tra số dư ví
        if (!hasEnoughBalance(request.getUserId(), p.getAmount())) {
            p.setStatus("FAILED");
            p.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(p);
            log.warn("❌ [EV Wallet] userId={} không đủ tiền thanh toán {}đ", request.getUserId(), p.getAmount());
            return new PaymentResponse("FAILED", "Số dư ví không đủ", p.getTransactionId(), null);
        }

        // ✅ Nếu đủ tiền, tiếp tục trừ và gửi event
        p.setStatus("SUCCESS");
        p.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(p);

        Map<String, Object> totalEvent = new HashMap<>();
        totalEvent.put("transactionId", p.getTransactionId());
        totalEvent.put("status", "SUCCESS");
        totalEvent.put("method", "EVWALLET");
        totalEvent.put("price", p.getAmount());
        totalEvent.put("type", "order-total");
        totalEvent.put("userId", request.getUserId());
        totalEvent.put("time", LocalDateTime.now().toString());
        mqPublisher.publish("wallet.order.debit", totalEvent);
        log.info("💳 [EV Wallet] Đã publish event trừ ví: {}", totalEvent);

        publishOrderSuccessEvents(
                p.getTransactionId(),
                "EVWALLET",
                request.getUserId(),
                items,
                request.getCartIds()
        );
    }


        return new PaymentResponse(p.getStatus(),
                "Thanh toán qua " + request.getPaymentMethod(),
                p.getTransactionId(),
                redirectUrl);
    }

    // ============================================================
    // ======================= CALLBACK ===========================
    // ============================================================
    @Override
    public void handleCallback(Map<String, Object> data) {
        log.info("📬 [CALLBACK] Dữ liệu nhận từ gateway: {}", data);

        final String transactionId;
        final String method;

        // ... (Logic nhận diện transactionId và method giữ nguyên) ...
        if (data.containsKey("vnp_TxnRef")) {
            transactionId = String.valueOf(data.get("vnp_TxnRef"));
            method = "VNPAY";
        } else if (data.containsKey("orderId") || data.containsKey("orderid")) {
            transactionId = String.valueOf(data.getOrDefault("orderId", data.get("orderid")));
            method = "MOMO";
        } else {
            throw new RuntimeException("Không tìm thấy transactionId trong callback!");
        }

        Payment p = paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment " + transactionId));

        boolean success = false;
        boolean canceled = false;

        // ===================== 🔹 VNPay =====================
        if ("VNPAY".equalsIgnoreCase(method)) {
            // ... (Logic xác thực chữ ký VNPay giữ nguyên) ...
            try {
                String receivedHash = String.valueOf(data.get("vnp_SecureHash"));
                Map<String, String> vnpParams = new TreeMap<>();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equalsIgnoreCase("vnp_SecureHash") && !key.equalsIgnoreCase("vnp_SecureHashType")) {
                        vnpParams.put(key, String.valueOf(entry.getValue()));
                    }
                }
                String recalculated = VNPayUtils.hmacSHA512(vnp_HashSecret, VNPayUtils.hashAllFields(vnpParams));
                if (!recalculated.equalsIgnoreCase(receivedHash)) {
                    log.error("❌ [VNPAY] Sai chữ ký cho transaction {}", transactionId);
                    p.setStatus("FAILED");
                    paymentRepo.save(p);
                    return;
                }

                String code = String.valueOf(data.getOrDefault("vnp_ResponseCode", "99"));
                if ("00".equals(code)) success = true;
                else if ("24".equals(code)) canceled = true;

            } catch (Exception e) {
                log.error("⚠️ [VNPAY] Lỗi callback: {}", e.getMessage());
            }
        }

        // ===================== 🔹 MoMo =====================
        else if ("MOMO".equalsIgnoreCase(method)) {
            // ... (Logic check resultCode MoMo giữ nguyên) ...
            try {
                int resultCode = Integer.parseInt(String.valueOf(data.getOrDefault("resultCode", "-1")));
                success = (resultCode == 0);
                canceled = (resultCode == 1006);
                log.info("💬 [MOMO] resultCode={} → success={}, canceled={}", resultCode, success, canceled);
            } catch (Exception e) {
                log.error("⚠️ [MOMO] Lỗi callback: {}", e.getMessage());
            }
        }

        boolean isDeposit = (p.getCartIdList() == null || p.getCartIdList().isEmpty());
        Long userId = (p.getUserId() != null) ? p.getUserId() : extractUserIdFromProductName(p.getProductNames());

        // =============== 🟢 THÀNH CÔNG ===============
        if (success) {
            p.setStatus("SUCCESS");
            p.setUpdatedAt(LocalDateTime.now());
            p.setMethod(method);
            paymentRepo.save(p);

            Map<String, Object> event = new HashMap<>();
            event.put("transactionId", transactionId);
            event.put("status", "SUCCESS");
            event.put("method", method);
            event.put("amount", p.getAmount());
            event.put("userId", userId);
            event.put("time", LocalDateTime.now().toString());

            // ⭐️⭐️⭐️ [SỬA ĐỔI QUAN TRỌNG] ⭐️⭐️⭐️
            if (isDeposit) {
                event.put("price", p.getAmount());
                event.put("type", "deposit");
                mqPublisher.publish("wallet.deposit.success", event);
                log.info("✅ [Callback] Nạp tiền thành công từ {} → wallet.deposit.success", method);
            } else {
                // 🔹 Gửi sự kiện chi tiết cho từng item (VNPay & MoMo)
                
                // 1. Lấy lại chi tiết items từ cart-service
                List<Map<String, Object>> items = fetchCartItems(p.getCartIdList(), transactionId);

                if (items.isEmpty()) {
                    log.error("❌ [Callback] Đã thanh toán thành công ({}) nhưng KHÔNG thể lấy chi tiết items từ cart-service cho TxID: {}", method, transactionId);
                } else {
                    // 2. Gửi sự kiện (cho Cart, Listing, Review)
                    publishOrderSuccessEvents(
                        transactionId,
                        method,
                        userId, // ID người mua
                        items,   // Danh sách item vừa lấy
                        p.getCartIdList()
                    );
                }
            }
            return;
        }
        // ⭐️⭐️⭐️ [KẾT THÚC SỬA ĐỔI] ⭐️⭐️⭐️


        // =============== ⚠️ HỦY GIAO DỊCH ===============
        if (canceled) {
            // ... (Logic Hủy giao dịch giữ nguyên) ...
            p.setStatus("CANCELED");
            p.setUpdatedAt(LocalDateTime.now());
            p.setMethod(method);
            paymentRepo.save(p);
            log.warn("⚠️ [Callback] Người dùng hủy giao dịch {} ({})", transactionId, method);
            return;
        }

        // =============== ❌ THẤT BẠI ===============
        // ... (Logic Thất bại giữ nguyên) ...
        p.setStatus("FAILED");
        p.setUpdatedAt(LocalDateTime.now());
        p.setMethod(method);
        paymentRepo.save(p);
        Map<String, Object> failEvent = Map.of(
            "transactionId", transactionId,
            "status", "FAILED",
            "method", method,
            "type", isDeposit ? "deposit" : "order",
            "userId", userId,
            "time", LocalDateTime.now().toString()
        );
        String queue = isDeposit ? "wallet.deposit.failed" : "order.failed";
        mqPublisher.publish(queue, failEvent);
        log.warn("❌ [Callback] Giao dịch thất bại từ {} → gửi event {}", method, queue);
    }



    // ============================================================
    // =================== HÀM PHỤ TRỢ ============================
    // ============================================================

    // ... (Hàm extractUserIdFromProductName giữ nguyên) ...
    private Long extractUserIdFromProductName(String productNames) {
        try {
            if (productNames != null && productNames.contains("#")) {
                String num = productNames.substring(productNames.indexOf("#") + 1).trim();
                return Long.parseLong(num);
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ... (Hàm buildVNPayUrl giữ nguyên) ...
    private String buildVNPayUrl(Payment payment) {
        try {
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue()));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", payment.getTransactionId());
            String orderInfo = payment.getProductNames()
                    .replaceAll("[^\\p{ASCII}]", "") // bỏ dấu tiếng Việt
                    .replaceAll("[^a-zA-Z0-9\\s,]", ""); // bỏ ký tự đặc biệt
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");
            vnp_Params.put("vnp_CreateDate", 
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder query = new StringBuilder();
            StringBuilder hashData = new StringBuilder();
            boolean first = true;
            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (!first) {
                        query.append('&');
                        hashData.append('&');
                    }
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                    first = false;
                }
            }
            String secureHash = VNPayUtils.hmacSHA512(vnp_HashSecret, hashData.toString());
            query.append("&vnp_SecureHash=").append(secureHash);
            return vnp_Url + "?" + query.toString();
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi tạo URL VNPay: " + e.getMessage(), e);
        }
    }

    // ... (Hàm buildMoMoUrl giữ nguyên) ...
    private String buildMoMoUrl(Payment payment) {
        try {
            String orderId = payment.getTransactionId();
            String requestId = orderId;
            long amount = payment.getAmount() != null ? payment.getAmount().longValue() : 0L;
            String extraData = Base64.getEncoder().encodeToString("SecondHandEV".getBytes(StandardCharsets.UTF_8));
            String orderInfo = payment.getProductNames();
            String redirectUrl = "http://localhost:9000/api/payments/callback"; // redirect MoMo về backend
            JSONObject body = new JSONObject();
            body.put("partnerCode", momoPartnerCode);
            body.put("accessKey", momoAccessKey);
            body.put("requestId", requestId);
            body.put("amount", amount);
            body.put("orderId", orderId);
            body.put("orderInfo", orderInfo);
            body.put("redirectUrl", redirectUrl);
            body.put("ipnUrl", momoNotifyUrl);
            body.put("extraData", extraData);
            body.put("requestType", momoRequestType);
            body.put("lang", "vi");
            String rawSignature =
                    "accessKey=" + momoAccessKey +
                            "&amount=" + amount +
                            "&extraData=" + extraData +
                            "&ipnUrl=" + momoNotifyUrl +
                            "&orderId=" + orderId +
                            "&orderInfo=" + orderInfo +
                            "&partnerCode=" + momoPartnerCode +
                            "&redirectUrl=" + redirectUrl +
                            "&requestId=" + requestId +
                            "&requestType=" + momoRequestType;
            String signature = hmacSHA256(rawSignature, momoSecretKey);
            body.put("signature", signature);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(momoEndpoint))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(res.body());
            log.info("💳 [MoMo] Response: {}", json.toString(2));
            if (json.has("payUrl")) return json.getString("payUrl");
            throw new RuntimeException("Không nhận được payUrl từ MoMo: " + json);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo URL MoMo: " + e.getMessage());
        }
    }

    // ... (Hàm hmacSHA256 giữ nguyên) ...
    private String hmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ... (Hàm getPaymentInfo giữ nguyên logic gọi MQ của nó) ...
    @Override
    public PaymentInfoResponse getPaymentInfo(String transactionId) {
        // ... (Hàm này giữ nguyên, không thay đổi logic) ...
        Payment p = paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch: " + transactionId));
        Customer c = p.getCustomer();
        String type = (p.getCartIdList() == null || p.getCartIdList().isEmpty()) ? "deposit" : "order";
        Long userId = (p.getUserId() != null) ? p.getUserId()
                : extractUserIdFromProductName(p.getProductNames());
        Long sellerId = null;
        try {
            if (p.getCartIdList() != null && !p.getCartIdList().isEmpty()) {
                // 📨 Gửi yêu cầu đến cart-service qua MQ
                String txId = UUID.randomUUID().toString();
                Map<String, Object> fetchRequest = Map.of(
                    "transactionId", txId,
                    "cartIds", p.getCartIdList()
                );
                mqPublisher.publish("cart.fetch.request", fetchRequest);
                log.info("📤 [MQ] Gửi yêu cầu lấy thông tin giỏ hàng để xác định sellerId");
                // ⏳ Chờ phản hồi
                Map<String, Object> response = null;
                int tries = 0;
                while (tries < 10 && response == null) {
                    Thread.sleep(1000);
                    response = cartResponseListener.getResponse(txId);
                    tries++;
                }
                if (response != null && response.containsKey("items")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    if (!items.isEmpty() && items.get(0).containsKey("sellerId")) {
                        sellerId = ((Number) items.get(0).get("sellerId")).longValue();
                        log.info("✅ Lấy được sellerId={} từ MQ", sellerId);
                    }
                } else {
                    log.warn("⚠️ Không có phản hồi hợp lệ từ cart-service khi lấy sellerId");
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Lỗi khi lấy sellerId từ MQ: {}", e.getMessage());
        }
        return PaymentInfoResponse.builder()
                .transactionId(p.getTransactionId())
                .status(p.getStatus())
                .method(p.getMethod())
                .fullName(c != null ? c.getFullName() : "Không có")
                .phone(c != null ? c.getPhone() : "Không có")
                .email(c != null ? c.getEmail() : "Không có")
                .address(c != null ? c.getAddress() : "Không có")
                .productName(p.getProductNames())
                .totalAmount(p.getTotalAmount() != null ? p.getTotalAmount().doubleValue() : 0d)
                .price(p.getAmount() != null ? p.getAmount().doubleValue() : 0d)
                .type(type)
                .userId(userId)
                .sellerId(sellerId)
                .build();
    }

    // ... (Hàm findByTransactionId giữ nguyên) ...
    @Override
    public Payment findByTransactionId(String transactionId) {
        return paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch với transactionId: " + transactionId));
    }

    // ... (Hàm updateStatus giữ nguyên) ...
    @Override
    public void updateStatus(String transactionId, String newStatus) {
        Payment p = paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment với transactionId: " + transactionId));
        p.setStatus(newStatus);
        p.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(p);
    }

    // ... (Hàm checkMQConnection giữ nguyên) ...
    @PostConstruct
    public void checkMQConnection() {
        try {
            log.info("🔗 [MQ] Ready to publish via exchange '{}'", "ev.exchange");
        } catch (Exception e) {
            log.error("❌ [MQ] RabbitMQ not available: {}", e.getMessage());
        }
    }

    // ============================================================
    // 🧩 HÀM PHỤ MỚI 1: Lấy chi tiết Cart
    // ============================================================
    /**
     * 🧩 HÀM PHỤ: Lấy chi tiết các mục trong giỏ hàng từ Cart-Service qua MQ.
     *
     * @param cartIds Danh sách ID giỏ hàng cần lấy
     * @param debugContext Chỉ dùng cho mục đích log (ví dụ: transactionId của payment)
     * @return Danh sách các 'items' (dưới dạng Map) từ cart-service
     */
    private List<Map<String, Object>> fetchCartItems(List<Long> cartIds, String debugContext) {
        if (cartIds == null || cartIds.isEmpty()) {
            log.warn("⚠️ [fetchCartItems] Danh sách cartIds rỗng cho context {}.", debugContext);
            return Collections.emptyList();
        }

        // 📨 Gửi yêu cầu lấy giỏ hàng từ cart-service
        String fetchId = UUID.randomUUID().toString(); // ID riêng cho yêu cầu fetch này
        mqPublisher.publish("cart.fetch.request", Map.of("transactionId", fetchId, "cartIds", cartIds));
        log.info("📤 [MQ] Gửi yêu cầu fetchCartItems (context={}) với fetchId={}", debugContext, fetchId);

        // ⏳ Chờ phản hồi
        Map<String, Object> response = null;
        int tries = 0;
        while (tries < 10 && response == null) { // Chờ tối đa 10s
            try {
                Thread.sleep(1000);
                response = cartResponseListener.getResponse(fetchId);
                tries++;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt(); // Khôi phục trạng thái gián đoạn
            }
        }

        if (response == null) {
    log.error("❌ [fetchCartItems] Không nhận được phản hồi cart cho fetchId={}", fetchId);
    return Collections.emptyList();
}

Object rawItems = response.get("items");

if (rawItems == null) {
    rawItems = response.get("cartItems");
}

if (rawItems == null) {
    rawItems = response.get("data");
}

if (!(rawItems instanceof List<?> list)) {
    log.error("❌ [fetchCartItems] Response không có items: {}", response);
    return Collections.emptyList();
}

@SuppressWarnings("unchecked")
List<Map<String, Object>> items = (List<Map<String, Object>>) list;

log.info("✅ [fetchCartItems] Lấy được {} sản phẩm", items.size());

return items;
    }


    // ============================================================
    // 🧩 HÀM PHỤ MỚI 2: Gửi sự kiện thành công (thay thế 2 hàm cũ)
    // ============================================================
    private void publishOrderSuccessEvents(
            String transactionId,
            String method,
            Long buyerId, // Đây là 'userId' từ request
            List<Map<String, Object>> items,
            List<Long> paidCartIds
    ) {
        // 1. Gửi event đến cart-service để xóa ĐÚNG CÁC ITEM ĐÃ MUA
        try {
            Map<String, Object> cartEvent = new HashMap<>(); // Dùng HashMap để dễ put
            cartEvent.put("event", "order.paid");
            cartEvent.put("transactionId", transactionId);
            cartEvent.put("userId", buyerId);
            cartEvent.put("cartIds", paidCartIds); // 🟢 QUAN TRỌNG: Gửi danh sách ID cần xóa
            cartEvent.put("method", method);
            cartEvent.put("time", LocalDateTime.now().toString());

            mqPublisher.publish("cart.order.paid", cartEvent);
            log.info("🧾 [Cart] Published cart.order.paid event -> {}", cartEvent);
        } catch (Exception e) {
            log.error("❌ Lỗi khi publish event cart.order.paid: {}", e.getMessage(), e);
        }

        // 2. Gửi event chi tiết cho TỪNG ITEM
        //    (Listing-service và Review-service sẽ lắng nghe 'order.paid')
        for (Map<String, Object> item : items) {
            try {
                // Lấy thông tin chi tiết từ item
                // (Giả định cart-service trả về các key này)
                Long productId = ((Number) item.get("productId")).longValue();
                Long sellerId = ((Number) item.get("sellerId")).longValue();
                BigDecimal price = BigDecimal.valueOf(((Number) item.get("price")).doubleValue());
                String productName = (String) item.get("productName");

                // Tạo payload chuẩn (giống OrderCompletedEventDTO của Review-service)
                Map<String, Object> event = new HashMap<>();
                event.put("transactionId", transactionId); // ID của giao dịch thanh toán
                event.put("productId", productId);         // ID sản phẩm (Listing-service cần)
                event.put("sellerId", sellerId);        // ID người bán (Review-service cần)
                event.put("userId", buyerId);
                event.put("price", price);              // Giá của item này
                event.put("productName", productName);  // Tên SP (Review-service cần)
                event.put("method", method);
                event.put("status", "SUCCESS");
                event.put("type", "order-item"); // 'order-item' để phân biệt
                event.put("time", LocalDateTime.now().toString());

                // Gửi với routing key 'order.paid'
                mqPublisher.publish("order.paid", event);
                log.info("📤 [OrderPaid] Published event cho productId #{}: {}", productId, event);

            } catch (Exception e) {
                log.error("❌ Lỗi khi publish event 'order.paid' cho item: {} - Lỗi: {}", item, e.getMessage(), e);
            }
        }
    }
    // ============================================================
    // 🧩 HÀM PHỤ MỚI 3: Kiểm tra số dư ví EV
    // ============================================================
    private boolean hasEnoughBalance(Long userId, BigDecimal amount) {
        try {
            // Gọi API kiểm tra số dư (ví dụ: GET /api/wallet/balance/{userId})
            String url = "http://wallet-service:8089/api/wallet/balance/" + userId;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                BigDecimal balance = json.has("balance")
                        ? BigDecimal.valueOf(json.getDouble("balance"))
                        : BigDecimal.ZERO;
                log.info("💰 [EV Wallet] userId={} có số dư {}đ, cần {}", userId, balance, amount);
                return balance.compareTo(amount) >= 0;
            } else {
                log.warn("⚠️ [EV Wallet] Không thể lấy số dư từ wallet-service (status {})", response.statusCode());
            }
        } catch (Exception e) {
            log.error("❌ [EV Wallet] Lỗi khi kiểm tra số dư ví: {}", e.getMessage());
        }
        return false;
    }

}
