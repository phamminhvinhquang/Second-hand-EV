package local.wallet_service.controller;

import local.wallet_service.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/debug")
public class TestPayrollController {

    @Autowired
    private PayrollService payrollService;

    @PostMapping("/payroll/run")
    public String runPayrollManually() {
        int count = payrollService.processMonthlySalaryAuto();
        return "Đã xử lý trả lương cho " + count + " nhân viên.";
    }
}
