package com.example.revenue_service.dto;

import lombok.Builder;
import lombok.Data;
import java.text.NumberFormat;
import java.util.Locale;

@Data
@Builder
public class RevenueDTO {
    private String date; // Dùng cho daily (yyyy-MM-dd)
    private Integer year; // Dùng cho monthly, yearly
    private Integer month; // Dùng cho monthly
    private String label; // Kết hợp Year & Month (yyyy-MM)
    private Double total;
    private String formattedTotal;

    // Helper: Định dạng tiền Việt Nam
    public String getFormattedTotal() {
        if (this.total == null) return "0 ₫";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(this.total) + " ₫";
    }
}