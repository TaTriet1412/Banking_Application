package com.example.bankingapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import android.content.Intent;
public class CustomerMainActivity extends AppCompatActivity {
    ImageView iv_user_icon, iv_copy_account_number, iv_toggle_balance,
            iv_charge_phone;

    LinearLayout ll_transaction_history, ll_account, ll_transfer_money_feature;

    TextView tv_user_name, tv_account_number, tv_balance;
    FrameLayout progressOverlay;
    Account currentAccount;
    User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_main);
        progressOverlay = findViewById(R.id.progress_overlay);
        iv_user_icon = findViewById(R.id.iv_user_icon);
        iv_copy_account_number = findViewById(R.id.iv_copy_account_number);
        iv_toggle_balance = findViewById(R.id.iv_toggle_balance);
        iv_charge_phone = findViewById(R.id.iv_charge_phone);
//        iv_transfer_money = findViewById(R.id.iv_transfer_money);
        ll_transaction_history = findViewById(R.id.ll_transaction_history);
        ll_account = findViewById(R.id.ll_account);
        ll_transfer_money_feature = findViewById(R.id.ll_transfer_money_feature);
        tv_user_name = findViewById(R.id.tv_user_name);
        tv_account_number = findViewById(R.id.tv_account_number);
        tv_balance = findViewById(R.id.tv_balance);

        currentAccount = GlobalVariables.getInstance().getCurrentAccount();
        currentUser = GlobalVariables.getInstance().getCurrentUser();

        initViews();
        initClickListeners();
    }

    @SuppressLint("SetTextI18n")
    private void initViews() {
        tv_user_name.setText(currentUser.getName());
        tv_account_number.setText(currentAccount.getAccountNumber());
        tv_balance.setText(R.string.pass_hint);
    }

    private void initClickListeners() {
        toggleBalanceClick();
        copyAccountNumberClick();
        transferMoneyFeatureClick();
    }

    @SuppressLint("SetTextI18n")
    private void toggleBalanceClick() {
        iv_toggle_balance.setOnClickListener(v -> {
            if (tv_balance.getText().toString().equals(getString(R.string.pass_hint))) {
                tv_balance.setText(NumberFormat.convertToCurrencyFormat(currentAccount.getChecking().getBalance()));
                iv_toggle_balance.setImageResource(R.drawable.ic_un_eye);
            } else {
                tv_balance.setText(R.string.pass_hint);
                iv_toggle_balance.setImageResource(R.drawable.ic_eye);
            }
        });
    }

    private void copyAccountNumberClick() {
        iv_copy_account_number.setOnClickListener(v -> {
            // Copy account number to clipboard
            String accountNumber = currentAccount.getAccountNumber();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Account Number", accountNumber);
            clipboard.setPrimaryClip(clip);
            // Show a message to the user
            Toast.makeText(this, "Đã sao chép số tài khoản", Toast.LENGTH_SHORT).show();
        });
    }

    // >>>>>> THÊM PHƯƠNG THỨC NÀY <<<<<<
    private void transferMoneyFeatureClick() {
        ll_transfer_money_feature.setOnClickListener(v -> {
            // Tạo Intent để chuyển sang TransactionActivity
            Intent intent = new Intent(CustomerMainActivity.this, TransactionActivity.class);
            startActivity(intent);
        });
    }
}