package local.contract.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import local.contract.entity.Contract;
import local.contract.model.ContractRequest;
import local.contract.model.ContractResponse;
import local.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepo;

    @Value("${transaction.service.url:http://transaction-service:8083}")
    private String transactionServiceBaseUrl;

    // ============================================================
    // 🟣 1️⃣ Tạo hợp đồng tự động (qua MQ hoặc API create)
    // ============================================================
    @Override
    public ContractResponse createContract(ContractRequest request) {
        try {
            log.info("📩 [ContractService] Nhận yêu cầu tạo hợp đồng tự động cho transactionId={}", request.getTransactionId());

            // ✅ Nếu hợp đồng đã tồn tại, bỏ qua (tránh rác)
            if (contractRepo.existsByTransactionId(request.getTransactionId())) {
                log.warn("⚠️ Hợp đồng transactionId={} đã tồn tại, bỏ qua!", request.getTransactionId());
                return ContractResponse.builder()
                        .transactionId(request.getTransactionId())
                        .message("Hợp đồng đã tồn tại, không tạo lại.")
                        .build();
            }

            // ✅ Gọi Transaction-Service để lấy thông tin
            String apiUrl = transactionServiceBaseUrl + "/api/payments/info/" + request.getTransactionId();
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new RuntimeException("Không thể lấy thông tin thanh toán từ transaction-service");

            JSONObject json = new JSONObject(response.body());

            // ✅ Tạo hợp đồng DRAFT (chưa ký)
            Contract ct = new Contract();
            ct.setTransactionId(json.optString("transactionId"));
            ct.setUserId(json.optLong("userId"));
            ct.setCustomerName(json.optString("fullName"));
            ct.setCustomerPhone(json.optString("phone"));
            ct.setCustomerEmail(json.optString("email"));
            ct.setCustomerAddress(json.optString("address"));
            ct.setPaymentMethod(request.getMethod());
            ct.setProductName(json.optString("productName"));
            ct.setTotalPrice(BigDecimal.valueOf(json.optDouble("totalAmount", 0)));
            ct.setStatus("DRAFT");
            ct.setCreatedAt(LocalDateTime.now());
            ct.setUpdatedAt(LocalDateTime.now());

            contractRepo.save(ct);
            log.info("✅ [ContractService] Đã tạo hợp đồng DRAFT transactionId={}", request.getTransactionId());

            return ContractResponse.builder()
                    .transactionId(ct.getTransactionId())
                    .userId(ct.getUserId())
                    .customerName(ct.getCustomerName())
                    .productName(ct.getProductName())
                    .totalPrice(ct.getTotalPrice())
                    .message("Hợp đồng nháp được tạo (chưa ký).")
                    .build();

        } catch (Exception e) {
            log.error("❌ [ContractService] Lỗi khi tạo hợp đồng tự động: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tạo hợp đồng tự động: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // 🟢 2️⃣ Người dùng ký hợp đồng (Frontend /sign)
    // ============================================================
    @Override
    public ContractResponse signContract(ContractRequest request) {
        try {
            log.info("✍️ [ContractService] Người dùng ký hợp đồng transactionId={}", request.getTransactionId());

            // ✅ Lấy thông tin thanh toán
            String apiUrl = transactionServiceBaseUrl + "/api/payments/info/" + request.getTransactionId();
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new RuntimeException("Không thể lấy thông tin thanh toán từ transaction-service");

            JSONObject json = new JSONObject(response.body());
            if (!"SUCCESS".equalsIgnoreCase(json.optString("status"))) {
                return ContractResponse.builder()
                        .transactionId(request.getTransactionId())
                        .message("❌ Thanh toán chưa hoàn tất, không thể ký hợp đồng.")
                        .build();
            }

            // ✅ Nếu đã có hợp đồng DRAFT -> cập nhật, không tạo mới
            Contract ct = contractRepo.findByTransactionId(request.getTransactionId())
                    .orElse(new Contract());

            ct.setTransactionId(request.getTransactionId());
            ct.setUserId(json.optLong("userId"));
            ct.setCustomerName(json.optString("fullName"));
            ct.setCustomerPhone(json.optString("phone"));
            ct.setCustomerEmail(json.optString("email"));
            ct.setCustomerAddress(json.optString("address"));
            ct.setPaymentMethod(json.optString("method"));
            ct.setProductName(json.optString("productName"));
            ct.setTotalPrice(BigDecimal.valueOf(json.optDouble("totalAmount", 0)));
            ct.setSignature(request.getSignature());
            ct.setSignedAt(LocalDateTime.now());
            ct.setStatus("SIGNED");

            // ✅ Ghi file PDF nếu có
            if (request.getPdfBase64() != null && !request.getPdfBase64().isBlank()) {
                try {
                    String fileName = "contract_" + UUID.randomUUID() + ".pdf";
                    Path dir = Paths.get("/app/contracts");

                    if (!Files.exists(dir)) Files.createDirectories(dir);

                    Path filePath = dir.resolve(fileName);
                    byte[] pdfBytes = Base64.getDecoder().decode(request.getPdfBase64());
                    Files.write(filePath, pdfBytes);

                    ct.setPdfUrl("http://localhost:9000/contracts/" + fileName);

                    

                    log.info("📄 Đã lưu file PDF hợp đồng tại {}", filePath.toAbsolutePath());
                } catch (IOException ex) {
                    log.error("❌ Lỗi khi ghi file PDF: {}", ex.getMessage());
                }
            }

            contractRepo.save(ct);
            log.info("✅ [ContractService] Hợp đồng ký thành công userId={}, transactionId={}", ct.getUserId(), ct.getTransactionId());

            return ContractResponse.builder()
                    .id(ct.getId())
                    .transactionId(ct.getTransactionId())
                    .userId(ct.getUserId())
                    .customerName(ct.getCustomerName())
                    .productName(ct.getProductName())
                    .totalPrice(ct.getTotalPrice())
                    .pdfUrl(ct.getPdfUrl())
                    .signedAt(ct.getSignedAt().toString())
                    .message("✅ Hợp đồng đã được ký và lưu thành công.")
                    .build();

        } catch (Exception e) {
            log.error("❌ [ContractService] Lỗi khi ký hợp đồng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi ký hợp đồng: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // 🧾 3️⃣ Lấy danh sách hợp đồng (chỉ lấy SIGNED)
    // ============================================================
    @Override
    public List<ContractResponse> getContractsByUserId(Long userId) {
        log.info("📚 [ContractService] Lấy danh sách hợp đồng của userId={}", userId);

        return contractRepo.findByUserId(userId).stream()
                .filter(c -> "SIGNED".equalsIgnoreCase(c.getStatus()))
                .map(c -> ContractResponse.builder()
                        .id(c.getId())
                        .transactionId(c.getTransactionId())
                        .userId(c.getUserId())
                        .customerName(c.getCustomerName())
                        .productName(c.getProductName())
                        .totalPrice(c.getTotalPrice())
                        .pdfUrl(c.getPdfUrl())
                        .signedAt(c.getSignedAt() != null ? c.getSignedAt().toString() : null)
                        .message("Đã ký hợp đồng")
                        .build())
                .collect(Collectors.toList());
    }
}
