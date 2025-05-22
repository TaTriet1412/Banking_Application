package com.example.bankingapplication.Adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.R;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Locale;

public class InterestRateAdapter extends RecyclerView.Adapter<InterestRateAdapter.InterestRateViewHolder> {

    private final List<Account> accounts;
    private final OnInterestRateUpdateListener updateListener;
    
    // Interface for rate update callbacks
    public interface OnInterestRateUpdateListener {
        void onInterestRateUpdate(String accountId, double newRate);
    }

    public InterestRateAdapter(List<Account> accounts, OnInterestRateUpdateListener listener) {
        this.accounts = accounts;
        this.updateListener = listener;
    }

    @NonNull
    @Override
    public InterestRateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_interest_rate, parent, false);
        return new InterestRateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InterestRateViewHolder holder, int position) {
        Account account = accounts.get(position);
        holder.bind(account);
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    public void updateBankOfficeList(List<Account> newAccounts) {
        this.accounts.clear();
        this.accounts.addAll(newAccounts);
        notifyDataSetChanged();
    }

    class InterestRateViewHolder extends RecyclerView.ViewHolder {
        private final TextView textAccountId;
        private final TextView textAccountOwner;
        private final TextView textCurrentRate;
        private final TextInputLayout layoutNewRate;
        private final EditText editNewRate;
        private final AppCompatButton btnUpdateRate;

        public InterestRateViewHolder(@NonNull View itemView) {
            super(itemView);
            textAccountId = itemView.findViewById(R.id.text_account_id);
            textAccountOwner = itemView.findViewById(R.id.text_account_owner);
            textCurrentRate = itemView.findViewById(R.id.text_current_rate);
            layoutNewRate = itemView.findViewById(R.id.layout_new_rate);
            editNewRate = itemView.findViewById(R.id.edit_new_rate);
            btnUpdateRate = itemView.findViewById(R.id.btn_update_rate);
        }

        public void bind(Account account) {
            if (account.getAccountNumber() != null) {
                textAccountId.setText(account.getAccountNumber());
            } else {
                textAccountId.setText(account.getUID());
            }

            // Get the account owner name - Account doesn't have getUserName(), use another property
            // or get from FireStore in the fragment
            textAccountOwner.setText("Tài khoản #" + (account.getAccountNumber() != null ? account.getAccountNumber() : account.getUID()));

            double currentRate = account.getSaving() != null ? account.getSaving().getInterestRate() : 0.0;
            textCurrentRate.setText(String.format(Locale.getDefault(), "%.2f%%", currentRate));

            // Reset input fields
            editNewRate.setText("");
            layoutNewRate.setError(null);

            // Input validation
            editNewRate.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Clear error when text changes
                    layoutNewRate.setError(null);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            btnUpdateRate.setOnClickListener(v -> {
                String rateText = editNewRate.getText().toString().trim();
                if (rateText.isEmpty()) {
                    layoutNewRate.setError("Vui lòng nhập lãi suất mới");
                    return;
                }

                try {
                    double newRate = Double.parseDouble(rateText);
                    if (newRate < 0.0 || newRate > 20.0) {
                        layoutNewRate.setError("Lãi suất phải từ 0.0% đến 20.0%");
                        return;
                    }

                    // Call the update listener
                    if (updateListener != null) {
                        updateListener.onInterestRateUpdate(account.getUID(), newRate);
                    }
                } catch (NumberFormatException e) {
                    layoutNewRate.setError("Giá trị không hợp lệ");
                }
            });
        }
    }
}
