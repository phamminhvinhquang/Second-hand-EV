package local.Second_hand_EV_Battery_Trading_Platform.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VNPayService {

    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;

    @Value("${vnpay.url}")
    private String vnp_Url;

    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl;

    public String createPaymentUrl(long amount, String transactionId) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnp_TmnCode);
        params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay yêu cầu nhân 100
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", transactionId);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + transactionId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        // Sắp xếp ký tự theo thứ tự alphabet
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String name : fieldNames) {
            String value = params.get(name);
            if ((value != null) && (value.length() > 0)) {
                hashData.append(name).append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(name, StandardCharsets.US_ASCII.toString()))
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.US_ASCII.toString()));
                query.append('&');
                hashData.append('&');
            }
        }
        // Xóa ký tự '&' cuối
        hashData.setLength(hashData.length() - 1);
        query.setLength(query.length() - 1);

        String secureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        return vnp_Url + "?" + query;
    }

    private String hmacSHA512(String key, String data) throws Exception {
        javax.crypto.Mac hmac512 = javax.crypto.Mac.getInstance("HmacSHA512");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac512.init(secretKey);
        byte[] hashBytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
