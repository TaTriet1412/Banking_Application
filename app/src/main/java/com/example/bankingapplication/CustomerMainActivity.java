package com.example.bankingapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.bankingapplication.Firebase.FirebaseAuth;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;

public class CustomerMainActivity extends AppCompatActivity {
    ImageView iv_user_icon, iv_copy_account_number, iv_toggle_balance,
            iv_charge_phone, iv_customer_logout;

    LinearLayout ll_transaction_history, ll_account, ll_transfer_money_feature, ll_nearby_branches_feature, ll_pay_electricity_bill_feature, ll_pay_water_bill_feature;

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
        iv_customer_logout = findViewById(R.id.iv_customer_logout); // Add the new logout icon
        ll_transaction_history = findViewById(R.id.ll_transaction_history);
        ll_account = findViewById(R.id.ll_account);
        ll_transfer_money_feature = findViewById(R.id.ll_transfer_money_feature);
        tv_user_name = findViewById(R.id.tv_user_name);
        tv_account_number = findViewById(R.id.tv_account_number);
        tv_balance = findViewById(R.id.tv_balance);
        ll_nearby_branches_feature = findViewById(R.id.ll_nearby_branches_feature);
        ll_pay_electricity_bill_feature = findViewById(R.id.ll_pay_electricity_bill_feature);
        ll_pay_water_bill_feature = findViewById(R.id.ll_pay_water_bill_feature);

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
        accountClick();
        phoneClick();
        nearbyBranchesFeatureClick();
        payElectricityBillFeatureClick();
        payWaterBillFeatureClick();
        userIconClick(); // Add this new method call
        transactionHistoryClick();

        // Add logout icon click listener
        if (iv_customer_logout != null) {
            iv_customer_logout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    private void transactionHistoryClick() {
        ll_transaction_history.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerMainActivity.this, TransactionHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận đăng xuất");
        builder.setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản?");
        builder.setPositiveButton("Đăng xuất", (dialog, which) -> performLogout());
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void performLogout() {
        // Sign out from Firebase Auth
        FirebaseAuth.signOut();

        // Clear global variables
        GlobalVariables.getInstance().setCurrentUser(null);
        GlobalVariables.getInstance().setCurrentAccount(null);

        // Navigate to SignInActivity and clear all activities in stack
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @SuppressLint("SetTextI18n")
    private void toggleBalanceClick() {
        iv_toggle_balance.setOnClickListener(v -> {
            if (tv_balance.getText().toString().contains("*")) {
                tv_balance.setText(NumberFormat.convertToCurrencyFormatHasUnit(currentAccount.getChecking().getBalance()));
                iv_toggle_balance.setImageResource(R.drawable.ic_un_eye);
            } else {
                tv_balance.setText(R.string.pass_hint);
                iv_toggle_balance.setImageResource(R.drawable.ic_eye);
            }
        });
    }

    private void phoneClick() {
        iv_charge_phone.setOnClickListener(v -> {
            // Handle phone charge click
            Intent intent = new Intent(this, RechargePhoneActivity.class);
            startActivity(intent);
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

    private void accountClick() {
        ll_account.setOnClickListener(v -> {
            // Handle account click
            Intent intent = new Intent(this, AccountActivity.class);
            startActivity(intent);
        });
    }
    // <<<<<< THÊM PHƯƠNG THỨC XỬ LÝ CLICK CHO CHỨC NĂNG BẢN ĐỒ >>>>>>
    private void nearbyBranchesFeatureClick() {
        ll_nearby_branches_feature.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerMainActivity.this, NavigationActivity.class);
            startActivity(intent);
        });
    }
    private void payElectricityBillFeatureClick() {
        ll_pay_electricity_bill_feature.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerMainActivity.this, PayElectricityBillActivity.class);
            startActivity(intent);
        });
    }

    private void payWaterBillFeatureClick() {
        ll_pay_water_bill_feature.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerMainActivity.this, PayWaterBillActivity.class);
            startActivity(intent);
        });
    }

    // Add the new click handler method for user icon
    private void userIconClick() {
        iv_user_icon.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerMainActivity.this, EditUserProfileActivity.class);
            // Pass current user information to the edit activity
            if (currentUser != null) {
                intent.putExtra("USER_ID", currentUser.getUID());
                intent.putExtra("USER_NAME", currentUser.getName());
                intent.putExtra("USER_EMAIL", currentUser.getEmail());
                intent.putExtra("USER_PHONE", currentUser.getPhone());
                intent.putExtra("USER_ADDRESS", currentUser.getAddress());
                intent.putExtra("USER_NATIONAL_ID", currentUser.getNationalId());
                
                // Optional: if you want to pass date of birth and gender
                if (currentUser.getDateOfBirth() != null) {
                    intent.putExtra("USER_DOB_SECONDS", currentUser.getDateOfBirth().getSeconds());
                }
                if (currentUser.getGender() != null) {
                    intent.putExtra("USER_GENDER", currentUser.getGender());
                }
            }
            startActivity(intent);
        });
    }
}