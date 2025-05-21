package com.example.bankingapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.chaos.view.PinView;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.Bill;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.EmailUtils;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.example.bankingapplication.Utils.VnPayUtils;
import com.google.firebase.Timestamp;

import java.util.Locale;

public class ConfirmRechargePhoneActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmRechargePhone";
    private Button btnConfirmMain;
    private TextView tvSourceAccount, tvPhoneNumber, tvTransactionFee, tvAmount, tvAmountInWords, tvAuthMethod;

    // PIN Dialog
    private AlertDialog pinDialog;
    private PinView pinViewPin;
    private TextView tvPinMessage;
    private Button btnVerifyPin;
    private ImageButton btnClosePinDialog;
    private static final String DEFAULT_PIN = "123456"; // This should be from user settings or secure storage

    // OTP Dialog
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

        // Nhận dữ liệu từ Intent
        Intent intent = getIntent();
        String phoneNumber = intent.getStringExtra("phoneNumber");
        amount = intent.getStringExtra("amount");
        tvCarrier.setText(intent.getStringExtra("carrier")); // nếu có truyền thêm carrier

        // Get user and account data from GlobalVariables with null checks
        User user = GlobalVariables.getInstance().getCurrentUser();
        Account account = GlobalVariables.getInstance().getCurrentAccount();
        
        // Check if user and account data is available
        if (user == null || account == null) {
            // Handle the case when user or account data is missing
            Toast.makeText(this, "Không thể tải thông tin tài khoản. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            
            // Redirect to sign in activity or previous screen
            finish();
            Intent signInIntent = new Intent(this, SignInActivity.class);
            signInIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(signInIntent);
            return;
        }
        
        // If we get here, we have valid user and account objects
        tvSourceAccount.setText(account.getAccountNumber());
        tvPhoneNumber.setText(phoneNumber);
        tvAmount.setText(NumberFormat.convertToCurrencyFormatOnlyNumber(Integer.parseInt(amount)));
        userEmail = user.getEmail();

        btnConfirmMain.setOnClickListener(v -> showPinDialog());
    }

    private void showPinDialog() {
        if (pinDialog != null && pinDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pin_verification, null);
        builder.setView(dialogView);
        builder.setCancelable(false); // User must interact with dialog

        pinDialog = builder.create();
        if (pinDialog.getWindow() != null) {
            pinDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        tvPinMessage = dialogView.findViewById(R.id.tvPinMessage);
        pinViewPin = dialogView.findViewById(R.id.pinViewPin);
        btnVerifyPin = dialogView.findViewById(R.id.btnVerifyPin);
        btnClosePinDialog = dialogView.findViewById(R.id.btnCloseDialog);

        tvPinMessage.setText("Nhập mã PIN của bạn");

        // Make sure PinView is ready to accept input
        pinViewPin.requestFocus();
        
        // Show keyboard automatically
        if (getWindow() != null && getWindow().getContext() != null) {
            InputMethodManager imm = (InputMethodManager) getWindow().getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                // Post a delayed action to make sure the dialog is shown before trying to show the keyboard
                new Handler().postDelayed(() -> 
                    imm.showSoftInput(pinViewPin, InputMethodManager.SHOW_IMPLICIT), 
                    100);
            }
        }

        // Monitor input changes to enable/disable the verify button
        pinViewPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnVerifyPin.setEnabled(s.length() == pinViewPin.getItemCount());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        btnVerifyPin.setEnabled(false); // Initially disable until all digits are entered

        btnClosePinDialog.setOnClickListener(v -> pinDialog.dismiss());

        btnVerifyPin.setOnClickListener(v -> verifyPin());

        pinDialog.show();
    }

    private void verifyPin() {
        String enteredPin = pinViewPin.getText().toString();
        
        // In production, this would verify against user's actual PIN from secure storage
        if (enteredPin.equals(DEFAULT_PIN)) {
            Toast.makeText(this, "Mã PIN hợp lệ", Toast.LENGTH_SHORT).show();
            pinDialog.dismiss();
            
            // Now show OTP dialog after PIN is verified
            showOtpDialog();
        } else {
            Toast.makeText(this, "Mã PIN không đúng, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            pinViewPin.setText("");
        }
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

            // Kiểm tra số dư tài khoản trước khi tiếp tục
            checkAccountBalance();
        } else {
            Toast.makeText(this, getString(R.string.otp_verification_failed), Toast.LENGTH_SHORT).show();
            if(pinViewOtp != null) pinViewOtp.setText(""); // Clear OTP on failure
        }
    }

    private void checkAccountBalance() {
        Account currentAccount = GlobalVariables.getInstance().getCurrentAccount();
        
        // Add null check for account
        if (currentAccount == null) {
            Toast.makeText(this, "Không thể tải thông tin tài khoản. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            if (otpDialog != null && otpDialog.isShowing()) {
                otpDialog.dismiss();
            }
            finish();
            return;
        }
        
        int amountValue = Integer.parseInt(amount);
        
        // Kiểm tra số dư của tài khoản checking
        if (currentAccount.getChecking() != null && currentAccount.getChecking().getBalance() != null) {
            int currentBalance = currentAccount.getChecking().getBalance();
            
            if (currentBalance >= amountValue) {
                // Đủ số dư, tiếp tục tạo bill và giao dịch
                createBill();
            } else {
                // Không đủ số dư, hiển thị thông báo
                showInsufficientBalanceDialog(currentBalance, amountValue);
                
                if (otpDialog != null && otpDialog.isShowing()) {
                    otpDialog.dismiss();
                }
            }
        } else {
            // Lỗi khi đọc số dư
            Toast.makeText(this, "Không thể kiểm tra số dư tài khoản. Vui lòng thử lại sau.", 
                    Toast.LENGTH_SHORT).show();
            
            if (otpDialog != null && otpDialog.isShowing()) {
                otpDialog.dismiss();
            }
        }
    }
    
    private void showInsufficientBalanceDialog(int currentBalance, int requiredAmount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Số dư không đủ");
        builder.setMessage("Số dư hiện tại: " + NumberFormat.convertToCurrencyFormatHasUnit(currentBalance) + 
                "\nSố tiền cần thanh toán: " + NumberFormat.convertToCurrencyFormatHasUnit(requiredAmount));
        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void createBill() {
        // Add null check for currentUser
        User currentUser = GlobalVariables.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Không thể tải thông tin người dùng. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            if (otpDialog != null && otpDialog.isShowing()) {
                otpDialog.dismiss();
            }
            finish();
            return;
        }
        
        Bill bill = new Bill(
                Firestore.generateId("BI"), // UID will be generated by Firestore
                null, // Transaction ID (if applicable)
                Integer.parseInt(amount), // Amount from the current activity
                "pending", // Status of the bill
                null, // Due date (if applicable)
                null, // Bill number (if applicable)
                tvCarrier.getText().toString(), // Provider from the UI
                "phone", // Type of the bill
                currentUser.getUID() // User ID from global variables
        );
        Firestore.addEditBill(bill, new Firestore.FirestoreAddCallback() {
            @Override
            public  void onCallback(boolean success) {
                if (success) {
                    Log.d(TAG, "Bill added successfully");
                    createTransaction(bill);

                } else {
                    Log.e(TAG, "Failed to add bill");
                }
            }
        });
    }

    private void createTransaction(Bill bill) {
        // Add null check for account
        Account currentAccount = GlobalVariables.getInstance().getCurrentAccount();
        if (currentAccount == null) {
            Toast.makeText(this, "Không thể tải thông tin tài khoản. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        TransactionData transactionData = new TransactionData(
                Firestore.generateId("TR"), // UID will be generated by Firestore
                Integer.parseInt(amount), // Amount from the UI
                "payment", // Type of transaction
                "pending", // Status of the transaction
                "Nạp tiền ĐT " + tvPhoneNumber.getText().toString(), // Description
                Timestamp.now(),
                currentAccount.getUID() // Account UID
        );
        Firestore.addEditTransaction(transactionData, new Firestore.FirestoreAddCallback() {
            @Override
            public void onCallback(boolean success) {
                if (success) {
                    Log.d(TAG, "Transaction added successfully");
                    // Launch payment URL after adding transaction
                    goToVnPay(transactionData,bill.getUID());
                } else {
                    Log.e(TAG, "Failed to add transaction");
                }
            }
        });
    }

    private void goToVnPay(TransactionData transactionData, String billId) {
        // Make sure we have valid IDs
        String transactionId = transactionData.getUID();

        if (transactionId == null || transactionId.isEmpty() || billId == null || billId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không thể xác định mã giao dịch", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a composite order reference that includes both IDs
        String orderRef = transactionId + ":" + billId;

        Log.d(TAG, "Creating payment URL with Order Reference: " + orderRef);

        // Create the payment URL with the composite order reference
        String paymentUrl = VnPayUtils.createPaymentUrl(
                Integer.parseInt(amount),
                transactionData.getDescription(),
                orderRef
        );

        // Launch VNPay payment
        launchVnPayUrl(paymentUrl);

        if (otpDialog != null && otpDialog.isShowing()) {
            otpDialog.dismiss();
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
        if (pinDialog != null && pinDialog.isShowing()) {
            pinDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        if (pinDialog != null && pinDialog.isShowing()) {
            Toast.makeText(this, "Vui lòng hoàn thành hoặc hủy xác thực PIN.", Toast.LENGTH_SHORT).show();
        } else if (otpDialog != null && otpDialog.isShowing()) {
            Toast.makeText(this, "Vui lòng hoàn thành hoặc hủy xác thực OTP.", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }
}