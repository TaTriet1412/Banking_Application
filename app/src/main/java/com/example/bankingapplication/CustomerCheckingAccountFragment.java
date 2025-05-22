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
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import com.example.bankingapplication.DepositWithdrawDialogFragment;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.CheckingAccount;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Object.Bill; // Thêm Bill nếu bạn tạo Bill cho nạp/rút
import com.example.bankingapplication.R;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp; // Import Timestamp

import java.util.HashMap;
import java.util.Map;

public class CustomerCheckingAccountFragment extends Fragment implements DepositWithdrawDialogFragment.DepositWithdrawListener {

    private static final String TAG = "CustCheckingFrag";
    private FirebaseFunctions mFunctions;// Tag cho logging

    private CheckingAccount checkingAccountData;

    private static final int TRANSACTION_NOTIFICATION_ID = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 123;
    private String accountNumber;
    private String parentAccountId; // ID của Account cha (chính là Account của khách hàng đang xem)

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
        mFunctions = FirebaseFunctions.getInstance();
        displayAccountInfo();
        setupClickListeners();
    }

    private void displayAccountInfo() {
        if (checkingAccountData != null && accountNumber != null) {
            tvTotalBalance.setText(NumberFormat.convertToCurrencyFormatHasUnit(checkingAccountData.getBalance()));
            tvAccountNumber_officer.setText(accountNumber);
            tvBalanceDetail_officer.setText(NumberFormat.convertToCurrencyFormatHasUnit(checkingAccountData.getBalance()));
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
                ClipData clip = ClipData.newPlainText("Số tài khoản", accountNumber);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "Đã sao chép số tài khoản", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ivViewDetailsArrow_officer.setOnClickListener(v -> {
            if (getActivity() != null && parentAccountId != null) {
                // Intent intent = new Intent(getActivity(), TransactionHistoryActivity.class);
                // intent.putExtra("ACCOUNT_ID_FOR_HISTORY", parentAccountId); // Truyền ID tài khoản khách hàng
                // startActivity(intent);
                Toast.makeText(getContext(), "Xem lịch sử (Checking) cho TK: " + parentAccountId, Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(getContext(), "Không thể mở lịch sử giao dịch.", Toast.LENGTH_SHORT).show();
            }
        });

        btnOfficerDeposit.setOnClickListener(v -> showDepositWithdrawDialog(true));
        btnOfficerWithdraw.setOnClickListener(v -> showDepositWithdrawDialog(false));
    }

    private void showDepositWithdrawDialog(boolean isDeposit) {
        String customerNameToPass = "Khách hàng"; // Default name
        if (getParentFragment() instanceof CustomerDetailsFragment) {
            User customerUser = ((CustomerDetailsFragment) getParentFragment()).getCurrentLoadedUser();
            if (customerUser != null && customerUser.getName() != null) {
                customerNameToPass = customerUser.getName();
            }
        }

        if (accountNumber == null || accountNumber.equals("N/A")) {
            Toast.makeText(getContext(), "Không thể thực hiện, thiếu thông tin số tài khoản.", Toast.LENGTH_SHORT).show();
            return;
        }

        DepositWithdrawDialogFragment dialogFragment = DepositWithdrawDialogFragment.newInstance(isDeposit, customerNameToPass, accountNumber);
        dialogFragment.setDepositWithdrawListener(this);
        dialogFragment.show(getParentFragmentManager(), DepositWithdrawDialogFragment.TAG);
    }

    @Override
    public void onTransactionConfirmedByOfficer(boolean isDeposit, int amount) {
        Log.d(TAG, "onTransactionConfirmedByOfficer called. IsDeposit: " + isDeposit + ", Amount: " + amount);
        // OTP nhân viên đã được xác thực trong DialogFragment

        if (parentAccountId == null || parentAccountId.isEmpty()) {
            Toast.makeText(getContext(), "Lỗi: Không xác định được tài khoản khách hàng để cập nhật.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "parentAccountId is null or empty in onTransactionConfirmedByOfficer.");
            return;
        }
        if (getContext() == null) {
            Log.e(TAG, "Context is null in onTransactionConfirmedByOfficer. Fragment might be detached.");
            return;
        }

        Toast.makeText(getContext(), "Đang xử lý " + (isDeposit ? "nạp tiền..." : "rút tiền..."), Toast.LENGTH_SHORT).show();


        Firestore.getAccount(parentAccountId, customerAccount -> {
            if (!isAdded() || getContext() == null) { // Kiểm tra lại fragment có còn attached không
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
            } else { // Withdraw
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
                    User officer = GlobalVariables.getInstance().getCurrentUser(); // Nhân viên đang thực hiện
                    String officerName = (officer != null && officer.getName() != null) ? officer.getName() : "Nhân viên";

                    String customerDisplayName = customerAccount.getUserId(); // Lấy tên KH nếu có, nếu không thì tạm dùng ID
                    if (getParentFragment() instanceof CustomerDetailsFragment) {
                        User customerUser = ((CustomerDetailsFragment) getParentFragment()).getCurrentLoadedUser();
                        if (customerUser != null && customerUser.getName() != null) {
                            customerDisplayName = customerUser.getName();
                        }
                    }

                    String description = String.format("NV %s %s %s cho KH %s (STK: %s).",
                            officerName,
                            isDeposit ? "nạp" : "rút",
                            NumberFormat.convertToCurrencyFormatOnlyNumber(amount), // Chỉ lấy số, không có VNĐ
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
                            isDeposit ? null : parentAccountId,
                            isDeposit ? parentAccountId : null
                    );
                    // if (officer != null) transaction.setOfficerId(officer.getUID()); // Nếu có trường này

                    Firestore.addEditTransaction(transaction, isTransactionSuccess -> { // Chỉ nhận 1 tham số
                        if (!isAdded() || getContext() == null) {
                            Log.w(TAG, "Fragment not attached or context is null after adding transaction.");
                            return;
                        }
                        if (isTransactionSuccess) {
                            showSuccessDialog(isDeposit, transaction.getAmount(), customerAccount.getAccountNumber());
                            checkingAccountData.setBalance(newBalance);
                            displayAccountInfo();

                            if (getParentFragment() instanceof CustomerDetailsFragment) {
                                ((CustomerDetailsFragment) getParentFragment()).refreshCustomerDataOnDemand();
                            }
                            String billTypeForBill = isDeposit ? "deposit_at_counter" : "withdrawal_at_counter";
                            // GỌI VÀ TRUYỀN BILLTYPE VÀO
                            createBillForDepositWithdraw(transaction, customerAccount, isDeposit, officerName, billTypeForBill);
                        } else {
                            // Lỗi đã được ghi log trong Firestore.java
                            Toast.makeText(getContext(), "Lỗi ghi nhận giao dịch.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Lỗi cập nhật số dư khách hàng." + (e != null ? " " + e.getMessage() : ""), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showSuccessDialog(boolean isDeposit, int amount, String accountNumber) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_success_notification, null);

        TextView tvSuccessMessage = dialogView.findViewById(R.id.tv_success_message_dialog);
        AppCompatButton btnOk = dialogView.findViewById(R.id.btn_ok_success_dialog);

        String operation = isDeposit ? "Nạp tiền" : "Rút tiền";
        String message = operation + " " + NumberFormat.convertToCurrencyFormatHasUnit(amount) +
                " vào tài khoản " + accountNumber + " thành công.";
        tvSuccessMessage.setText(message);

        builder.setView(dialogView);
        builder.setCancelable(false); // Không cho hủy bằng nút back

        AlertDialog successDialog = builder.create();
        if (successDialog.getWindow() != null) {
            successDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnOk.setOnClickListener(v -> successDialog.dismiss());
        successDialog.show();
    }

    private void createBillForDepositWithdraw(TransactionData transaction, Account customerAccount, boolean isDeposit, String officerName, String billType) {
        if (!isAdded() || getContext() == null) return;

        // String billType = isDeposit ? "deposit_at_counter" : "withdrawal_at_counter"; // BỎ DÒNG NÀY VÌ ĐÃ TRUYỀN VÀO

        String customerDisplayName = customerAccount.getUserId();
        if (getParentFragment() instanceof CustomerDetailsFragment) {
            User customerUser = ((CustomerDetailsFragment) getParentFragment()).getCurrentLoadedUser();
            if (customerUser != null && customerUser.getName() != null) {
                customerDisplayName = customerUser.getName();
            }
        }

        String billDescription = String.format("GD %s %s tại quầy cho KH %s (STK: %s). Thực hiện bởi NV %s.",
                isDeposit ? "nạp tiền" : "rút tiền",
                NumberFormat.convertToCurrencyFormatOnlyNumber(transaction.getAmount()), // Chỉ lấy số
                customerDisplayName,
                customerAccount.getAccountNumber(),
                officerName
        );

        Bill bill = new Bill(
                Firestore.generateId("BI"),
                transaction.getUID(),
                transaction.getAmount(),
                "completed",
                Timestamp.now(),
                "DW" + System.currentTimeMillis(),
                "Tại quầy - 3TBank",
                billType,                   // SỬ DỤNG THAM SỐ billType Ở ĐÂY
                customerAccount.getUserId()
        );

        Firestore.addEditBill(bill, isBillSuccess -> {
            if (isBillSuccess) {
                Log.d(TAG, "Bill created successfully for " + billType + ": " + bill.getUID());
                // Gọi hàm mới để kích hoạt gửi thông báo qua backend
                triggerSendNotificationToCustomerViaCloudFunction(
                        customerAccount.getUserId(), // ID của khách hàng
                        (isDeposit ? "Tiền vào tài khoản" : "Tiền trừ từ tài khoản"), // Title cho notification
                        String.format("Tài khoản %s của quý khách vừa %s %s. Thực hiện bởi NV %s.",
                                customerAccount.getAccountNumber(),
                                isDeposit ? "nhận" : "thực hiện rút",
                                NumberFormat.convertToCurrencyFormatHasUnit(transaction.getAmount()),
                                officerName), // Body cho notification
                        transaction.getUID() // Transaction ID để deep link
                );
            } else {
                Log.e(TAG, "Failed to create bill for " + billType);
            }
        });
    }

    private void triggerSendNotificationToCustomerViaCloudFunction(String customerUserId, String title, String body, String transactionId) {
        if (getContext() == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerUserId);
        data.put("title", title);
        data.put("body", body);
        data.put("transactionId", transactionId);

        Log.d(TAG, "Calling Cloud Function 'sendTransactionNotificationToCustomer' with data: " + data.toString());

        mFunctions
                .getHttpsCallable("sendTransactionNotificationToCustomer") // Tên Cloud Function
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        HttpsCallableResult result = task.getResult();
                        Log.i(TAG, "Cloud Function call successful: " + (result != null ? result.getData() : "No data returned"));
                        // Toast.makeText(getContext(), "Yêu cầu gửi thông báo cho KH đã được gửi.", Toast.LENGTH_SHORT).show();
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Cloud Function call failed.", e);
                        Toast.makeText(getContext(), "Lỗi khi yêu cầu gửi thông báo cho khách hàng.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
