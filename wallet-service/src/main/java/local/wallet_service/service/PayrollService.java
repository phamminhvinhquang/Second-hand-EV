package local.wallet_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import local.wallet_service.model.*;
import local.wallet_service.model.enums.*;
import local.wallet_service.repository.*;
import local.wallet_service.dto.StaffSalaryDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {

    private final StaffSalaryRepository staffRepo;
    private final SalaryTransactionRepository salaryTxRepo;
    private final PlatformWalletRepository platformRepo;
    private final UserWalletRepository userWalletRepo;
    private final WalletTransactionRepository walletTxRepo;

    private final WebClient.Builder webClientBuilder;

    @Value("${wallet.platform.id:1}")
    private Long platformWalletId;

    @Value("${user.service.url:http://user-service:8084}") // ⚙️ user-service chạy port 8084
    private String userServiceUrl;

    // ============================================
    // 🔹 AUTO PAYROLL - chạy mỗi ngày 10h sáng
    // ============================================
    @Scheduled(cron = "0 0 10 * * ?") // mỗi ngày 10:00 sáng
    public void autoRunPayroll() {
        int count = processMonthlySalaryAuto();
        log.info("💸 [AutoPayroll] Đã xử lý trả lương tự động cho {} nhân viên", count);
    }

    /**
     * ✅ Tự động chạy hàng tháng: kiểm tra và trả lương
     */
    @Transactional
    public int processMonthlySalaryAuto() {
        List<StaffSalary> activeStaffs = staffRepo.findByStatus("ACTIVE");
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        int count = 0;

        for (StaffSalary s : activeStaffs) {
            int payDay = Math.min(s.getPayDay(), ym.lengthOfMonth());
            boolean dueToday = today.getDayOfMonth() == payDay;
            boolean alreadyPaidThisMonth = s.getLastPaid() != null &&
                    s.getLastPaid().getMonthValue() == today.getMonthValue() &&
                    s.getLastPaid().getYear() == today.getYear();

            if (dueToday && !alreadyPaidThisMonth) {
                try {
                    boolean ok = payOneStaff(s.getUserId(), s.getSalary(), ym.toString(), "AUTO");
                    if (ok) {
                        s.setLastPaid(today);
                        staffRepo.save(s);
                        count++;
                    }
                } catch (Exception e) {
                    log.error("⚠️ Lỗi khi trả lương tự động cho user #{}: {}", s.getUserId(), e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * ✅ Trả lương cho 1 nhân viên (admin hoặc tự động)
     * - Kiểm tra role từ user-service
     */
    @Transactional
    public boolean payOneStaff(Long userId, BigDecimal amount, String periodLabel, String note) {
        log.info("🏁 [Payroll] Bắt đầu trả lương cho user #{} ({}): {}", userId, periodLabel, amount);

        // ⚙️ 0️⃣ Kiểm tra ngày trả lương hợp lệ
        StaffSalary salaryConfig = staffRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy cấu hình lương cho user #" + userId));

        int payDay = salaryConfig.getPayDay();
        int today = LocalDate.now().getDayOfMonth();

        if (today != payDay) {
            log.warn("🚫 Hôm nay ({}) không phải ngày trả lương ({}) cho user #{}", today, payDay, userId);
            salaryTxRepo.save(SalaryTransaction.builder()
                    .userId(userId)
                    .amount(amount)
                    .periodLabel(periodLabel)
                    .status(RecordStatus.FAILED)
                    .note("Hôm nay không phải ngày trả lương, bị từ chối")
                    .build());
            return false;
        }

        // ⚙️ 1️⃣ Gọi user-service kiểm tra role
        Set<String> roles = fetchUserRoles(userId);
        if (roles == null || roles.stream().noneMatch(r -> r.equalsIgnoreCase("STAFF"))) {
            log.warn("🚫 User #{} không có quyền STAFF - hủy giao dịch", userId);
            salaryTxRepo.save(SalaryTransaction.builder()
                    .userId(userId)
                    .amount(amount)
                    .periodLabel(periodLabel)
                    .status(RecordStatus.FAILED)
                    .note("User không có quyền STAFF - rejected")
                    .build());
            throw new IllegalStateException("User #" + userId + " không phải STAFF, không thể trả lương");
        }

        // ⚙️ 2️⃣ Lấy ví sàn
        PlatformWallet platform = platformRepo.findById(platformWalletId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy ví sàn"));

        // ⚙️ 3️⃣ Kiểm tra số dư ví sàn
        if (platform.getBalance().compareTo(amount) < 0) {
            log.error("❌ Không đủ số dư ví sàn (balance={}, cần={})", platform.getBalance(), amount);
            salaryTxRepo.save(SalaryTransaction.builder()
                    .userId(userId)
                    .amount(amount)
                    .periodLabel(periodLabel)
                    .status(RecordStatus.FAILED)
                    .note("Insufficient balance: " + note)
                    .build());
            return false;
        }

        // ⚙️ 4️⃣ Thực hiện trả lương (như cũ)
        platform.setBalance(platform.getBalance().subtract(amount));
        platformRepo.save(platform);

        UserWallet staff = userWalletRepo.findByUserId(userId)
                .orElseGet(() -> userWalletRepo.save(
                        UserWallet.builder()
                                .userId(userId)
                                .balance(BigDecimal.ZERO)
                                .build()));

        staff.setBalance(staff.getBalance().add(amount));
        userWalletRepo.save(staff);

        // ⚙️ 5️⃣ Ghi nhận giao dịch và cập nhật ngày trả
        walletTxRepo.save(WalletTransaction.builder()
                .walletType(WalletType.PLATFORM)
                .walletRefId(platformWalletId)
                .txType(TxType.DEBIT)
                .amount(amount)
                .description("Monthly salary " + periodLabel + " to user #" + userId)
                .build());

        walletTxRepo.save(WalletTransaction.builder()
                .walletType(WalletType.USER)
                .walletRefId(userId)
                .txType(TxType.CREDIT)
                .amount(amount)
                .description("Salary " + periodLabel)
                .build());

        salaryTxRepo.save(SalaryTransaction.builder()
                .userId(userId)
                .amount(amount)
                .periodLabel(periodLabel)
                .status(RecordStatus.PAID)
                .note(note)
                .build());

        salaryConfig.setLastPaid(LocalDate.now());
        staffRepo.save(salaryConfig);

        log.info("✅ [Payroll] Đã trả lương thành công cho user #{}: {}", userId, amount);
        return true;
    }


    /**
     * ✅ Gọi sang user-service để lấy role của user (qua REST API)
     */
    private Set<String> fetchUserRoles(Long userId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(userServiceUrl + "/api/user/" + userId + "/roles")
                    .retrieve()
                    .bodyToMono(Set.class)
                    .block();
        } catch (Exception e) {
            log.error("⚠️ Không thể kết nối user-service để kiểm tra role: {}", e.getMessage());
            throw new IllegalStateException("Không thể kết nối user-service để kiểm tra role");
        }
    }

    /**
     * ✅ Cập nhật hoặc tạo mới cấu hình lương cho nhân viên
     */
    @Transactional
    public void upsertStaffSalary(Long userId, BigDecimal salary, Integer payDay, String status) {
        StaffSalary staff = staffRepo.findByUserId(userId)
                .orElseGet(() -> StaffSalary.builder()
                        .userId(userId)
                        .salary(salary)
                        .payDay(payDay)
                        .status(status)
                        .startDate(LocalDate.now())
                        .build());

        staff.setSalary(salary);
        staff.setPayDay(payDay);
        staff.setStatus(status);
        staffRepo.save(staff);
        log.info("🧾 [Payroll] Cập nhật cấu hình lương cho user #{} ({} VND, trả ngày {})", userId, salary, payDay);
    }

    @Transactional(readOnly = true)
    public List<StaffSalaryDTO> getAllStaffWithSalary() {
        return staffRepo.findAll().stream()
                .map(s -> new StaffSalaryDTO(
                        s.getUserId(),
                        s.getSalary(),
                        s.getPayDay(),
                        s.getStatus(),
                        s.getLastPaid()
                ))
                .toList();
    }


}
