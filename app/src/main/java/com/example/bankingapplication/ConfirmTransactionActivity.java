package com.example.bankingapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.MaterialToolbar;

public class ConfirmTransactionActivity extends AppCompatActivity {

    // Views for included layouts
    View rowTransferType, rowSourceAccount, rowRecipientAccount,
            rowRecipientName, rowRecipientBank, rowContent, // rowRecipientBank giờ là View đơn giản
            rowTransferFee, rowAmountView;

    MaterialToolbar toolbar;
    Button btnConfirmTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm_transaction);

        toolbar = findViewById(R.id.toolbar_confirm);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_confirm), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ánh xạ các view
        rowTransferType = findViewById(R.id.row_transfer_type);
        rowSourceAccount = findViewById(R.id.row_source_account);
        rowRecipientAccount = findViewById(R.id.row_recipient_account);
        rowRecipientName = findViewById(R.id.row_recipient_name);
        rowRecipientBank = findViewById(R.id.row_recipient_bank);
        rowContent = findViewById(R.id.row_content);
        rowTransferFee = findViewById(R.id.row_transfer_fee);
        rowAmountView = findViewById(R.id.row_amount);
        btnConfirmTransaction = findViewById(R.id.btn_confirm_transaction);

        // Nhận dữ liệu từ Intent
        Intent intent = getIntent();
        String sourceAccount = intent.getStringExtra("SOURCE_ACCOUNT");
        // String sourceBalance = intent.getStringExtra("SOURCE_BALANCE"); // Có thể không cần hiển thị lại số dư nguồn
        String recipientAccount = intent.getStringExtra("RECIPIENT_ACCOUNT");
        String recipientName = intent.getStringExtra("RECIPIENT_NAME");
        String recipientBankName = intent.getStringExtra("RECIPIENT_BANK_NAME"); // Ví dụ: "3T Banking"
        String content = intent.getStringExtra("CONTENT");
        String amount = intent.getStringExtra("AMOUNT");
        // Bạn có thể cần hàm chuyển đổi số tiền thành chữ (ví dụ: 2,000 -> Hai nghìn đồng)

        // Hiển thị dữ liệu lên các TextView
        setInfoRow(rowTransferType, "Hình thức chuyển", "Chuyển tiền nhanh 24/7"); // Hoặc lấy từ intent nếu có
        setInfoRow(rowSourceAccount, "Tài khoản nguồn", sourceAccount);
        setInfoRow(rowRecipientAccount, "Tài khoản/thẻ nhận", recipientAccount);
        int redColor = ContextCompat.getColor(this, android.R.color.holo_red_dark);
        setInfoRowColor(rowRecipientName, "Tên người nhận", recipientName, redColor);
        setInfoRow(rowRecipientBank, "Ngân hàng nhận", recipientBankName);
        setInfoRow(rowContent, "Nội dung", content);
        setInfoRow(rowTransferFee, "Phí chuyển tiền", "Miễn phí"); // Hoặc lấy từ intent nếu có
        setAmountInfoRow(rowAmountView, "Số tiền", amount + " VND", convertNumberToWords(amount) + " đồng"); // Hàm convertNumberToWords cần tự viết


        btnConfirmTransaction.setOnClickListener(v -> {
            // Xử lý khi nhấn Xác nhận -> Yêu cầu nhập mã PIN
            // Hiện tại chỉ Toast
            Toast.makeText(ConfirmTransactionActivity.this, "Yêu cầu nhập mã PIN...", Toast.LENGTH_SHORT).show();
            // Trong thực tế, bạn sẽ mở một Dialog hoặc Activity mới để nhập PIN
            // Ví dụ: showPinInputDialog();
        });
    }

    private void setInfoRow(View rowView, String label, String value) {
        TextView tvLabel = rowView.findViewById(R.id.tv_label);
        TextView tvValue = rowView.findViewById(R.id.tv_value);
        tvLabel.setText(label);
        tvValue.setText(value);
    }

    private void setInfoRowColor(View rowView, String label, String value, int color) {
        TextView tvLabel = rowView.findViewById(R.id.tv_label);
        TextView tvValue = rowView.findViewById(R.id.tv_value);
        tvLabel.setText(label);
        tvValue.setText(value);
        tvValue.setTextColor(color);
    }

//    private void setBankInfoRow(View rowView, String label, String bankShortName, String bankFullName) {
//        TextView tvLabel = rowView.findViewById(R.id.tv_label_bank);
//        ImageView ivBankLogo = rowView.findViewById(R.id.iv_bank_logo);
//        TextView tvBankShort = rowView.findViewById(R.id.tv_bank_name_short);
//        TextView tvBankFull = rowView.findViewById(R.id.tv_bank_name_full);
//
//        tvLabel.setText(label);
//        // ivBankLogo.setImageResource(...); // Đặt logo ngân hàng nếu có
//        tvBankShort.setText(bankShortName);
//        tvBankFull.setText(bankFullName);
//    }

    private void setAmountInfoRow(View rowView, String label, String amountValue, String amountText) {
        TextView tvLabel = rowView.findViewById(R.id.tv_label_amount);
        TextView tvValue = rowView.findViewById(R.id.tv_value_amount);
        TextView tvText = rowView.findViewById(R.id.tv_value_amount_text);

        tvLabel.setText(label);
        tvValue.setText(amountValue);
        tvText.setText("(" + amountText + ")");
    }

    // Hàm ví dụ đơn giản để chuyển số thành chữ (cần hoàn thiện nhiều)
    private String convertNumberToWords(String numberStr) {
        if (numberStr == null || numberStr.isEmpty()) return "";
        try {
            // Loại bỏ dấu phẩy nếu có (ví dụ "2,000" -> "2000")
            String cleanNumberStr = numberStr.replaceAll(",", "");
            long number = Long.parseLong(cleanNumberStr);

            if (number == 0) return "Không";
            if (number == 2000) return "Hai nghìn";
            // ... thêm các trường hợp khác hoặc sử dụng thư viện
            return String.valueOf(number); // Trả về số nếu không có quy tắc
        } catch (NumberFormatException e) {
            return numberStr; // Trả về chuỗi gốc nếu không parse được
        }
    }
}