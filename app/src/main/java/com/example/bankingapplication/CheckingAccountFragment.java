package com.example.bankingapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;

public class CheckingAccountFragment extends Fragment {
    TextView tv_account_number, tv_balance, tv_balance_sub;
    ImageView iv_copy, iv_arrow_details;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.activity_checking_account_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tv_account_number = view.findViewById(R.id.tv_account_number);
        tv_balance = view.findViewById(R.id.tv_balance);
        tv_balance_sub = view.findViewById(R.id.tv_balance_sub);
        iv_copy = view.findViewById(R.id.iv_copy);
        iv_arrow_details = view.findViewById(R.id.iv_arrow_details);

        initViews();
    }

    private void initViews() {
        Account account = GlobalVariables.getInstance().getCurrentAccount();

        // Set the account number and balance
        tv_account_number.setText(account.getAccountNumber());
        tv_balance.setText(NumberFormat.convertToCurrencyFormatHasUnit((account.getChecking().getBalance())));
        tv_balance_sub.setText(NumberFormat.convertToCurrencyFormatHasUnit((account.getChecking().getBalance())));

        clickListeners();
    }

    private void clickListeners() {
        // Set click listeners for the copy and arrow icons
        copyCLickEvent();
        arrowClickEvent();
    }

    private void copyCLickEvent() {
        iv_copy.setOnClickListener(v -> {
            // Handle copy account number action
            // You can use ClipboardManager to copy the account number to the clipboard
            // For example:
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Account Number", tv_account_number.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Đã sao chép số tài khoản", Toast.LENGTH_SHORT).show();
        });
    }

    private void arrowClickEvent() {
        iv_arrow_details.setOnClickListener(v -> {
            // Handle arrow details action
        });
    }

}