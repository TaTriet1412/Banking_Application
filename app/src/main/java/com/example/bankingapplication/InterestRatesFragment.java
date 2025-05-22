package com.example.bankingapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bankingapplication.Adapter.InterestRateAdapter;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InterestRatesFragment extends Fragment {

    private static final String TAG = "InterestRatesFragment";
    private static final double MIN_INTEREST_RATE = 0.0;
    private static final double MAX_INTEREST_RATE = 20.0;

    private RecyclerView recyclerViewInterestRates;
    private InterestRateAdapter adapter;
    private List<Account> accountsList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView textEmptyState;
    private TextInputLayout layoutDefaultInterestRate;
    private TextInputEditText editDefaultInterestRate;
    private AppCompatButton btnUpdateDefaultRate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interest_rates, container, false);
        
        recyclerViewInterestRates = view.findViewById(R.id.recycler_view_interest_rates);
        progressBar = view.findViewById(R.id.progress_bar_interest_rates);
        textEmptyState = view.findViewById(R.id.text_empty_state);
        layoutDefaultInterestRate = view.findViewById(R.id.layout_default_interest_rate);
        editDefaultInterestRate = view.findViewById(R.id.edit_default_interest_rate);
        btnUpdateDefaultRate = view.findViewById(R.id.btn_update_default_rate);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRecyclerView();
        setupDefaultRateButton();
        loadAccounts();
    }

    private void setupRecyclerView() {
        recyclerViewInterestRates.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InterestRateAdapter(accountsList, this::updateAccountInterestRate);
        recyclerViewInterestRates.setAdapter(adapter);
    }

    private void setupDefaultRateButton() {
        btnUpdateDefaultRate.setOnClickListener(v -> {
            String rateStr = editDefaultInterestRate.getText().toString().trim();
            if (TextUtils.isEmpty(rateStr)) {
                layoutDefaultInterestRate.setError("Vui lòng nhập lãi suất");
                return;
            }
            
            try {
                double rate = Double.parseDouble(rateStr);
                if (rate < MIN_INTEREST_RATE || rate > MAX_INTEREST_RATE) {
                    layoutDefaultInterestRate.setError(
                            String.format(Locale.US, "Lãi suất phải từ %.1f%% đến %.1f%%", 
                                    MIN_INTEREST_RATE, MAX_INTEREST_RATE));
                    return;
                }
                
                updateAllAccountsInterestRate(rate);
                
            } catch (NumberFormatException e) {
                layoutDefaultInterestRate.setError("Giá trị lãi suất không hợp lệ");
            }
        });
    }

    private void loadAccounts() {
        showLoading(true);
        
        Firestore.getAllAccounts((accountList, e) -> {
            showLoading(false);
            
            if (e != null) {
                Log.e(TAG, "Error loading accounts: ", e);
                showEmptyState("Lỗi khi tải danh sách tài khoản");
                return;
            }
            
            if (accountList != null && !accountList.isEmpty()) {
                accountsList.clear();
                for (Account account : accountList) {
                    if (account.getSaving() != null) {
                        accountsList.add(account);
                    }
                }
                
                if (accountsList.isEmpty()) {
                    showEmptyState("Không tìm thấy tài khoản tiết kiệm nào");
                } else {
                    hideEmptyState();
                    adapter.notifyDataSetChanged();
                }
            } else {
                showEmptyState("Không tìm thấy tài khoản nào");
            }
        });
    }

    private void updateAccountInterestRate(String accountId, double newRate) {
        if (accountId == null || accountId.isEmpty()) {
            Toast.makeText(getContext(), "ID tài khoản không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("saving.interestRate", newRate);
        
        Firestore.updateAccountFields(accountId, updates, (isSuccess, e) -> {
            if (getContext() == null) return;
            
            if (isSuccess) {
                Toast.makeText(getContext(), "Cập nhật lãi suất thành công", Toast.LENGTH_SHORT).show();
                // Update local data
                for (int i = 0; i < accountsList.size(); i++) {
                    Account account = accountsList.get(i);
                    if (account.getUID().equals(accountId) && account.getSaving() != null) {
                        account.getSaving().setInterestRate(newRate);
                        adapter.notifyItemChanged(i);
                        break;
                    }
                }
            } else {
                String errorMsg = "Lỗi: Không thể cập nhật lãi suất";
                if (e != null && e.getMessage() != null) {
                    errorMsg += " - " + e.getMessage();
                }
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error updating interest rate", e);
            }
        });
    }

    private void updateAllAccountsInterestRate(double newRate) {
        showLoading(true);
        
        // Proceed with each account update
        int totalAccounts = accountsList.size();
        final int[] updatedCount = {0};
        final int[] failedCount = {0};
        
        if (totalAccounts == 0) {
            showLoading(false);
            Toast.makeText(getContext(), "Không có tài khoản để cập nhật", Toast.LENGTH_SHORT).show();
            return;
        }
        
        for (Account account : accountsList) {
            if (account.getUID() == null || account.getSaving() == null) {
                failedCount[0]++;
                checkCompletion(totalAccounts, updatedCount[0], failedCount[0]);
                continue;
            }
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("saving.interestRate", newRate);
            
            Firestore.updateAccountFields(account.getUID(), updates, (isSuccess, e) -> {
                if (isSuccess) {
                    account.getSaving().setInterestRate(newRate);
                    updatedCount[0]++;
                } else {
                    failedCount[0]++;
                    Log.e(TAG, "Failed to update account " + account.getUID(), e);
                }
                checkCompletion(totalAccounts, updatedCount[0], failedCount[0]);
            });
        }
    }

    private void checkCompletion(int total, int success, int fail) {
        if (success + fail == total) {
            showLoading(false);
            if (getContext() != null) {
                if (fail == 0) {
                    Toast.makeText(getContext(), 
                            "Cập nhật lãi suất thành công cho tất cả " + success + " tài khoản", 
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), 
                            "Cập nhật " + success + " tài khoản thành công, " + fail + " thất bại", 
                            Toast.LENGTH_LONG).show();
                }
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewInterestRates != null) {
            recyclerViewInterestRates.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void showEmptyState(String message) {
        if (textEmptyState != null) {
            textEmptyState.setText(message);
            textEmptyState.setVisibility(View.VISIBLE);
        }
        if (recyclerViewInterestRates != null) {
            recyclerViewInterestRates.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (textEmptyState != null) {
            textEmptyState.setVisibility(View.GONE);
        }
        if (recyclerViewInterestRates != null) {
            recyclerViewInterestRates.setVisibility(View.VISIBLE);
        }
    }
}
