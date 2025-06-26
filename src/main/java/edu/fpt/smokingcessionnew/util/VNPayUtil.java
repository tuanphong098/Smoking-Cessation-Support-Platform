package edu.fpt.smokingcessionnew.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Lớp tiện ích cho VNPay, chứa các phương thức xử lý thanh toán
 * Dựa trên code mẫu do VNPay cung cấp
 */
public class VNPayUtil {

    /**
     * Tạo chuỗi hash dữ liệu SHA256 theo đúng yêu cầu của VNPay
     */
    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        // Sắp xếp theo thứ tự từ điển
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        // Tạo chuỗi hash
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName).append("=").append(fieldValue);
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }

        return hmacSHA512(secretKey, sb.toString());
    }

    /**
     * Tạo chuỗi HMAC_SHA512 theo đúng tiêu chuẩn VNPay
     */
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA512");
            final javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac.init(secretKey);
            final byte[] hmacData = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacData);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * Chuyển byte array sang chuỗi hex
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Lấy IP của người dùng từ request
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress.equals("0:0:0:0:0:0:0:1")) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
    }

    /**
     * Tạo mã giao dịch duy nhất
     */
    public static String getTransactionRef() {
        return String.valueOf(System.currentTimeMillis()) + getRandomNumber(8);
    }

    /**
     * Tạo chuỗi số ngẫu nhiên với độ dài cho trước
     */
    public static String getRandomNumber(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Tạo URL thanh toán từ các tham số
     */
    public static String createPaymentUrl(
            String vnpPayUrl, String vnpVersion, String vnpCommand,
            String vnpTmnCode, String vnpAmount, String vnpBankCode,
            String vnpOrderInfo, String vnpTxnRef, String vnpIpAddr,
            String vnpCurrCode, String vnpLocale, String vnpReturnUrl,
            String vnpHashSecret) {
        try {
            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", vnpVersion);
            vnpParams.put("vnp_Command", vnpCommand);
            vnpParams.put("vnp_TmnCode", vnpTmnCode);
            vnpParams.put("vnp_Amount", vnpAmount);
            vnpParams.put("vnp_CurrCode", vnpCurrCode);

            // Thêm bankCode nếu có
            if (vnpBankCode != null && !vnpBankCode.isEmpty()) {
                vnpParams.put("vnp_BankCode", vnpBankCode);
            }

            vnpParams.put("vnp_TxnRef", vnpTxnRef);
            vnpParams.put("vnp_OrderInfo", vnpOrderInfo);
            vnpParams.put("vnp_OrderType", "190000"); // Mã danh mục hàng hóa
            vnpParams.put("vnp_Locale", vnpLocale);
            vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
            vnpParams.put("vnp_IpAddr", vnpIpAddr);

            // Tạo thời gian thanh toán
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnpCreateDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_CreateDate", vnpCreateDate);

            // Tạo thời gian hết hạn
            cld.add(Calendar.MINUTE, 15);
            String vnpExpireDate = formatter.format(cld.getTime());
            vnpParams.put("vnp_ExpireDate", vnpExpireDate);

            // Tạo chuỗi hash và query
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnpParams.get(fieldName);
                if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                    // Build hash data (không URL encode)
                    hashData.append(fieldName).append("=").append(fieldValue);

                    // Build query (URL encode)
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                            .append("=")
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                    if (itr.hasNext()) {
                        hashData.append("&");
                        query.append("&");
                    }
                }
            }

            // Tạo signature
            String vnpSecureHash = hmacSHA512(vnpHashSecret, hashData.toString());
            query.append("&vnp_SecureHash=").append(vnpSecureHash);

            return vnpPayUrl + "?" + query;

        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * Kiểm tra chữ ký trong kết quả trả về từ VNPay
     */
    public static boolean verifyPaymentReturn(Map<String, String> vnpParams, String secretKey) {
        String vnpSecureHash = vnpParams.get("vnp_SecureHash");
        if (vnpSecureHash == null) {
            return false;
        }

        // Xóa các tham số không cần thiết
        Map<String, String> validParams = new HashMap<>();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            if (!entry.getKey().equals("vnp_SecureHash") && !entry.getKey().equals("vnp_SecureHashType")) {
                validParams.put(entry.getKey(), entry.getValue());
            }
        }

        // Tạo chữ ký và so sánh
        String checkHash = hashAllFields(validParams, secretKey);
        return checkHash.equals(vnpSecureHash);
    }
}
