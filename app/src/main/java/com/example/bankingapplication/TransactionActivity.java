package com.example.bankingapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View; // <<<<<< THÊM IMPORT NÀY
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout; // <<<<<< THÊM IMPORT NÀY

public class TransactionActivity extends AppCompatActivity {

    TextInputEditText etRecipientAccount, etRecipientName, etAmount, etDescription;
    TextInputLayout tilRecipientName; // <<<<<< KHAI BÁO TIL CHO TÊN NGƯỜI NHẬN
    MaterialButton btnContinue;
    MaterialToolbar toolbar;

    TextView tvSourceAccountNumber, tvBalanceAmount;
    private static final String TARGET_ACCOUNT_NUMBER = "0364123957";
    private static final String TARGET_RECIPIENT_NAME = "THAN QUOC THINH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transaction);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, v.getPaddingTop(), systemBars.right, systemBars.bottom);
            return insets;
        });
        tvSourceAccountNumber = findViewById(R.id.tvSourceAccountNumber); // <<<<<< ÁNH XẠ
        tvBalanceAmount = findViewById(R.id.tvBalanceAmount);
        etRecipientAccount = findViewById(R.id.etRecipientAccount);
        tilRecipientName = findViewById(R.id.tilRecipientName); // <<<<<< ÁNH XẠ TEXTINPUTLAYOUT
        etRecipientName = findViewById(R.id.etRecipientName);
        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        btnContinue = findViewById(R.id.btnContinue);

        etRecipientAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String accountNumber = s.toString().trim();
                if (accountNumber.equals(TARGET_ACCOUNT_NUMBER)) {
                    tilRecipientName.setVisibility(View.VISIBLE); // <<<<<< HIỆN TIL
                    etRecipientName.setText(TARGET_RECIPIENT_NAME);
                } else {
                    tilRecipientName.setVisibility(View.GONE); // <<<<<< ẨN TIL
                    etRecipientName.setText(""); // Xóa tên để đảm bảo
                }
            }
        });

        btnContinue.setOnClickListener(v_continue -> {
            String recipientAccount = etRecipientAccount.getText().toString().trim();
            // String recipientName = etRecipientName.getText().toString().trim(); // Không cần lấy trực tiếp nếu đã ẩn/hiện
            String amount = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (recipientAccount.isEmpty()) {
                etRecipientAccount.setError("Vui lòng nhập số tài khoản người nhận");
                etRecipientAccount.requestFocus();
                return;
            }

            // Kiểm tra xem số tài khoản có khớp với số mục tiêu không
            // Nếu không khớp, tên người nhận sẽ không hiển thị và không nên cho phép giao dịch
            if (!recipientAccount.equals(TARGET_ACCOUNT_NUMBER)) {
                Toast.makeText(TransactionActivity.this, "Số tài khoản không hợp lệ", Toast.LENGTH_SHORT).show();
                etRecipientAccount.requestFocus(); // Có thể focus lại ô số tài khoản
                return;
            }
            // Nếu đến đây, nghĩa là recipientAccount == TARGET_ACCOUNT_NUMBER
            // và tilRecipientName đã được đặt là VISIBLE và etRecipientName đã có tên.

            if (amount.isEmpty()) {
                etAmount.setError("Vui lòng nhập số tiền");
                etAmount.requestFocus();
                return;
            }

            if (description.isEmpty()) {
                etDescription.setError("Vui lòng nhập nội dung chuyển tiền");
                etDescription.requestFocus();
                return;
            }

            Toast.makeText(TransactionActivity.this, "Đang xử lý chuyển tiền cho " + TARGET_RECIPIENT_NAME + "...", Toast.LENGTH_SHORT).show();
            // Thêm logic xử lý chuyển tiền
            Intent confirmIntent = new Intent(TransactionActivity.this, ConfirmTransactionActivity.class);
            confirmIntent.putExtra("SOURCE_ACCOUNT", tvSourceAccountNumber.getText().toString()); // Lấy từ TextView tài khoản nguồn
            // confirmIntent.putExtra("SOURCE_BALANCE", tvBalanceAmount.getText().toString()); // Có thể truyền số dư nếu cần
            confirmIntent.putExtra("RECIPIENT_ACCOUNT", recipientAccount);
            confirmIntent.putExtra("RECIPIENT_NAME", TARGET_RECIPIENT_NAME); // Hoặc etRecipientName.getText().toString()
            confirmIntent.putExtra("RECIPIENT_BANK_NAME", "3T Banking"); // Tên ngân hàng nhận (hiện tại đang là static)
            confirmIntent.putExtra("CONTENT", description);
            confirmIntent.putExtra("AMOUNT", amount);
            startActivity(confirmIntent);
        });
    }
}