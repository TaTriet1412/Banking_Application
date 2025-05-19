package com.example.bankingapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.SavingAccount;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;

public class SavingAccountFragment extends Fragment {
    TextView tv_balance, tv_balance_sub, tv_interest_rate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.activity_saving_account_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize views
        tv_balance = view.findViewById(R.id.tv_balance);
        tv_balance_sub = view.findViewById(R.id.tv_balance_sub);
        tv_interest_rate = view.findViewById(R.id.tv_interest_rate);
        
        initData();
    }
    
    private void initData() {
        // Assuming you have a SavingAccount object with the necessary data
        Account account = GlobalVariables.getInstance().getCurrentAccount();
        SavingAccount savingAccount = account.getSaving();
        if (savingAccount != null) {
            tv_balance.setText(NumberFormat.convertToCurrencyFormatOnlyNumber(savingAccount.getBalance()));
            tv_balance_sub.setText(NumberFormat.convertToCurrencyFormatOnlyNumber(savingAccount.getBalance()));
            tv_interest_rate.setText(String.valueOf(savingAccount.getInterestRate()));
        }
    }
}