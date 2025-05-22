package com.example.bankingapplication;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
// import android.content.pm.PackageManager; // Không thấy dùng trực tiếp
// import androidx.core.content.ContextCompat; // Không thấy dùng trực tiếp
// import androidx.core.app.ActivityCompat; // Không thấy dùng trực tiếp

// import com.example.bankingapplication.DepositWithdrawDialogFragment; // Đã import
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException; // Import thêm
import com.google.firebase.functions.HttpsCallableResult;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.CheckingAccount;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Object.Bill;
import com.example.bankingapplication.R;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class CustomerCheckingAccountFragment extends Fragment implements DepositWithdrawDialogFragment.DepositWithdrawListener {

    private static final String TAG = "CustCheckingFrag";
    private FirebaseFunctions mFunctions; // Sẽ được khởi tạo với region

    private CheckingAccount checkingAccountData;

    // private static final int TRANSACTION_NOTIFICATION_ID = 1001; // Không thấy dùng
    // private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 123; // Không thấy dùng
    private String accountNumber;
    private String parentAccountId;

    private TextView tvTotalBalance, tvAccountNumber_officer, tvBalanceDetail_officer;
    private ImageView ivCopyAccNum_officer, ivViewDetailsArrow_officer;
    private Button btnDefaultAccountIndicator_officer;
    private MaterialButton btnOfficerDeposit, btnOfficerWithdraw;

    private static final String ARG_BALANCE = "balance";
    private static final String ARG_ACCOUNT_NUMBER = "accountNumber";
    private static final String ARG_PARENT_ACCOUNT_ID_CHECKING = "parent_account_id_checking";


    public static CustomerCheckingAccountFragment newInstance(CheckingAccount checkingAcc, String accNum, String parentAccIdParam) {
        CustomerCheckingAccountFragment fragment = new CustomerCheckingAccountFragment();
        Bundle args = new Bundle();
        if (checkingAcc != null) {
            args.putInt(ARG_BALANCE, checkingAcc.getBalance() != null ? checkingAcc.getBalance() : 0);
        } else {
            args.putInt(ARG_BALANCE, 0);
        }
        args.putString(ARG_ACCOUNT_NUMBER, accNum);
        args.putString(ARG_PARENT_ACCOUNT_ID_CHECKING, parentAccIdParam);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            int balance = getArguments().getInt(ARG_BALANCE, 0);
            accountNumber = getArguments().getString(ARG_ACCOUNT_NUMBER, "N/A");
            parentAccountId = getArguments().getString(ARG_PARENT_ACCOUNT_ID_CHECKING);
            checkingAccountData = new CheckingAccount(balance);
            Log.d(TAG, "onCreate: parentAccountId received: " + parentAccountId + ", AccountNumber: " + accountNumber);
        } else {
            checkingAccountData = new CheckingAccount(0);
            accountNumber = "N/A";
            parentAccountId = null;
            Log.w(TAG, "onCreate: Arguments bundle is null.");
        }
        // Khởi tạo FirebaseFunctions với region cụ thể
        mFunctions = FirebaseFunctions.getInstance("asia-southeast1"); // <<<<< THAY ĐỔI QUAN TRỌNG
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_checking_account_officer, container, false);

        tvTotalBalance = view.findViewById(R.id.tv_officer_checking_total_balance);
        tvAccountNumber_officer = view.findViewById(R.id.tv_officer_checking_account_number);
        tvBalanceDetail_officer = view.findViewById(R.id.tv_officer_checking_balance_detail);
        ivCopyAccNum_officer = view.findViewById(R.id.iv_officer_checking_copy_acc_num);
        ivViewDetailsArrow_officer = view.findViewById(R.id.iv_officer_checking_view_details_arrow);
        btnDefaultAccountIndicator_officer = view.findViewById(R.id.btn_officer_checking_default_account_indicator);

        btnOfficerDeposit = view.findViewById(R.id.btn_officer_deposit_customer);
        btnOfficerWithdraw = view.findViewById(R.id.btn_officer_withdraw_customer);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // mFunctions đã được khởi tạo trong onCreate
        displayAccountInfo();
        setupClickListeners();
    }

    private void displayAccountInfo() {
        if (checkingAccountData != null && accountNumber != null) {
            Integer balance = checkingAccountData.getBalance(); // Lấy balance một lần
            String formattedBalance = NumberFormat.convertToCurrencyFormatHasUnit(balance != null ? balance : 0);
            tvTotalBalance.setText(formattedBalance);
            tvAccountNumber_officer.setText(accountNumber);
            tvBalanceDetail_officer.setText(formattedBalance);
        } else {
            tvTotalBalance.setText(NumberFormat.convertToCurrencyFormatHasUnit(0));
            tvAccountNumber_officer.setText("N/A");
            tvBalanceDetail_officer.setText(NumberFormat.convertToCurrencyFormatHasUnit(0));
        }
    }

    private void setupClickListeners() {
        ivCopyAccNum_officer.setOnClickListener(v -> {
            if (getContext() != null && accountNumber != null && !accountNumber.equals("N/A")) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) { // Kiểm tra clipboard null
                    ClipData clip = ClipData.newPlainText("Số tài khoản", accountNumber);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "Đã sao chép số tài khoản", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Lỗi: Không thể truy cập clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ivViewDetailsArrow_officer.setOnClickListener(v -> {
            // Hiện tại chỉ là Toast, nếu bạn muốn mở Activity khác thì bỏ comment
            // if (getActivity() != null && parentAccountId != null) {
            //     Intent intent = new Intent(getActivity(), TransactionHistoryActivity.class);
            //     intent.putExtra("ACCOUNT_ID_FOR_HISTORY", parentAccountId);
            //     startActivity(intent);
            // } else {
            //     Toast.makeText(getContext(), "Không thể mở lịch sử giao dịch.", Toast.LENGTH_SHORT).show();
            // }
            Toast.makeText(getContext(), "Xem lịch sử (Checking) cho TK: " + (parentAccountId != null ? parentAccountId : "N/A"), Toast.LENGTH_SHORT).show();
        });

        btnOfficerDeposit.setOnClickListener(v -> showDepositWithdrawDialog(true));
        btnOfficerWithdraw.setOnClickListener(v -> showDepositWithdrawDialog(false));
    }

    private void showDepositWithdrawDialog(boolean isDeposit) {
        String customerNameToPass = "Khách hàng";
        // Lấy customerName an toàn hơn
        Fragment parentFrag = getParentFragment();
        if (parentFrag instanceof CustomerDetailsFragment) {
            User customerUser = ((CustomerDetailsFragment) parentFrag).getCurrentLoadedUser();
            if (customerUser != null && customerUser.getName() != null && !customerUser.getName().isEmpty()) {
                customerNameToPass = customerUser.getName();
            }
        }

        if (accountNumber == null || "N/A".equals(accountNumber)) {
            Toast.makeText(getContext(), "Không thể thực hiện, thiếu thông tin số tài khoản.", Toast.LENGTH_SHORT).show();
            return;
        }

        DepositWithdrawDialogFragment dialogFragment = DepositWithdrawDialogFragment.newInstance(isDeposit, customerNameToPass, accountNumber);
        dialogFragment.setDepositWithdrawListener(this);
        // Sử dụng getChildFragmentManager() nếu dialog này là con trực tiếp của fragment này
        // Hoặc getParentFragmentManager() nếu nó liên quan đến fragment cha (CustomerDetailsFragment)
        dialogFragment.show(getParentFragmentManager(), DepositWithdrawDialogFragment.TAG);
    }

    @Override
    public void onTransactionConfirmedByOfficer(boolean isDeposit, int amount) {
        Log.d(TAG, "onTransactionConfirmedByOfficer called. IsDeposit: " + isDeposit + ", Amount: " + amount);

        if (parentAccountId == null || parentAccountId.isEmpty()) {
            if (getContext() != null) Toast.makeText(getContext(), "Lỗi: Không xác định được tài khoản khách hàng để cập nhật.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "parentAccountId is null or empty in onTransactionConfirmedByOfficer.");
            return;
        }
        if (getContext() == null) {
            Log.e(TAG, "Context is null in onTransactionConfirmedByOfficer. Fragment might be detached.");
            return;
        }

        Toast.makeText(getContext(), "Đang xử lý " + (isDeposit ? "nạp tiền..." : "rút tiền..."), Toast.LENGTH_SHORT).show();

        Firestore.getAccount(parentAccountId, customerAccount -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "Fragment not attached or context is null after getting customer account.");
                return;
            }
            if (customerAccount == null || customerAccount.getChecking() == null) {
                Toast.makeText(getContext(), "Lỗi: Không thể tải thông tin tài khoản khách hàng.", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentBalance = customerAccount.getChecking().getBalance() != null ? customerAccount.getChecking().getBalance() : 0;
            int newBalance;

            if (isDeposit) {
                newBalance = currentBalance + amount;
            } else {
                if (currentBalance < amount) {
                    Toast.makeText(getContext(), "Số dư khách hàng không đủ để rút tiền.", Toast.LENGTH_SHORT).show();
                    return;
                }
                newBalance = currentBalance - amount;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("checking.balance", newBalance);

            Firestore.updateAccountFields(parentAccountId, updates, (isSuccess, e) -> {
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "Fragment not attached or context is null after updating account fields.");
                    return;
                }
                if (isSuccess) {
                    String transactionType = isDeposit ? "deposit" : "withdrawal";
                    User officer = GlobalVariables.getInstance().getCurrentUser();
                    String officerName = (officer != null && officer.getName() != null && !officer.getName().isEmpty()) ? officer.getName() : "Nhân viên";

                    String customerDisplayName = customerAccount.getUserId();
                    Fragment parentFrag = getParentFragment();
                    if (parentFrag instanceof CustomerDetailsFragment) {
                        User customerUser = ((CustomerDetailsFragment) parentFrag).getCurrentLoadedUser();
                        if (customerUser != null && customerUser.getName() != null && !customerUser.getName().isEmpty()) {
                            customerDisplayName = customerUser.getName();
                        }
                    }

                    String description = String.format("NV %s %s %s cho KH %s (STK: %s).",
                            officerName,
                            isDeposit ? "nạp" : "rút",
                            NumberFormat.convertToCurrencyFormatOnlyNumber(amount),
                            customerDisplayName,
                            customerAccount.getAccountNumber()
                    );

                    TransactionData transaction = new TransactionData(
                            Firestore.generateId("TR"),
                            amount,
                            transactionType,
                            "completed",
                            description,
                            Timestamp.now(),
                            isDeposit ? null : parentAccountId, // fromAccountId: null nếu nạp, parentAccountId nếu rút
                            isDeposit ? parentAccountId : null  // toAccountId: parentAccountId nếu nạp, null nếu rút
                    );

                    Firestore.addEditTransaction(transaction, isTransactionSuccess -> {
                        if (!isAdded() || getContext() == null) {
                            Log.w(TAG, "Fragment not attached or context is null after adding transaction.");
                            return;
                        }
                        if (isTransactionSuccess) {
                            showSuccessDialog(isDeposit, transaction.getAmount(), customerAccount.getAccountNumber());
                            if (checkingAccountData != null) { // Kiểm tra null
                                checkingAccountData.setBalance(newBalance);
                            }
                            displayAccountInfo(); // Cập nhật UI

                            Fragment parentFragRefresh = getParentFragment();
                            if (parentFragRefresh instanceof CustomerDetailsFragment) {
                                ((CustomerDetailsFragment) parentFragRefresh).refreshCustomerDataOnDemand();
                            }
                            String billTypeForBill = isDeposit ? "deposit_at_counter" : "withdrawal_at_counter";
                            createBillForDepositWithdraw(transaction, customerAccount, isDeposit, officerName, billTypeForBill);
                        } else {
                            Toast.makeText(getContext(), "Lỗi ghi nhận giao dịch.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    String errorMsg = "Lỗi cập nhật số dư khách hàng.";
                    if (e != null && e.getMessage() != null) {
                        errorMsg += " " + e.getMessage();
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showSuccessDialog(boolean isDeposit, int amount, String accNumber) { // Sửa tên tham số
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_success_notification, null);

        TextView tvSuccessMessage = dialogView.findViewById(R.id.tv_success_message_dialog);
        AppCompatButton btnOk = dialogView.findViewById(R.id.btn_ok_success_dialog);

        String operation = isDeposit ? "Nạp tiền" : "Rút tiền";
        String message = operation + " " + NumberFormat.convertToCurrencyFormatHasUnit(amount) +
                " vào tài khoản " + accNumber + " thành công."; // Sử dụng accNumber
        tvSuccessMessage.setText(message);

        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog successDialog = builder.create();
        if (successDialog.getWindow() != null) {
            successDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnOk.setOnClickListener(v -> successDialog.dismiss());
        successDialog.show();
    }

    private void createBillForDepositWithdraw(TransactionData transaction, Account customerAccount, boolean isDeposit, String officerName, String billType) {
        if (!isAdded() || getContext() == null || transaction == null || customerAccount == null) { // Thêm kiểm tra null
            Log.w(TAG, "Cannot create bill, fragment not attached or context/transaction/customerAccount is null.");
            return;
        }

        String customerDisplayName = customerAccount.getUserId(); // Default to ID
        Fragment parentFrag = getParentFragment();
        if (parentFrag instanceof CustomerDetailsFragment) {
            User customerUser = ((CustomerDetailsFragment) parentFrag).getCurrentLoadedUser();
            if (customerUser != null && customerUser.getName() != null && !customerUser.getName().isEmpty()) {
                customerDisplayName = customerUser.getName();
            }
        }

        String billDescription = String.format("GD %s %s tại quầy cho KH %s (STK: %s). Thực hiện bởi NV %s.",
                isDeposit ? "nạp tiền" : "rút tiền",
                NumberFormat.convertToCurrencyFormatOnlyNumber(transaction.getAmount()),
                customerDisplayName,
                customerAccount.getAccountNumber(),
                officerName
        );

        Bill bill = new Bill(
                Firestore.generateId("BI"),
                transaction.getUID(),
                transaction.getAmount(),
                "completed", // Giao dịch tại quầy thường hoàn tất ngay
                Timestamp.now(), // Thời điểm tạo bill
                "DW" + System.currentTimeMillis(), // Mã bill tạm thời
                "Tại quầy - 3TBank",
                billType,
                customerAccount.getUserId()
        );

        Firestore.addEditBill(bill, isBillSuccess -> {
            if (!isAdded() || getContext() == null) return; // Kiểm tra lại
            if (isBillSuccess) {
                Log.d(TAG, "Bill created successfully for " + billType + ": " + bill.getUID());
                triggerSendNotificationToCustomerViaCloudFunction(
                        customerAccount.getUserId(),
                        (isDeposit ? "Tiền vào tài khoản" : "Tiền trừ từ tài khoản"),
                        String.format("Tài khoản %s của quý khách vừa %s %s. Thực hiện bởi NV %s.",
                                customerAccount.getAccountNumber(),
                                isDeposit ? "nhận" : "thực hiện rút",
                                NumberFormat.convertToCurrencyFormatHasUnit(transaction.getAmount()),
                                officerName),
                        transaction.getUID()
                );
            } else {
                Log.e(TAG, "Failed to create bill for " + billType);
            }
        });
    }

    private void triggerSendNotificationToCustomerViaCloudFunction(String customerUserId, String title, String body, String transactionId) {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot trigger Cloud Function.");
            return;
        }
        if (customerUserId == null || customerUserId.isEmpty()) {
            Log.e(TAG, "Customer User ID is null or empty, cannot send notification.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerUserId);
        data.put("title", title);
        data.put("body", body);
        if (transactionId != null && !transactionId.isEmpty()) {
            data.put("transactionId", transactionId);
        }

        Log.d(TAG, "Calling Cloud Function 'sendTransactionNotificationToCustomer' with data: " + data.toString());

        // mFunctions đã được khởi tạo với region trong onCreate
        mFunctions
                .getHttpsCallable("sendTransactionNotificationToCustomer")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (!isAdded() || getContext() == null) { // Kiểm tra lại fragment
                        Log.w(TAG, "Fragment detached or context null after Cloud Function call.");
                        return;
                    }
                    if (task.isSuccessful()) {
                        HttpsCallableResult result = task.getResult();
                        Log.i(TAG, "Cloud Function call successful: " + (result != null && result.getData() != null ? result.getData().toString() : "No data returned"));
                        // Không cần Toast ở đây nữa, notification sẽ tự hiện
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Cloud Function call failed.", e);
                        String errorMessage = "Lỗi khi yêu cầu gửi thông báo cho khách hàng.";
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            errorMessage += " Code: " + ffe.getCode() + ". Message: " + ffe.getMessage();
                        } else if (e != null){
                            errorMessage += " " + e.getMessage();
                        }
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}