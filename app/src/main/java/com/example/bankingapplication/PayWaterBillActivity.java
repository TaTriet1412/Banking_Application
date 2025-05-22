package com.example.bankingapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class PayWaterBillActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvSourceAccountNumber, tvSourceAccountBalance;
    private ImageView ivToggleBalance;
    private AutoCompleteTextView actvProvider;
    private TextInputLayout tilBillCode;
    private TextInputEditText tietBillCode;
    private AppCompatButton btnContinue;
    private ProgressBar pbLoading;

    private Account currentAccount;
    private boolean isBalanceVisible = false;
    private static final String TAG = "PayWaterBillActivity";

    private final String[] WATER_PROVIDERS = new String[]{
            "WaterCo Miền Bắc", "WaterCo Miền Trung", "WaterCo Miền Nam" // Thay bằng danh sách nhà cung cấp nước thực tế
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) this).setContentView(R.layout.activity_pay_water_bill);

        currentAccount = GlobalVariables.getInstance().getCurrentAccount();
        if (currentAccount == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin tài khoản.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar_pay_water);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tvSourceAccountNumber = findViewById(R.id.tv_source_account_number);
        tvSourceAccountBalance = findViewById(R.id.tv_source_account_balance);
        ivToggleBalance = findViewById(R.id.iv_toggle_balance_water);
        actvProvider = findViewById(R.id.actv_provider);
        tilBillCode = findViewById(R.id.til_bill_code);
        tietBillCode = findViewById(R.id.tiet_bill_code);
        btnContinue = findViewById(R.id.btn_continue_water_payment);
        pbLoading = findViewById(R.id.pb_loading_bill_check);

        setupSourceAccountInfo();
        setupProviderDropdown();
        setupClickListeners();
    }

    @SuppressLint("SetTextI18n")
    private void setupSourceAccountInfo() {
        tvSourceAccountNumber.setText(currentAccount.getAccountNumber());
        toggleBalanceDisplay();
    }

    private void setupProviderDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, WATER_PROVIDERS);
        actvProvider.setAdapter(adapter);
    }

    private void setupClickListeners() {
        ivToggleBalance.setOnClickListener(v -> {
            isBalanceVisible = !isBalanceVisible;
            toggleBalanceDisplay();
        });

        btnContinue.setOnClickListener(v -> {
            String provider = actvProvider.getText().toString().trim();
            String billCode = tietBillCode.getText().toString().trim();

            if (provider.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn nhà cung cấp", Toast.LENGTH_SHORT).show();
                actvProvider.requestFocus();
                return;
            }
            if (billCode.isEmpty()) {
                tilBillCode.setError("Vui lòng nhập mã hóa đơn");
                tietBillCode.requestFocus();
                return;
            } else {
                tilBillCode.setError(null);
            }

            checkBillWithFirestoreClass(provider, billCode);
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            pbLoading.setVisibility(View.VISIBLE);
            btnContinue.setEnabled(false);
        } else {
            pbLoading.setVisibility(View.GONE);
            btnContinue.setEnabled(true);
        }
    }

    private void checkBillWithFirestoreClass(String provider, String billCode) {
        showLoading(true);
        final String billType = "water";

        Firestore.checkBillExists(provider, billCode, billType, (billExists, e) -> {
            showLoading(false);
            if (e != null) {
                Log.e(TAG, "Error checking bill: ", e);
                Toast.makeText(PayWaterBillActivity.this, "Lỗi khi kiểm tra hóa đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (billExists) {
                Log.d(TAG, "Bill found for provider: " + provider + ", billCode: " + billCode + ", type: " + billType);
                proceedToConfirmation(provider, billCode, billType);
            } else {
                Log.w(TAG, "No bill found for provider: " + provider + ", billCode: " + billCode);
                Toast.makeText(PayWaterBillActivity.this, "Không tìm thấy hóa đơn hoặc thông tin không chính xác. Vui lòng kiểm tra lại.", Toast.LENGTH_LONG).show();
                tilBillCode.setError("Mã hóa đơn hoặc nhà cung cấp không đúng");
            }
        });
    }

    private void proceedToConfirmation(String provider, String billCode, String billType) {
        Intent intent = new Intent(PayWaterBillActivity.this, ConfirmWaterPaymentActivity.class);
        intent.putExtra(ConfirmWaterPaymentActivity.EXTRA_BILL_NUMBER, billCode);
        intent.putExtra(ConfirmWaterPaymentActivity.EXTRA_PROVIDER_NAME, provider);
        intent.putExtra(ConfirmWaterPaymentActivity.EXTRA_BILL_TYPE, billType);
        startActivity(intent);
    }

    @SuppressLint("SetTextI18n")
    private void toggleBalanceDisplay() {
        if (isBalanceVisible) {
            tvSourceAccountBalance.setText(NumberFormat.convertToCurrencyFormatHasUnit(currentAccount.getChecking().getBalance()));
            ivToggleBalance.setImageResource(R.drawable.ic_un_eye);
        } else {
            tvSourceAccountBalance.setText("•••••••• VND");
            ivToggleBalance.setImageResource(R.drawable.ic_eye);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}