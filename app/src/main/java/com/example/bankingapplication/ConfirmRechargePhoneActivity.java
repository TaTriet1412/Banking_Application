package com.example.bankingapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;

import com.chaos.view.PinView; // Import PinView
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.R; // Replace with your actual R file
import com.example.bankingapplication.Utils.EmailUtils;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.example.bankingapplication.Utils.VnPayUtils;

import java.util.Locale;

public class ConfirmRechargePhoneActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmRechargePhone";
    private Button btnConfirmMain;
    private TextView tvSourceAccount, tvPhoneNumber, tvTransactionFee, tvAmount, tvAmountInWords, tvAuthMethod;

    private AlertDialog otpDialog;
    private PinView pinViewOtp;
    private TextView tvOtpMessage, tvResendOtp, tvCarrier;
    private Button btnVerifyOtp;
    private ImageButton btnCloseDialog;

    private CountDownTimer resendOtpTimer;
    private CountDownTimer otpExpiryTimer;

    private String userEmail = ""; // Example email
    private final long OTP_EXPIRY_DURATION = 2 * 60 * 1000; // 2 minutes in milliseconds

    private int[] resendDelaysSeconds = {15, 20, 30, 50, 90}; // Sequence: 15s, then 15+5, then 20+10, then 30+20, then 50+40
    private int currentResendAttempt = 0;
    private boolean isOtpExpired = false;
    String amount = "";
    private String currentOtp;
    private long otpExpiryTimestamp; // thời gian OTP hết hạn tính bằng milliseconds


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_recharge_phone);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views from main layout
        tvSourceAccount = findViewById(R.id.tvSourceAccount);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvTransactionFee = findViewById(R.id.tvTransactionFee);
        tvAmount = findViewById(R.id.tvAmount);
        tvAuthMethod = findViewById(R.id.tvAuthMethod);
        btnConfirmMain = findViewById(R.id.btnConfirm);
        tvCarrier = findViewById(R.id.tvCarrier);


//        // Nhận dữ liệu từ Intent
//        Intent intent = getIntent();
//        String phoneNumber = intent.getStringExtra("phoneNumber");
//        amount = intent.getStringExtra("amount");
//        tvCarrier.setText(intent.getStringExtra("carrier")); // nếu có truyền thêm carrier
//
//        // Populate data (in a real app, this would come from previous activity/API)
//        User user = GlobalVariables.getInstance().getCurrentUser();
//        Account account = GlobalVariables.getInstance().getCurrentAccount();
//        tvSourceAccount.setText(account.getAccountNumber());
//        tvPhoneNumber.setText(phoneNumber);
//        tvAmount.setText(NumberFormat.convertToCurrencyFormatOnlyNumber(Integer.parseInt(amount)));
//        userEmail = user.getEmail();

