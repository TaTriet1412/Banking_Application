package com.example.bankingapplication; // Ví dụ, tạo package Dialogs

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.DialogFragment;

import com.example.bankingapplication.Object.User; // Cần để lấy tên User
import com.example.bankingapplication.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class DepositWithdrawDialogFragment extends DialogFragment {

    public static final String TAG = "DepositWithdrawDialog";
    private static final String ARG_IS_DEPOSIT = "is_deposit";
    private static final String ARG_CUSTOMER_NAME = "customer_name";
    private static final String ARG_CUSTOMER_ACCOUNT_NUMBER = "customer_account_number";

    private TextView tvDialogTitle, tvCustomerAccountInfo;
    private TextInputLayout tilAmount, tilOfficerPin;
    private TextInputEditText etAmount, etOfficerPin;
    private AppCompatButton btnCancel, btnConfirm;

    private boolean isDepositOperation;
    private String customerName;
    private String customerAccountNumber;

    private DepositWithdrawListener listener;

    public interface DepositWithdrawListener {
        void onConfirmTransaction(boolean isDeposit, int amount, String officerPin);
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
        tilOfficerPin = view.findViewById(R.id.til_officer_pin);
        etOfficerPin = view.findViewById(R.id.et_officer_pin);
        btnCancel = view.findViewById(R.id.btn_cancel_deposit_withdraw);
        btnConfirm = view.findViewById(R.id.btn_confirm_deposit_withdraw);

        if (isDepositOperation) {
            tvDialogTitle.setText("Nạp tiền cho khách hàng");
        } else {
            tvDialogTitle.setText("Rút tiền cho khách hàng");
        }
        tvCustomerAccountInfo.setText("Tài khoản: " + customerAccountNumber + "\nTên: " + customerName);

        btnCancel.setOnClickListener(v -> dismiss());
        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String officerPinStr = etOfficerPin.getText().toString().trim();

            if (TextUtils.isEmpty(amountStr)) {
                tilAmount.setError("Vui lòng nhập số tiền");
                return;
            }
            int amountValue;
            try {
                amountValue = Integer.parseInt(amountStr);
                if (amountValue <= 0) {
                    tilAmount.setError("Số tiền phải lớn hơn 0");
                    return;
                }
                tilAmount.setError(null);
            } catch (NumberFormatException e) {
                tilAmount.setError("Số tiền không hợp lệ");
                return;
            }

            if (TextUtils.isEmpty(officerPinStr) || officerPinStr.length() != 6) {
                tilOfficerPin.setError("Mã PIN nhân viên phải có 6 chữ số");
                return;
            }
            tilOfficerPin.setError(null);

            if (listener != null) {
                listener.onConfirmTransaction(isDepositOperation, amountValue, officerPinStr);
            }
            dismiss();
        });

        builder.setView(view);
        return builder.create();
    }
}