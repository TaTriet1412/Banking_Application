package com.example.bankingapplication;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.chaos.view.PinView;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.R;
import com.example.bankingapplication.Utils.EmailUtils;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DepositWithdrawDialogFragment extends DialogFragment {

    public static final String TAG = "DepositWithdrawDialog";
    private static final String ARG_IS_DEPOSIT = "is_deposit";
    private static final String ARG_CUSTOMER_NAME = "customer_name";
    private static final String ARG_CUSTOMER_ACCOUNT_NUMBER = "customer_account_number";

    private TextView tvDialogTitle, tvCustomerAccountInfo;
    private TextInputLayout tilAmount;
    private TextInputEditText etAmount;
    private GridLayout gridLayoutQuickAmounts;
    private AppCompatButton btnCancel, btnConfirm;

    private boolean isDepositOperation;
    private String customerName;
    private String customerAccountNumber;

    private DepositWithdrawListener listener;

    // OTP Dialog components (sẽ được tạo khi cần)
    private AlertDialog otpDialogInstance; // Đổi tên để phân biệt với otpDialog trong Activity
    private PinView pinViewOtpDialog;
    private TextView tvOtpMessageDialog, tvResendOtpDialog;
    private Button btnVerifyOtpDialog;
    private ImageButton btnCloseOtpDialogInstance;
    private CountDownTimer resendOtpDialogTimer;
    private CountDownTimer otpExpiryDialogTimer;
    private String currentOtpDialog;
    private long otpExpiryDialogTimestamp;
    private final long OTP_EXPIRY_DURATION_DIALOG = 2 * 60 * 1000; // 2 minutes
    private int[] resendDelaysSecondsDialog = {15, 20, 30, 50, 90};
    private int currentResendAttemptDialog = 0;
    private boolean isOtpDialogExpired = false;

    private int pendingAmount; // Lưu số tiền chờ xác thực OTP

    private final String[] quickAmounts = {
            "50.000", "100.000", "200.000",
            "500.000", "1.000.000", "2.000.000"
    };
    private List<AppCompatButton> quickAmountButtons = new ArrayList<>();
    private AppCompatButton selectedQuickAmountButton = null;


    public interface DepositWithdrawListener {
        void onTransactionConfirmedByOfficer(boolean isDeposit, int amount); // Chỉ cần số tiền và loại giao dịch
    }

    public static DepositWithdrawDialogFragment newInstance(boolean isDeposit, String custName, String custAccNum) {
        DepositWithdrawDialogFragment fragment = new DepositWithdrawDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_DEPOSIT, isDeposit);
        args.putString(ARG_CUSTOMER_NAME, custName);
        args.putString(ARG_CUSTOMER_ACCOUNT_NUMBER, custAccNum);
        fragment.setArguments(args);
        return fragment;
    }

    public void setDepositWithdrawListener(DepositWithdrawListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isDepositOperation = getArguments().getBoolean(ARG_IS_DEPOSIT);
            customerName = getArguments().getString(ARG_CUSTOMER_NAME);
            customerAccountNumber = getArguments().getString(ARG_CUSTOMER_ACCOUNT_NUMBER);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_deposit_withdraw, null);

        tvDialogTitle = view.findViewById(R.id.tv_dialog_title_deposit_withdraw);
        tvCustomerAccountInfo = view.findViewById(R.id.tv_customer_account_info_dialog);
        tilAmount = view.findViewById(R.id.til_amount_deposit_withdraw);
        etAmount = view.findViewById(R.id.et_amount_deposit_withdraw);
        gridLayoutQuickAmounts = view.findViewById(R.id.grid_layout_quick_amounts_dialog);
        btnCancel = view.findViewById(R.id.btn_cancel_deposit_withdraw);
        btnConfirm = view.findViewById(R.id.btn_confirm_deposit_withdraw);

        tvDialogTitle.setText(isDepositOperation ? "Nạp tiền cho khách hàng" : "Rút tiền cho khách hàng");
        tvCustomerAccountInfo.setText("Tài khoản: " + customerAccountNumber + "\nTên: " + customerName);

        populateQuickAmountButtons();
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Bỏ chọn nút chọn nhanh nếu người dùng tự nhập
                if (selectedQuickAmountButton != null) {
                    setQuickButtonState(selectedQuickAmountButton, false);
                    selectedQuickAmountButton = null;
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });


        btnCancel.setOnClickListener(v -> dismiss());
        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            if (TextUtils.isEmpty(amountStr) || amountStr.equals("0")) {
                tilAmount.setError("Vui lòng nhập hoặc chọn số tiền");
                return;
            }
            try {
                pendingAmount = Integer.parseInt(amountStr.replace(".", "")); // Xóa dấu chấm nếu có
                if (pendingAmount <= 0) {
                    tilAmount.setError("Số tiền phải lớn hơn 0");
                    return;
                }
                tilAmount.setError(null);
                showOtpVerificationDialogForOfficer(); // Gọi dialog OTP
            } catch (NumberFormatException e) {
                tilAmount.setError("Số tiền không hợp lệ");
            }
        });

        builder.setView(view);
        Dialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    private void populateQuickAmountButtons() {
        gridLayoutQuickAmounts.removeAllViews();
        quickAmountButtons.clear();

        // Lấy kích thước màn hình để tính toán linh hoạt
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        // Padding của Dialog (ví dụ: 20dp mỗi bên như trong XML của bạn)
        int dialogHorizontalPadding = dpToPx(20) * 2;

        // Padding của chính GridLayout (nếu có, trong XML của bạn hiện không có)
        int gridLayoutPaddingStart = gridLayoutQuickAmounts.getPaddingStart();
        int gridLayoutPaddingEnd = gridLayoutQuickAmounts.getPaddingEnd();
        int gridLayoutTotalHorizontalPadding = gridLayoutPaddingStart + gridLayoutPaddingEnd;

        // Chiều rộng khả dụng cho nội dung bên trong GridLayout
        int availableWidthForGridContent = screenWidth - dialogHorizontalPadding - gridLayoutTotalHorizontalPadding;

        int columnCount = gridLayoutQuickAmounts.getColumnCount();
        if (columnCount <= 0) columnCount = 3; // Giá trị mặc định nếu XML không set

        // Margin bạn muốn cho mỗi button (ví dụ: 2dp mỗi bên trái/phải)
        int buttonMarginHorizontalEachSide = dpToPx(2); // margin ngang mỗi bên của button
        // Tổng margin ngang cho MỘT button
        int totalMarginPerButton = buttonMarginHorizontalEachSide * 2;

        // Tổng không gian bị chiếm bởi margin của tất cả các button trong một hàng
        // (columnCount button sẽ có columnCount * totalMarginPerButton)
        int totalHorizontalMarginsInRow = columnCount * totalMarginPerButton;

        // Chiều rộng còn lại cho nội dung của tất cả các button (sau khi trừ hết margin của chúng)
        int remainingWidthForButtonActualContent = availableWidthForGridContent - totalHorizontalMarginsInRow;

        // Chiều rộng cho nội dung của mỗi button
        int buttonContentWidth = remainingWidthForButtonActualContent / columnCount;

        for (String amountText : quickAmounts) {
            AppCompatButton button = new AppCompatButton(requireContext());
            button.setText(amountText + "đ"); // Giữ nguyên "đ" để nhất quán với ảnh
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            button.setAllCaps(false);
            // Bỏ padding cố định ở đây, để button tự điều chỉnh theo text và width đã tính
            // button.setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = buttonContentWidth; // Set chiều rộng đã tính
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(buttonMarginHorizontalEachSide, dpToPx(6), buttonMarginHorizontalEachSide, dpToPx(6)); // Điều chỉnh margin dọc nếu cần
            button.setLayoutParams(params);

            setQuickButtonState(button, false);
            button.setOnClickListener(v -> {
                handleQuickAmountButtonClick((AppCompatButton) v, amountText);
            });
            quickAmountButtons.add(button);
            gridLayoutQuickAmounts.addView(button);
        }
    }

    private void handleQuickAmountButtonClick(AppCompatButton clickedButton, String amountValue) {
        if (selectedQuickAmountButton != null && selectedQuickAmountButton != clickedButton) {
            setQuickButtonState(selectedQuickAmountButton, false);
        }
        setQuickButtonState(clickedButton, true);
        selectedQuickAmountButton = clickedButton;
        etAmount.setText(amountValue.replace(".", "")); // Cập nhật EditText, bỏ dấu chấm
        etAmount.setSelection(etAmount.getText().length()); // Di chuyển con trỏ cuối
        tilAmount.setError(null); // Xóa lỗi nếu có
    }

    private void setQuickButtonState(AppCompatButton button, boolean isSelected) {
        if (isSelected) {
            button.setBackgroundResource(R.drawable.rounded_corner_green); // Nền xanh khi chọn
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackgroundResource(R.drawable.rounded_corner_white_bordered); // Nền trắng viền xám
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dark));
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }


    // --- OTP Verification Logic for Officer ---
    private void showOtpVerificationDialogForOfficer() {
        if (otpDialogInstance != null && otpDialogInstance.isShowing()) {
            return;
        }
        User officer = GlobalVariables.getInstance().getCurrentUser();
        if (officer == null || officer.getEmail() == null || officer.getEmail().isEmpty()) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy thông tin email nhân viên để gửi OTP.", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_otp_verification, null);
        builder.setView(dialogView).setCancelable(false);

        otpDialogInstance = builder.create();
        if (otpDialogInstance.getWindow() != null) {
            otpDialogInstance.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        tvOtpMessageDialog = dialogView.findViewById(R.id.tvOtpMessage);
        pinViewOtpDialog = dialogView.findViewById(R.id.pinViewOtp);
        tvResendOtpDialog = dialogView.findViewById(R.id.tvResendOtp);
        btnVerifyOtpDialog = dialogView.findViewById(R.id.btnVerifyOtp);
        btnCloseOtpDialogInstance = dialogView.findViewById(R.id.btnCloseDialog);

        String maskedEmail = officer.getEmail().replaceAll("(?<=.{2}).(?=.*@)", "*");
        tvOtpMessageDialog.setText("Nhập mã OTP đã được gửi đến email của bạn: " + maskedEmail);
        pinViewOtpDialog.requestFocus();

        pinViewOtpDialog.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnVerifyOtpDialog.setEnabled(s.length() == pinViewOtpDialog.getItemCount());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        btnVerifyOtpDialog.setEnabled(false);

        btnCloseOtpDialogInstance.setOnClickListener(v -> {
            cancelOtpTimersDialog();
            otpDialogInstance.dismiss();
        });

        btnVerifyOtpDialog.setOnClickListener(v -> verifyOtpForOfficer());
        tvResendOtpDialog.setOnClickListener(v -> handleResendOtpForOfficer());

        sendNewOtpForOfficer(officer.getEmail());
        otpDialogInstance.show();
    }

    private void sendNewOtpForOfficer(String officerEmail) {
        isOtpDialogExpired = false;
        currentOtpDialog = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        otpExpiryDialogTimestamp = System.currentTimeMillis() + OTP_EXPIRY_DURATION_DIALOG;

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        String expiryTimeFormatted = sdf.format(new java.util.Date(otpExpiryDialogTimestamp));

        String operationType = isDepositOperation ? "Nạp tiền" : "Rút tiền";
        String subject = "Xác thực OTP cho giao dịch " + operationType;
        String body = "Mã OTP của bạn để thực hiện giao dịch " + operationType + " cho khách hàng "
                + customerName + " (STK: " + customerAccountNumber + ") "
                + "với số tiền " + NumberFormat.convertToCurrencyFormatHasUnit(pendingAmount)
                + " là: " + currentOtpDialog
                + "\nHiệu lực đến: " + expiryTimeFormatted + " (trong vòng 2 phút).";

        EmailUtils.sendEmail(officerEmail, subject, body);
        Toast.makeText(getContext(), "Mã OTP đã gửi đến email nhân viên.", Toast.LENGTH_SHORT).show();

        if (pinViewOtpDialog != null) pinViewOtpDialog.setText("");
        if (btnVerifyOtpDialog != null) btnVerifyOtpDialog.setEnabled(false);

        startOtpExpiryTimerDialog();
        startResendOtpCooldownDialog();
    }

    private void handleResendOtpForOfficer() {
        if (tvResendOtpDialog.isEnabled()) {
            cancelOtpTimersDialog();
            currentResendAttemptDialog++;
            User officer = GlobalVariables.getInstance().getCurrentUser();
            if (officer != null && officer.getEmail() != null) {
                sendNewOtpForOfficer(officer.getEmail());
            }
        }
    }
    private void startOtpExpiryTimerDialog() {
        if (otpExpiryDialogTimer != null) otpExpiryDialogTimer.cancel();
        otpExpiryDialogTimer = new CountDownTimer(OTP_EXPIRY_DURATION_DIALOG, 1000) {
            @Override public void onTick(long millisUntilFinished) {}
            @Override public void onFinish() {
                isOtpDialogExpired = true;
                if (otpDialogInstance != null && otpDialogInstance.isShowing() && getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.otp_expired), Toast.LENGTH_LONG).show();
                    if(btnVerifyOtpDialog != null) btnVerifyOtpDialog.setEnabled(false);
                    if(pinViewOtpDialog != null) pinViewOtpDialog.setText("");
                }
            }
        }.start();
    }

    private void startResendOtpCooldownDialog() {
        if(tvResendOtpDialog == null) return;
        tvResendOtpDialog.setEnabled(false);
        long delayMillis = resendDelaysSecondsDialog[Math.min(currentResendAttemptDialog, resendDelaysSecondsDialog.length - 1)] * 1000L;

        if (resendOtpDialogTimer != null) resendOtpDialogTimer.cancel();
        resendOtpDialogTimer = new CountDownTimer(delayMillis, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                if(tvResendOtpDialog != null) tvResendOtpDialog.setText(String.format(Locale.getDefault(), "Gửi lại (%ds)", millisUntilFinished / 1000));
            }
            @Override public void onFinish() {
                if(tvResendOtpDialog != null) {
                    tvResendOtpDialog.setText("Gửi lại");
                    tvResendOtpDialog.setEnabled(true);
                }
            }
        }.start();
    }


    private void verifyOtpForOfficer() {
        String enteredOtp = pinViewOtpDialog.getText().toString();
        if (System.currentTimeMillis() > otpExpiryDialogTimestamp) {
            isOtpDialogExpired = true; // Đảm bảo cờ được đặt
        }

        if (isOtpDialogExpired) {
            Toast.makeText(getContext(), getString(R.string.otp_expired), Toast.LENGTH_SHORT).show();
            if(pinViewOtpDialog != null) pinViewOtpDialog.setText("");
            if(btnVerifyOtpDialog != null) btnVerifyOtpDialog.setEnabled(false);
            return;
        }

        if (enteredOtp.equals(currentOtpDialog)) {
            Toast.makeText(getContext(), getString(R.string.otp_verification_success), Toast.LENGTH_SHORT).show();
            cancelOtpTimersDialog();
            otpDialogInstance.dismiss();
            if (listener != null) {
                listener.onTransactionConfirmedByOfficer(isDepositOperation, pendingAmount);
            }
            dismiss(); // Đóng dialog chính (DepositWithdrawDialogFragment)
        } else {
            Toast.makeText(getContext(), getString(R.string.otp_verification_failed), Toast.LENGTH_SHORT).show();
            if(pinViewOtpDialog != null) pinViewOtpDialog.setText("");
        }
    }

    private void cancelOtpTimersDialog() {
        if (resendOtpDialogTimer != null) resendOtpDialogTimer.cancel();
        if (otpExpiryDialogTimer != null) otpExpiryDialogTimer.cancel();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelOtpTimersDialog(); // Hủy timer khi dialog bị hủy
        if (otpDialogInstance != null && otpDialogInstance.isShowing()) {
            otpDialogInstance.dismiss();
        }
    }
}