//        Demo
        String phoneNumber = "0908871318";
        amount = "1000000"; // ví dụ số tiền nạp
        String carrier = "Viettel";
        tvCarrier.setText(carrier); // nếu có truyền thêm carrier
        tvSourceAccount.setText("123456789"); // ví dụ số tài khoản
        tvPhoneNumber.setText(phoneNumber);
        tvAmount.setText(NumberFormat.convertToCurrencyFormatOnlyNumber(Integer.parseInt(amount)));
        userEmail = "triettrinhthinh@gmail.com"; // ví dụ email người dùng




        btnConfirmMain.setOnClickListener(v -> showOtpDialog());
    }

    private void showOtpDialog() {
        if (otpDialog != null && otpDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_otp_verification, null);
        builder.setView(dialogView);
        builder.setCancelable(false); // User must interact with dialog

        otpDialog = builder.create();
        if (otpDialog.getWindow() != null) {
            otpDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        tvOtpMessage = dialogView.findViewById(R.id.tvOtpMessage);
        pinViewOtp = dialogView.findViewById(R.id.pinViewOtp);
        tvResendOtp = dialogView.findViewById(R.id.tvResendOtp);
        btnVerifyOtp = dialogView.findViewById(R.id.btnVerifyOtp);
        btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);

        tvOtpMessage.setText("Nhập mã OTP đã được gửi đến email của bạn: " + userEmail.replaceAll("(?<=.{2}).(?=.*@)", "*"));

        pinViewOtp.requestFocus(); // Request focus for PinView

        pinViewOtp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnVerifyOtp.setEnabled(s.length() == pinViewOtp.getItemCount());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        btnVerifyOtp.setEnabled(false); // Initially disable until 6 digits are entered


        btnCloseDialog.setOnClickListener(v -> {
            cancelTimers();
            otpDialog.dismiss();
        });

        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
        tvResendOtp.setOnClickListener(v -> handleResendOtp());

        sendNewOtp(); // Send OTP and start timers
        otpDialog.show();
    }

    private void sendNewOtp() {
        isOtpExpired = false;

        // Sinh mã OTP
        currentOtp = generateRandomOtp();
        otpExpiryTimestamp = System.currentTimeMillis() + OTP_EXPIRY_DURATION;

        // Tính giờ hết hạn (ví dụ: 14:35)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        String expiryTimeFormatted = sdf.format(new java.util.Date(otpExpiryTimestamp));

        // Nội dung email
        String subject = "Mã OTP của bạn";
        String body = "Mã OTP của bạn là: " + currentOtp
                + "\nHiệu lực đến: " + expiryTimeFormatted + " (trong vòng 2 phút).";

        EmailUtils.sendEmail(userEmail, subject, body);

        Toast.makeText(this, "Mã OTP đã gửi đến email: " + userEmail, Toast.LENGTH_SHORT).show();

        // Reset giao diện
        if (pinViewOtp != null) {
            new Handler(Looper.getMainLooper()).post(() -> pinViewOtp.setText(""));
        }
        if (btnVerifyOtp != null) btnVerifyOtp.setEnabled(false);

        startOtpExpiryTimer();
        startResendOtpCooldown();
    }




    private void handleResendOtp() {
        if (tvResendOtp.isEnabled()) {
            cancelTimers(); // Cancel existing timers before sending new OTP
            currentResendAttempt++;
            sendNewOtp();
        }
    }

    private void startResendOtpCooldown() {
        tvResendOtp.setEnabled(false);
        long delayMillis = getCurrentResendDelay() * 1000L;

        if (resendOtpTimer != null) {
            resendOtpTimer.cancel();
        }
        resendOtpTimer = new CountDownTimer(delayMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendOtp.setText(String.format(Locale.getDefault(), "Gửi lại (%ds)", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                tvResendOtp.setText("Gửi lại");
                tvResendOtp.setEnabled(true);
            }
        }.start();
    }

    private String generateRandomOtp() {
        int otp = 100000 + new java.util.Random().nextInt(900000); // random từ 100000 -> 999999
        return String.valueOf(otp);
    }


    private long getCurrentResendDelay() {
        int index = Math.min(currentResendAttempt, resendDelaysSeconds.length - 1);
        return resendDelaysSeconds[index];
    }

    private void startOtpExpiryTimer() {
        if (otpExpiryTimer != null) {
            otpExpiryTimer.cancel();
        }
        otpExpiryTimer = new CountDownTimer(OTP_EXPIRY_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // You could show a countdown for OTP expiry if needed
            }

            @Override
            public void onFinish() {
                isOtpExpired = true;
                if (otpDialog != null && otpDialog.isShowing()) {
                    Toast.makeText(ConfirmRechargePhoneActivity.this, getString(R.string.otp_expired), Toast.LENGTH_LONG).show();
                    btnVerifyOtp.setEnabled(false); // Disable verification if OTP expired
                    // Optionally, clear OTP fields
                    if(pinViewOtp != null) pinViewOtp.setText("");
                }
            }
        }.start();
    }


    private void verifyOtp() {
        String enteredOtp = pinViewOtp.getText().toString();

        // Kiểm tra hết hạn theo thời gian thực
        if (System.currentTimeMillis() > otpExpiryTimestamp) {
            isOtpExpired = true;
            Toast.makeText(this, getString(R.string.otp_expired), Toast.LENGTH_SHORT).show();
            if(pinViewOtp != null) pinViewOtp.setText("");
            btnVerifyOtp.setEnabled(false);
            return;
        }

        if (enteredOtp.equals(currentOtp)) {
            Toast.makeText(this, getString(R.string.otp_verification_success), Toast.LENGTH_SHORT).show();
            cancelTimers();
            // Xử lý logic xác thực thành công ở đây
            String orderId = String.valueOf("Transaction_" + System.currentTimeMillis()); // Mã đơn hàng duy nhất
            String paymentUrl = VnPayUtils.createPaymentUrl(Integer.parseInt(amount), "Nạp tiền điện thoại", orderId);
            Log.d("VNPAY_URL", "Payment URL: " + paymentUrl);

            launchVnPayUrl(paymentUrl);
            if (otpDialog != null && otpDialog.isShowing()) {
                otpDialog.dismiss();
            }
            // Bạn có thể xử lý logic tiếp theo ở đây
        } else {
            Toast.makeText(this, getString(R.string.otp_verification_failed), Toast.LENGTH_SHORT).show();
            if(pinViewOtp != null) pinViewOtp.setText(""); // Clear OTP on failure
        }
    }

    private void launchVnPayUrl(String paymentUrl) {
        try {
            // Log payment URL for debugging
            Log.d(TAG, "Launching VNPAY URL with WebViewPaymentActivity: " + paymentUrl);
            
            // Create intent for WebViewPaymentActivity
            Intent intent = new Intent(this, WebViewPaymentActivity.class);
            
            // Pass the payment URL to the WebViewPaymentActivity
            intent.putExtra("paymentUrl", paymentUrl);
            
            // Optionally pass transaction details
            intent.putExtra("transactionType", "PHONE_RECHARGE");
            intent.putExtra("amount", amount);
            intent.putExtra("phoneNumber", tvPhoneNumber.getText().toString());
            
            // Start the WebViewPaymentActivity
            startActivity(intent);
            
            Log.d(TAG, "Successfully launched WebViewPaymentActivity");
        } catch (Exception e) {
            Log.e(TAG, "Error launching WebViewPaymentActivity", e);
            Toast.makeText(this, "Không thể mở trang thanh toán. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
        }
    }


    private void cancelTimers() {
        if (resendOtpTimer != null) {
            resendOtpTimer.cancel();
        }
        if (otpExpiryTimer != null) {
            otpExpiryTimer.cancel();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimers();
        if (otpDialog != null && otpDialog.isShowing()) {
            otpDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        if (otpDialog != null && otpDialog.isShowing()) {
            // Let user close dialog with X button or explicitly handle back press for dialog
            Toast.makeText(this, "Please complete or cancel OTP verification.", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }
}