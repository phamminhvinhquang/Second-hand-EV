package local.Second_hand_EV_Battery_Trading_Platform.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPayUtils {

    // ====== TẠO CHỮ KÝ HMAC SHA512 ======
    public static String hmacSHA512(String key, String data) throws Exception {
        Mac hmac512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac512.init(secretKey);
        byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            hash.append(String.format("%02x", b));
        }
        return hash.toString();
    }

    // ====== HÀM TẠO CHUỖI HASH DATA CHUẨN (A→Z, URL Encode) ======
    public static String hashAllFields(Map<String, String> fields) throws Exception {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) sb.append('&');
                sb.append(fieldName)
                  .append('=')
                  .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                first = false;
            }
        }
        return sb.toString();
    }
}
