package com.example.bankingapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.SavingAccount;
import com.example.bankingapplication.R;
import com.example.bankingapplication.Utils.NumberFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class CustomerSavingAccountFragment extends Fragment {

    private static final String TAG = "CustSavingAccFragment";

    private SavingAccount savingAccountData;
    private String parentAccountId;

    private TextView tvTotalAccumulated, tvSavingBalanceValue, tvInterestRateValue;

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
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        displayAccountInfo();
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
}