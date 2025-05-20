package com.example.bankingapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PaymentReturnActivity extends AppCompatActivity {

    private static final String TAG = "PaymentReturnActivity";
    private TextView tvResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_return);

        Log.d(TAG, "PaymentReturnActivity onCreate called");

        tvResult = findViewById(R.id.tvResult);

        Intent intent = getIntent();
        Uri data = intent.getData();

        Log.d(TAG, "Intent: " + intent);
        Log.d(TAG, "Intent action: " + (intent.getAction() != null ? intent.getAction() : "null"));
        Log.d(TAG, "Intent data: " + (data != null ? data.toString() : "null"));

        if (data != null) {
            Log.d(TAG, "URI received: " + data.toString());
            StringBuilder resultText = new StringBuilder("Kết quả thanh toán VNPAY:\n");

            // Lấy các tham số từ query string
            Map<String, String> params = new HashMap<>();
            Set<String> queryParameterNames = data.getQueryParameterNames();
            for (String key : queryParameterNames) {
                String value = data.getQueryParameter(key);
                params.put(key, value);
                resultText.append(key).append(": ").append(value).append("\n");
                Log.d(TAG, "Param: " + key + " = " + value);
            }

            tvResult.setText(resultText.toString());

            // Lấy các thông tin quan trọng
            String responseCode = params.get("vnp_ResponseCode");
            String transactionNo = params.get("vnp_TransactionNo"); // Mã giao dịch của VNPAY
            String amount = params.get("vnp_Amount"); // Số tiền, chia 100 để ra VNĐ
            String orderInfo = params.get("vnp_OrderInfo");
            String txnRef = params.get("vnp_TxnRef"); // Mã đơn hàng của bạn
            String secureHash = params.get("vnp_SecureHash");

            // TODO: Xác thực vnp_SecureHash (RẤT QUAN TRỌNG)
            // Việc này nên được thực hiện ở backend để đảm bảo an toàn.
            // Nếu làm ở client, cần có logic tương tự VNPAYUtils.createPaymentUrl
            // để tạo hash từ các tham số trả về (trừ vnp_SecureHashType và vnp_SecureHash)
            // rồi so sánh.
            // Ví dụ: boolean isValidSignature = verifySecureHash(params, VNPAYUtils.vnp_HashSecret);

            if ("00".equals(responseCode)) {
                Toast.makeText(this, "Thanh toán thành công! Mã GD VNPAY: " + transactionNo, Toast.LENGTH_LONG).show();
                // TODO: Xử lý logic khi thanh toán thành công
                // Ví dụ: Cập nhật trạng thái đơn hàng, thông báo cho người dùng,...
            } else {
                Toast.makeText(this, "Thanh toán thất bại. Mã lỗi: " + responseCode, Toast.LENGTH_LONG).show();
                // TODO: Xử lý logic khi thanh toán thất bại
            }
        } else {
            Log.e(TAG, "No data received in intent.");
            tvResult.setText("Không nhận được dữ liệu trả về từ VNPAY.");
        }

    }

    // Hàm ví dụ để xác thực chữ ký (nên làm ở server)
    // Cần điều chỉnh cho đúng với cách VNPAY yêu cầu tạo hash cho response
    private boolean verifySecureHash(Map<String, String> params, String secretKey) {
        // Lấy vnp_SecureHash từ params
        String inputHash = params.get("vnp_SecureHash");
        if (inputHash == null) return false;

        // Loại bỏ vnp_SecureHash và vnp_SecureHashType (nếu có) ra khỏi params để tạo hash
        Map<String, String> fieldsToHash = new HashMap<>(params);
        fieldsToHash.remove("vnp_SecureHash");
        fieldsToHash.remove("vnp_SecureHashType"); // Thường VNPAY không trả về cái này nhưng kiểm tra cho chắc

        // Sắp xếp các trường theo thứ tự alphabet và tạo chuỗi hashData
        // (Logic tương tự như khi tạo URL thanh toán)
        // ... (Bạn cần tự implement logic này dựa trên tài liệu VNPAY về cách tạo hash cho response)
        // String generatedHash = VNPAYUtils.hmacSHA512(secretKey, hashDataString);

        // return inputHash.equalsIgnoreCase(generatedHash);
        Log.w(TAG, "verifySecureHash is not fully implemented for client-side. This should be done on SERVER SIDE.");
        return true; // Tạm thời trả về true cho demo, CẦN IMPLEMENT ĐÚNG
    }
}