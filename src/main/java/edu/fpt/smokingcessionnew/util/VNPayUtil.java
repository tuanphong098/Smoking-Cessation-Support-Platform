package edu.fpt.smokingcessionnew.util;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * VNPay Utility Class theo chuẩn từ VNPay documentation
 * Đã sửa lỗi signature và URL encoding theo khuyến nghị
 */
public class VNPayUtil {

    /**
     * Tạo mã giao dịch ngẫu nhiên
     */
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Sắp xếp và encode parameters theo chuẩn VNPay
     * Đây là hàm quan trọng để tạo signature đúng
     */
    public static Map<String, String> sortObject(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                try {
                    // Encode theo chuẩn VNPay: thay %20 thành +
                    String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                        .replace("%20", "+");
                    sorted.put(key, encodedValue);
                } catch (UnsupportedEncodingException e) {
                    sorted.put(key, value);
                }
            }
        }
        return sorted;
    }

    /**
     * Tạo URL thanh toán VNPay với signature đúng
     */
    public static String createPaymentUrl(Map<String, String> vnpParams, String secretKey) {
        // Loại bỏ các tham số không cần thiết cho việc tạo signature
        Map<String, String> paramsForSign = new HashMap<>(vnpParams);
        paramsForSign.remove("vnp_SecureHashType");
        paramsForSign.remove("vnp_SecureHash");

        // Sắp xếp parameters theo thứ tự alphabet
        List<String> fieldNames = new ArrayList<>(paramsForSign.keySet());
        Collections.sort(fieldNames);

        // Tạo hash data cho signature
        StringBuilder hashData = new StringBuilder();
        // Tạo query string cho URL
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = paramsForSign.get(fieldName);

            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data (không encode cho hash)
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);

                // Build query (encode cho URL)
                try {
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (UnsupportedEncodingException e) {
                    query.append(fieldName);
                    query.append('=');
                    query.append(fieldValue);
                }

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        // Tạo secure hash
        String vnpSecureHash = hmacSHA512(secretKey, hashData.toString());

        // Thêm secure hash vào query
        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        return query.toString();
    }

    /**
     * Tạo HMAC SHA512 signature
     */
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("Key hoặc data không được null");
            }

            Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKeySpec);

            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);

            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new RuntimeException("Lỗi tạo HMAC SHA512: " + ex.getMessage(), ex);
        }
    }

    /**
     * Lấy IP Address của client (chuyển về IPv4)
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = null;

        try {
            // Kiểm tra các header proxy phổ biến
            ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }

            // Xử lý trường hợp có nhiều IP (lấy IP đầu tiên)
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }

            // Chuyển IPv6 localhost thành IPv4
            if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress)) {
                ipAddress = "127.0.0.1";
            }

            // Validate IPv4 format
            if (ipAddress != null && !isValidIPv4(ipAddress)) {
                ipAddress = "127.0.0.1";
            }

        } catch (Exception ex) {
            ipAddress = "127.0.0.1";
        }

        return ipAddress != null ? ipAddress : "127.0.0.1";
    }

    /**
     * Kiểm tra định dạng IPv4 hợp lệ
     */
    private static boolean isValidIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate signature từ VNPay response
     */
    public static boolean validateSignature(Map<String, String> fields, String secretKey) {
        String vnpSecureHash = fields.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
            return false;
        }

        // Tạo bản sao để không ảnh hưởng đến map gốc
        Map<String, String> fieldsToValidate = new HashMap<>(fields);
        fieldsToValidate.remove("vnp_SecureHashType");
        fieldsToValidate.remove("vnp_SecureHash");

        String signValue = hashAllFields(fieldsToValidate, secretKey);
        return signValue.equalsIgnoreCase(vnpSecureHash);
    }

    /**
     * Hash tất cả các fields để tạo signature
     */
    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        // Sắp xếp theo alphabet
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);

            if (fieldValue != null && !fieldValue.isEmpty()) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(fieldValue);
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }

        return hmacSHA512(secretKey, sb.toString());
    }

    /**
     * Tạo thời gian theo format VNPay (yyyyMMddHHmmss)
     */
    public static String getCurrentTimeString() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return formatter.format(calendar.getTime());
    }

    /**
     * Tạo expire time (15 phút sau thời điểm hiện tại)
     */
    public static String getExpireTimeString() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        calendar.add(Calendar.MINUTE, 15);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return formatter.format(calendar.getTime());
    }

    /**
     * Tạo query string từ Map parameters với encode đúng chuẩn
     */
    public static String buildQueryString(Map<String, String> params, boolean encode) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder query = new StringBuilder();
        boolean first = true;

        // Sắp xếp theo alphabet
        Map<String, String> sortedParams = new TreeMap<>(params);

        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value != null && !value.isEmpty()) {
                if (!first) {
                    query.append("&");
                }

                if (encode) {
                    try {
                        query.append(URLEncoder.encode(key, StandardCharsets.UTF_8.toString()));
                        query.append("=");
                        query.append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()));
                    } catch (UnsupportedEncodingException e) {
                        query.append(key).append("=").append(value);
                    }
                } else {
                    query.append(key).append("=").append(value);
                }
                first = false;
            }
        }

        return query.toString();
    }
}
