
package com.example.bankingapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
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
import com.example.bankingapplication.Object.CheckingAccount;
import com.example.bankingapplication.R;
import com.example.bankingapplication.Utils.NumberFormat;

public class CustomerCheckingAccountFragment extends Fragment {

    private CheckingAccount checkingAccountData;
    private String accountNumber;

    private TextView tvTotalBalance, tvAccountNumber, tvBalanceDetail;
    private ImageView ivCopyAccNum, ivViewDetailsArrow;
    private Button btnDefaultAccountIndicator;

    public static CustomerCheckingAccountFragment newInstance(CheckingAccount checkingAcc, String accNum) {
        CustomerCheckingAccountFragment fragment = new CustomerCheckingAccountFragment();
        Bundle args = new Bundle();
        if (checkingAcc != null) {
            args.putInt("balance", checkingAcc.getBalance() != null ? checkingAcc.getBalance() : 0);
        } else {
            args.putInt("balance", 0); // Mặc định nếu checkingAccount là null
        }
        args.putString("accountNumber", accNum);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            int balance = getArguments().getInt("balance", 0);
            accountNumber = getArguments().getString("accountNumber", "N/A");
            checkingAccountData = new CheckingAccount(balance);
        } else {
            checkingAccountData = new CheckingAccount(0); // Khởi tạo mặc định
            accountNumber = "N/A";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_checking_account_officer, container, false);

        tvTotalBalance = view.findViewById(R.id.tv_officer_checking_total_balance);
        tvAccountNumber = view.findViewById(R.id.tv_officer_checking_account_number);
        tvBalanceDetail = view.findViewById(R.id.tv_officer_checking_balance_detail);
        ivCopyAccNum = view.findViewById(R.id.iv_officer_checking_copy_acc_num);
        ivViewDetailsArrow = view.findViewById(R.id.iv_officer_checking_view_details_arrow);
        btnDefaultAccountIndicator = view.findViewById(R.id.btn_officer_checking_default_account_indicator);

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
            tvAccountNumber.setText(accountNumber);
            tvBalanceDetail.setText(NumberFormat.convertToCurrencyFormatHasUnit(checkingAccountData.getBalance()));
        } else {
            tvTotalBalance.setText(NumberFormat.convertToCurrencyFormatHasUnit(0));
            tvAccountNumber.setText("N/A");
            tvBalanceDetail.setText(NumberFormat.convertToCurrencyFormatHasUnit(0));
        }
        // Mặc định, nút "Tài khoản mặc định" có thể không cần thay đổi text, chỉ là chỉ báo
    }

    private void setupClickListeners() {
        ivCopyAccNum.setOnClickListener(v -> {
            if (getContext() != null && accountNumber != null && !accountNumber.equals("N/A")) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Số tài khoản", accountNumber);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Đã sao chép số tài khoản", Toast.LENGTH_SHORT).show();
            }
        });

        ivViewDetailsArrow.setOnClickListener(v -> {
            // TODO: Xử lý khi nhân viên muốn xem chi tiết hơn về tài khoản này (nếu có)
            // Hoặc nút này có thể không cần thiết nếu tất cả thông tin đã hiển thị.
            Toast.makeText(getContext(), "Xem chi tiết (đang phát triển)", Toast.LENGTH_SHORT).show();
        });
    }
}