package com.example.bankingapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent; // Thêm nếu bạn đã thêm logic mở TransactionHistoryActivity
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
import androidx.fragment.app.Fragment;

import com.example.bankingapplication.DepositWithdrawDialogFragment;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.CheckingAccount;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.R;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class CustomerCheckingAccountFragment extends Fragment implements DepositWithdrawDialogFragment.DepositWithdrawListener {

    private CheckingAccount checkingAccountData;
    private String accountNumber;
    private String parentAccountId; // ID của Account cha (chính là Account của khách hàng đang xem)

    private TextView tvTotalBalance, tvAccountNumber_officer, tvBalanceDetail_officer; // Đổi tên để tránh nhầm lẫn nếu bạn copy-paste
    private ImageView ivCopyAccNum_officer, ivViewDetailsArrow_officer;
    private Button btnDefaultAccountIndicator_officer;
    private MaterialButton btnOfficerDeposit, btnOfficerWithdraw;


    // KEY MỚI CHO PARENT ACCOUNT ID
    private static final String ARG_PARENT_ACCOUNT_ID_CHECKING = "parent_account_id_checking";

    // Cập nhật newInstance
    public static CustomerCheckingAccountFragment newInstance(CheckingAccount checkingAcc, String accNum, String parentAccIdParam) {
        CustomerCheckingAccountFragment fragment = new CustomerCheckingAccountFragment();
        Bundle args = new Bundle();
        if (checkingAcc != null) {
            args.putInt("balance", checkingAcc.getBalance() != null ? checkingAcc.getBalance() : 0);
        } else {
            args.putInt("balance", 0);
        }
        args.putString("accountNumber", accNum);
        args.putString(ARG_PARENT_ACCOUNT_ID_CHECKING, parentAccIdParam); // Truyền parentAccountId
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            int balance = getArguments().getInt("balance", 0);
            accountNumber = getArguments().getString("accountNumber", "N/A");
            parentAccountId = getArguments().getString(ARG_PARENT_ACCOUNT_ID_CHECKING); // Lấy parentAccountId
            checkingAccountData = new CheckingAccount(balance);
            Log.d("CustCheckingFrag", "onCreate: parentAccountId received: " + parentAccountId);
        } else {
            checkingAccountData = new CheckingAccount(0);
            accountNumber = "N/A";
            parentAccountId = null;
            Log.w("CustCheckingFrag", "onCreate: Arguments bundle is null.");
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
            // Mở TransactionHistoryActivity nếu cần
            if (getActivity() != null) {
                // Intent intent = new Intent(getActivity(), TransactionHistoryActivity.class);
                // startActivity(intent);
                Toast.makeText(getContext(), "Xem lịch sử giao dịch (Checking)", Toast.LENGTH_SHORT).show();
            }
        });

        btnOfficerDeposit.setOnClickListener(v -> showDepositWithdrawDialog(true));
        btnOfficerWithdraw.setOnClickListener(v -> showDepositWithdrawDialog(false));
    }

    private void showDepositWithdrawDialog(boolean isDeposit) {
        String customerNameToPass = "Khách hàng";
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
    public void onConfirmTransaction(boolean isDeposit, int amount, String officerPin) {
        // ... (logic xác thực PIN nhân viên của bạn ở đây) ...
        // User currentOfficer = GlobalVariables.getInstance().getCurrentUser(); // Nhân viên đang đăng nhập
        // Account officerAccount = GlobalVariables.getInstance().getOfficerAccount(); // Cần có cách lấy tài khoản của officer
        // if (currentOfficer == null || officerAccount == null || !officerPin.equals(officerAccount.getPinCode())) {
        //    Toast.makeText(getContext(), "Mã PIN nhân viên không đúng!", Toast.LENGTH_SHORT).show();
        //    return;
        // }
        Toast.makeText(getContext(), "PIN nhân viên hợp lệ (giả định). Đang xử lý " + (isDeposit ? "nạp tiền..." : "rút tiền..."), Toast.LENGTH_SHORT).show();


        if (parentAccountId == null || parentAccountId.isEmpty()) {
            Toast.makeText(getContext(), "Lỗi: Không xác định được tài khoản khách hàng để cập nhật.", Toast.LENGTH_SHORT).show();
            Log.e("CustCheckingFrag", "parentAccountId is null or empty in onConfirmTransaction.");
            return;
        }

        Firestore.getAccount(parentAccountId, customerAccount -> {
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
                if (isSuccess) {
                    String transactionType = isDeposit ? "deposit" : "withdrawal";
                    String description = (isDeposit ? "NV nạp tiền vào TK " : "NV rút tiền từ TK ") + customerAccount.getAccountNumber();
                    User officer = GlobalVariables.getInstance().getCurrentUser();
                    String officerInfoForDesc = (officer != null && officer.getName() != null) ? " bởi " + officer.getName() : "";
                    description += officerInfoForDesc;


                    TransactionData transaction = new TransactionData(
                            Firestore.generateId("TR"),
                            amount,
                            transactionType,
                            "completed",
                            description,
                            Timestamp.now(),
                            isDeposit ? null : parentAccountId, // From: KH nếu rút, null nếu nạp tại quầy
                            isDeposit ? parentAccountId : null  // To: KH nếu nạp, null nếu rút tại quầy
                    );
                    // if (officer != null) transaction.setOfficerId(officer.getUID()); // Gán ID nhân viên nếu có

                    Firestore.addEditTransaction(transaction, isTransactionSuccess -> { // Chỉ nhận một tham số isTransactionSuccess
                        if (isTransactionSuccess) {
                            Toast.makeText(getContext(), (isDeposit ? "Nạp tiền" : "Rút tiền") + " thành công!", Toast.LENGTH_SHORT).show();
                            checkingAccountData.setBalance(newBalance);
                            displayAccountInfo();

                            if (getParentFragment() instanceof CustomerDetailsFragment) {
                                ((CustomerDetailsFragment) getParentFragment()).refreshCustomerDataOnDemand();
                            }
                        } else {
                            Toast.makeText(getContext(), "Lỗi ghi nhận giao dịch.", Toast.LENGTH_SHORT).show();
                            // Bạn sẽ không có thông tin Exception cụ thể ở đây
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Lỗi cập nhật số dư khách hàng.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}