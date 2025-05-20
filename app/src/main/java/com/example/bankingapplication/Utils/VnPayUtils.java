package com.example.bankingapplication.Utils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VnPayUtils {

    // TODO: THAY THẾ BẰNG THÔNG TIN TEST CỦA BẠN
    // Đây là thông tin test mặc định, bạn cần thay bằng thông tin của mình
    public static String vnp_TmnCode = "CGXZLS0Z"; // Thay bằng TmnCode của bạn
    public static String vnp_HashSecret = "XNBCJFAKAZQSGTARRLGCHVZWCIOIGSHN"; // Thay bằng HashSecret của bạn
    public static String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_ReturnUrl = "myapp://vnpayresponse"; // Quan trọng: schema và host này phải giống trong Intent Filter

    //Hàm tạo checksum
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();

        } catch (Exception ex) {
            return "";
        }
    }

    public static String getIpAddress() {
        // Logic lấy IP Address, ví dụ đơn giản.
        // Trong thực tế, bạn có thể cần một giải pháp phức tạp hơn để lấy IP public
        // hoặc nếu app của bạn gọi qua server thì server sẽ lấy IP của client.
        // Vì demo client-side, ta có thể hardcode hoặc bỏ qua tham số này nếu VNPAY cho phép
        // (thường là không).
        // VNPAY yêu cầu vnp_IpAddr là IP của khách hàng.
        // Nếu bạn tạo URL ở client, bạn có thể thử dùng 1.1.1.1 hoặc IP của thiết bị.
        // Nếu tạo URL ở server, server sẽ lấy IP của request từ client.
        return "127.0.0.1"; // Ví dụ, nên lấy IP thực của thiết bị
    }

    public static String createPaymentUrl(long amount, String orderInfo, String orderId) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        // String vnp_OrderInfo = "Thanh toan don hang " + orderId; // Mô tả đơn hàng
        String vnp_OrderType = "other"; // Loại hàng hóa, xem thêm tài liệu VNPAY
        String vnp_TxnRef = orderId; // Mã tham chiếu của giao dịch tại hệ thống của merchant. Mã này là duy nhất dùng để phân biệt các đơn hàng gửi sang VNPAY. Không được trùng lặp trong ngày.
        String vnp_IpAddr = getIpAddress(); // Lấy IP Address
        String vnp_TmnCode = VnPayUtils.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // Số tiền thanh toán, nhân 100 theo quy định VNPAY
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", "vn"); // Ngôn ngữ giao diện thanh toán, vn: Tiếng Việt, en: Tiếng Anh
        vnp_Params.put("vnp_ReturnUrl", VnPayUtils.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15); // Thời gian hết hạn thanh toán
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build data to hash
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VnPayUtils.hmacSHA512(VnPayUtils.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return VnPayUtils.vnp_Url + "?" + queryUrl;
    }
}
