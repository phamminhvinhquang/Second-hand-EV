package local.wallet_service.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import local.wallet_service.dto.PaymentSuccessEvent;
import local.wallet_service.dto.WalletPaymentRequest;
import local.wallet_service.service.WalletService;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // ✅ 1️⃣ API chia hoa hồng sau khi thanh toán thành công
    @PostMapping("/commission/apply")
    public ResponseEntity<?> applyCommission(@RequestBody PaymentSuccessEvent event) {
        return ResponseEntity.ok(walletService.applyCommission(event));
    }

    // ✅ 2️⃣ API lấy số dư ví người dùng
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getUserBalance(userId));
    }

    // ✅ 3️⃣ API lấy lịch sử giao dịch người dùng
    @GetMapping("/transactions/user/{userId}")
    public ResponseEntity<?> getUserTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getUserTransactions(userId));
    }

    // ✅ 4️⃣ API nạp tiền vào ví
    @PostMapping("/deposit")
    public ResponseEntity<?> depositToUser(
            @RequestParam Long userId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String method) {

        String result = walletService.depositToUser(userId, amount, transactionId, method);
        boolean success = result.startsWith("✅");

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", result);
        response.put("userId", userId);
        response.put("amount", amount);
        response.put("method", method);

        return ResponseEntity.status(success ? 200 : 400).body(response);
    }

    // ✅ 5️⃣ API thanh toán bằng ví EV (trừ tiền ví)
    @PostMapping("/pay")
    public ResponseEntity<?> payWithWallet(@RequestBody WalletPaymentRequest req) {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = walletService.payWithWallet(req);
            boolean success = result.startsWith("✅");

            response.put("success", success);
            response.put("message", result);
            response.put("userId", req.getUserId());
            response.put("amount", req.getAmount());
            response.put("description", req.getDescription());

            return ResponseEntity.status(success ? 200 : 400).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi thanh toán bằng ví EV: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ✅ 6️⃣ API: Tổng số dư ví sàn
    @GetMapping("/platform/balance")
    public ResponseEntity<?> getPlatformBalance() {
        BigDecimal balance = walletService.getPlatformBalance();
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("balance", balance != null ? balance : BigDecimal.ZERO);
        return ResponseEntity.ok(res);
    }



    // ✅ 7️⃣ API: Lịch sử giao dịch ví sàn
    @GetMapping("/platform/transactions")
    public ResponseEntity<?> getPlatformTransactions() {
        return ResponseEntity.ok(
            walletService.getPlatformTransactions()
        );
    }

    // ✅ 8️⃣ API: Lịch sử giao dịch toàn hệ thống (mọi ví)
    @GetMapping("/all/transactions")
    public ResponseEntity<?> getAllTransactions() {
        return ResponseEntity.ok(
            walletService.getAllTransactions()
        );
    }

    // ✅ 9️⃣ Lấy số dư chi tiết theo userId (được transaction-service gọi)
    @GetMapping("/balance/{userId}")
    public ResponseEntity<?> getBalanceForTransaction(@PathVariable Long userId) {
        try {
            BigDecimal balance = walletService.getBalanceByUserId(userId);
            return ResponseEntity.ok(Map.of("balance", balance));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

}
