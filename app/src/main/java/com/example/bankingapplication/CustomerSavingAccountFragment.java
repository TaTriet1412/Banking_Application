package com.example.bankingapplication;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bankingapplication.Adapter.InterestHistoryAdapter;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.InterestLog;
import com.example.bankingapplication.Object.SavingAccount;
import com.example.bankingapplication.R;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerSavingAccountFragment extends Fragment {

    private static final String TAG = "CustSavingAccFragment";

    private SavingAccount savingAccountData;
    private String parentAccountId;

    private TextView tvTotalAccumulated, tvSavingBalanceValue, tvInterestRateValue;
    private ImageView ivSavingHistory;

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
        ivSavingHistory = view.findViewById(R.id.iv_saving_history);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        displayAccountInfo();
        
        // Setup click listener for saving history icon
        if (ivSavingHistory != null) {
            ivSavingHistory.setOnClickListener(v -> showInterestHistoryDialog());
        }
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
    
    private void showInterestHistoryDialog() {
        if (parentAccountId == null || parentAccountId.isEmpty()) {
            Toast.makeText(getContext(), "Không thể tải lịch sử tiết kiệm. ID tài khoản không hợp lệ.", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create and show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_interest_history, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        // Get views from dialog
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_view_interest_history);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_bar_interest_history);
        TextView tvNoHistory = dialogView.findViewById(R.id.tv_no_interest_history);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close_interest_dialog);
        TextView tvCurrentInterestRate = dialogView.findViewById(R.id.tv_current_interest_rate);
        TextView tvTotalInterestEarned = dialogView.findViewById(R.id.tv_total_interest_earned);
        
        // Set current interest rate
        double currentRate = savingAccountData != null ? savingAccountData.getInterestRate() : 0.0;
        tvCurrentInterestRate.setText(String.format(Locale.getDefault(), "%.2f%%", currentRate));
        
        // Setup recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<InterestLog> emptyList = new ArrayList<>();
        InterestHistoryAdapter adapter = new InterestHistoryAdapter(emptyList);
        recyclerView.setAdapter(adapter);
        
        // Setup close button
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        // Show progress while loading
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvNoHistory.setVisibility(View.GONE);
        
        // Load interest history
        loadInterestHistory(parentAccountId, new InterestHistoryCallback() {
            @Override
            public void onHistoryLoaded(List<InterestLog> interestLogs) {
                if (getContext() == null) return; // Fragment might be detached
                
                progressBar.setVisibility(View.GONE);
                
                if (interestLogs != null && !interestLogs.isEmpty()) {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvNoHistory.setVisibility(View.GONE);
                    adapter.updateData(interestLogs);
                    
                    // Calculate total interest earned
                    int totalInterest = 0;
                    for (InterestLog log : interestLogs) {
                        if (log.getInterestAmount() != null) {
                            totalInterest += log.getInterestAmount();
                        }
                    }
                    tvTotalInterestEarned.setText(NumberFormat.convertToCurrencyFormatHasUnit(totalInterest));
                } else {
                    recyclerView.setVisibility(View.GONE);
                    tvNoHistory.setVisibility(View.VISIBLE);
                    tvTotalInterestEarned.setText("0 VNĐ");
                }
            }

            @Override
            public void onError(Exception e) {
                if (getContext() == null) return; // Fragment might be detached
                
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
                tvNoHistory.setVisibility(View.VISIBLE);
                tvNoHistory.setText("Lỗi tải dữ liệu: " + e.getMessage());
                tvTotalInterestEarned.setText("0 VNĐ");
                
                Log.e(TAG, "Error loading interest history", e);
            }
        });
        
        dialog.show();
        
        // Set dialog height to half of screen height
        if (dialog.getWindow() != null) {
            int displayHeight = requireActivity().getWindowManager().getDefaultDisplay().getHeight();
            int displayWidth = requireActivity().getWindowManager().getDefaultDisplay().getWidth();
            int dialogHeight = (int)(displayHeight * 0.5); // 50% of screen height
            int dialogWidth = (int)(displayWidth * 0.9);  // 90% of screen width
            
            dialog.getWindow().setLayout(dialogWidth, dialogHeight);
        }
    }
    
    private void loadInterestHistory(String accountId, InterestHistoryCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference interestLogsRef = db.collection("accounts").document(accountId)
                .collection("interestlogs");
        
        interestLogsRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<InterestLog> interestLogs = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        InterestLog log = doc.toObject(InterestLog.class);
                        interestLogs.add(log);
                    }
                    
                    callback.onHistoryLoaded(interestLogs);
                })
                .addOnFailureListener(e -> callback.onError(e));
    }
    
    // Callback interface for interest history loading
    private interface InterestHistoryCallback {
        void onHistoryLoaded(List<InterestLog> interestLogs);
        void onError(Exception e);
    }
}