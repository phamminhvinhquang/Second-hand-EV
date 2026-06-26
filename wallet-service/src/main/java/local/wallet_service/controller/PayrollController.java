package local.wallet_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

import local.wallet_service.dto.*;
import local.wallet_service.model.SalaryTransaction;
import local.wallet_service.model.StaffSalary;
import local.wallet_service.repository.SalaryTransactionRepository;
import local.wallet_service.repository.StaffSalaryRepository;
import local.wallet_service.service.PayrollService;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final SalaryTransactionRepository salaryTxRepo;
    private final StaffSalaryRepository staffRepo;

    // ================================================================
    // ✅ 1️⃣ Lấy danh sách tất cả nhân viên & thông tin lương
    // ================================================================
    @GetMapping("/staff")
    public ResponseEntity<List<StaffSalary>> getAllStaff() {
        List<StaffSalary> list = staffRepo.findAll();
        return ResponseEntity.ok(list);
    }

    // ================================================================
    // ✅ 2️⃣ Cập nhật hoặc thêm mới cấu hình lương cho nhân viên
    // ================================================================
    @PutMapping("/staff/{userId}")
    public ResponseEntity<?> upsertStaffSalary(
            @PathVariable Long userId,
            @RequestBody UpdateStaffSalaryRequest req) {
        try {
            payrollService.upsertStaffSalary(userId, req.getSalary(), req.getPayDay(), req.getStatus());
            return ResponseEntity.ok("Updated salary for userId=" + userId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ================================================================
    // ✅ 3️⃣ Trả lương thủ công (Admin nhấn nút “Trả lương”)
    // ================================================================
    @PostMapping("/run")
    public ResponseEntity<?> paySalary(@RequestBody PaySalaryRequest req) {
        try {
            boolean ok = payrollService.payOneStaff(
                    req.getUserId(),
                    req.getAmount(),
                    req.getPeriodLabel(),
                    "MANUAL"
            );
            return ResponseEntity.ok(ok ? "PAID" : "FAILED");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Payroll failed: " + e.getMessage());
        }
    }

    // ================================================================
    // ✅ 4️⃣ Chạy trả lương tự động cho tất cả nhân viên (test)
    // ================================================================
    @PostMapping("/run-auto")
    public ResponseEntity<?> runAuto() {
        try {
            int count = payrollService.processMonthlySalaryAuto();
            return ResponseEntity.ok("Auto payroll done for " + count + " staff(s)");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Auto payroll error: " + e.getMessage());
        }
    }

    // ================================================================
    // ✅ 5️⃣ Xem lịch sử trả lương của 1 nhân viên
    // ================================================================
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<SalaryTransaction>> history(@PathVariable Long userId) {
        return ResponseEntity.ok(salaryTxRepo.findByUserIdOrderByPayDateDesc(userId));
    }
}
