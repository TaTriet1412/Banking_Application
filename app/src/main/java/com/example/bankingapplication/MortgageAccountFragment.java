package com.example.bankingapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.MortgageAccount;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.example.bankingapplication.Utils.TimeUtils;

public class MortgageAccountFragment extends Fragment {

    TextView tv_loan_amount, tv_remaining_amount, tv_interest_rate,
            tv_payment_amount,  tv_start_date, tv_end_date,
            tv_payment_frequency;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        View rootView = inflater.inflate(R.layout.activity_mortgage_account_fragment, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize views
        tv_loan_amount = view.findViewById(R.id.tv_loan_amount);
        tv_remaining_amount = view.findViewById(R.id.tv_remaining_amount);
        tv_interest_rate = view.findViewById(R.id.tv_interest_rate);
        tv_payment_amount = view.findViewById(R.id.tv_payment_amount);
        tv_start_date = view.findViewById(R.id.tv_start_date);
        tv_end_date = view.findViewById(R.id.tv_end_date);
        tv_payment_frequency = view.findViewById(R.id.tv_payment_frequency);

        // Set the data to the views
        initData();
    }

    private void initData() {
        // Assuming you have a MortgageAccount object with the necessary data
        Account account = GlobalVariables.getInstance().getCurrentAccount();
        MortgageAccount mortgageAccount = account.getMortgage();

        if (mortgageAccount != null) {
            tv_loan_amount.setText(NumberFormat.convertToCurrencyFormatHasUnit(mortgageAccount.getLoanAmount()));
            tv_remaining_amount.setText(NumberFormat.convertToCurrencyFormatHasUnit(mortgageAccount.getRemainingAmount()));
            tv_interest_rate.setText(String.valueOf(mortgageAccount.getInterestRate()));
            tv_payment_amount.setText(NumberFormat.convertToCurrencyFormatHasUnit(mortgageAccount.getPaymentAmount()));
            tv_start_date.setText(TimeUtils.formatFirebaseTimestamp(mortgageAccount.getStartDate()));
            tv_end_date.setText(TimeUtils.formatFirebaseTimestamp(mortgageAccount.getEndDate()));
            tv_payment_frequency.setText(mortgageAccount.getPaymentFrequency());
        }
    }


}