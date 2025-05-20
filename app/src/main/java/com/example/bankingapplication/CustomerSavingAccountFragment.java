package com.example.bankingapplication;

import android.app.AlertDialog;
// import android.content.DialogInterface; // Không cần thiết nếu chỉ dùng lambda
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.Button; // Nếu nút trong layout là Button thường
import android.widget.EditText;
import android.widget.LinearLayout;
// import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.SavingAccount;
import com.example.bankingapplication.R;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class CustomerSavingAccountFragment extends Fragment {

    private static final String TAG = "CustSavingAccFragment";

    private SavingAccount savingAccountData;
    private String parentAccountId;

    private TextView tvTotalAccumulated, tvSavingBalanceValue, tvInterestRateValue;
    private MaterialButton btnEditInterestRate;
    // private MaterialButton btnSavingAccountIndicator;

    // Constants cho giới hạn lãi suất
    private static final double MIN_INTEREST_RATE = 0.0;
    private static final double MAX_INTEREST_RATE = 20.0; // Ví dụ: tối đa 20%

    private static final String ARG_BALANCE = "balance";
    private static final String ARG_INTEREST_RATE = "interestRate";
    private static final String ARG_PARENT_ACCOUNT_ID = "parent_account_id";


    public static CustomerSavingAccountFragment newInstance(SavingAccount savingAcc, String ignoredAccountNumber, String parentAccId) {
        CustomerSavingAccountFragment fragment = new CustomerSavingAccountFragment();
        Bundle args = new Bundle();
        if (savingAcc != null) {
            args.putInt(ARG_BALANCE, savingAcc.getBalance() != null ? savingAcc.getBalance() : 0);
            args.putDouble(ARG_INTEREST_RATE, savingAcc.getInterestRate());
        } else {
            args.putInt(ARG_BALANCE, 0);
            args.putDouble(ARG_INTEREST_RATE, 0.0);
        }
        args.putString(ARG_PARENT_ACCOUNT_ID, parentAccId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            int balance = getArguments().getInt(ARG_BALANCE, 0);
            double interestRate = getArguments().getDouble(ARG_INTEREST_RATE, 0.0);
            parentAccountId = getArguments().getString(ARG_PARENT_ACCOUNT_ID);
            savingAccountData = new SavingAccount(balance, interestRate, "active");
        } else {
            savingAccountData = new SavingAccount(0, 0.0, "inactive");
            parentAccountId = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_saving_account_officer, container, false);
        tvTotalAccumulated = view.findViewById(R.id.tv_officer_saving_total_accumulated);
        tvSavingBalanceValue = view.findViewById(R.id.tv_officer_saving_balance);
        tvInterestRateValue = view.findViewById(R.id.tv_officer_saving_interest_rate);
        btnEditInterestRate = view.findViewById(R.id.btn_officer_edit_saving_interest_rate);
        // btnSavingAccountIndicator = view.findViewById(R.id.btn_officer_saving_account_indicator);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        displayAccountInfo();
        setupClickListeners();
    }

    private void displayAccountInfo() {
        String formattedBalance = "0 VNĐ";
        double interestRate = 0.0;

        if (savingAccountData != null) {
            if (savingAccountData.getBalance() != null) {
                formattedBalance = NumberFormat.convertToCurrencyFormatHasUnit(savingAccountData.getBalance());
            }
            interestRate = savingAccountData.getInterestRate();
        }

        tvTotalAccumulated.setText(formattedBalance);
        tvSavingBalanceValue.setText(formattedBalance);
        tvInterestRateValue.setText(String.format(Locale.US, "%.2f %%", interestRate));
    }

    private void setupClickListeners() {
        btnEditInterestRate.setOnClickListener(v -> {
            if (getContext() == null || parentAccountId == null) {
                if (getContext() != null) Toast.makeText(getContext(), "Lỗi: Không thể thực hiện thao tác.", Toast.LENGTH_SHORT).show();
                return;
            }
            showEditInterestRateDialog();
        });
    }

    private void showEditInterestRateDialog() {
        if (getContext() == null) return; // Kiểm tra context một lần nữa

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Sửa Lãi Suất Tiết Kiệm");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(String.format(Locale.US, "Lãi suất (%.1f%% - %.1f%%)", MIN_INTEREST_RATE, MAX_INTEREST_RATE));
        if (savingAccountData != null) {
            input.setText(String.valueOf(savingAccountData.getInterestRate()));
        }

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        // Lấy dimen từ resources
        int horizontalMargin = (int) getResources().getDimension(R.dimen.app_padding_horizontal); // Giả sử bạn có dimen này
        int verticalMargin = horizontalMargin / 2; // Hoặc một dimen khác
        lp.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
        input.setLayoutParams(lp);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newRateStr = input.getText().toString();
            if (TextUtils.isEmpty(newRateStr)) {
                Toast.makeText(getContext(), "Lãi suất không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double newRate = Double.parseDouble(newRateStr);
                // KIỂM TRA GIỚI HẠN LÃI SUẤT
                if (newRate < MIN_INTEREST_RATE || newRate > MAX_INTEREST_RATE) {
                    Toast.makeText(getContext(),
                            String.format(Locale.US, "Lãi suất phải từ %.1f%% đến %.1f%%.", MIN_INTEREST_RATE, MAX_INTEREST_RATE),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                updateSavingInterestRateInFirestore(newRate);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Vui lòng nhập số hợp lệ cho lãi suất", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateSavingInterestRateInFirestore(double newRate) {
        if (parentAccountId == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy thông tin tài khoản.", Toast.LENGTH_SHORT).show();
            return;
        }
        btnEditInterestRate.setEnabled(false);

        String fieldPath = "saving.interestRate";
        Map<String, Object> updates = new HashMap<>();
        updates.put(fieldPath, newRate);

        Firestore.updateAccountFields(parentAccountId, updates, new Firestore.FirestoreUpdateCallback() {
            @Override
            public void onCallback(boolean isSuccess, Exception e) {
                if (!isAdded() || getContext() == null) return;
                btnEditInterestRate.setEnabled(true);

                if (isSuccess) {
                    Toast.makeText(getContext(), "Cập nhật lãi suất thành công!", Toast.LENGTH_SHORT).show();
                    if (savingAccountData != null) {
                        savingAccountData.setInterestRate(newRate);
                    }
                    displayAccountInfo();
                } else {
                    String errorMessage = "Lỗi cập nhật lãi suất.";
                    if (e != null && e.getMessage() != null) {
                        errorMessage += "\n" + e.getMessage();
                    }
                    Log.e(TAG, "Error updating saving interest rate for account: " + parentAccountId, e);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}