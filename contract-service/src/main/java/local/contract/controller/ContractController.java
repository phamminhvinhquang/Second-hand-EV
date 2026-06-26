package local.contract.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import local.contract.model.ContractRequest;
import local.contract.model.ContractResponse;
import local.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Cho phép frontend gọi API
public class ContractController {

    private final ContractService contractService;

    // ============================================================
    // 1️⃣ Người dùng ký hợp đồng thủ công (Frontend gửi chữ ký)
    // ============================================================
    @PostMapping("/sign")
    public ResponseEntity<?> sign(@RequestBody ContractRequest request) {
        log.info("🖋️ [API] Yêu cầu ký hợp đồng cho transactionId={}", request.getTransactionId());

        // 🔒 Kiểm tra dữ liệu bắt buộc
        if (request.getTransactionId() == null || request.getTransactionId().isBlank()) {
            return ResponseEntity.badRequest().body(
                new ContractResponse(null, null, null, null, null, null, null, null, "❌ Thiếu transactionId!")
            );
        }

        // 🔒 Nếu thiếu chữ ký thì không cho lưu
        if (request.getSignature() == null || request.getSignature().isBlank()) {
            return ResponseEntity.badRequest().body(
                new ContractResponse(null, request.getTransactionId(), null, null, null, null, null, null,
                        "❌ Chưa có chữ ký, không thể xác nhận hợp đồng.")
            );
        }

        // 🔒 Nếu thiếu file PDF base64 cũng không lưu (bảo vệ thêm)
        if (request.getPdfBase64() == null || request.getPdfBase64().isBlank()) {
            return ResponseEntity.badRequest().body(
                new ContractResponse(null, request.getTransactionId(), null, null, null, null, null, null,
                        "⚠️ Thiếu file PDF, vui lòng ký lại để lưu hợp đồng.")
            );
        }

        // ✅ Nếu đủ điều kiện thì mới xử lý
        ContractResponse result = contractService.signContract(request);
        return ResponseEntity.ok(result);
    }

    // ============================================================
    // 2️⃣ Dùng cho MQ event: tạo hợp đồng tự động sau thanh toán
    // ============================================================
    @PostMapping("/create")
    public ResponseEntity<ContractResponse> create(@RequestBody ContractRequest request) {
        log.info("⚙️ [API] Yêu cầu tạo hợp đồng tự động cho transactionId={}", request.getTransactionId());
        return ResponseEntity.ok(contractService.createContract(request));
    }

    // ============================================================
    // 3️⃣ Lấy danh sách hợp đồng đã ký theo userId (trang lịch sử)
    // ============================================================
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ContractResponse>> getByUser(@PathVariable Long userId) {
        log.info("📜 [API] Lấy danh sách hợp đồng của userId={}", userId);
        return ResponseEntity.ok(contractService.getContractsByUserId(userId));
    }
}
