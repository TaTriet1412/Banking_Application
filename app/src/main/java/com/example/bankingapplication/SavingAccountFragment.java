package com.example.bankingapplication;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.InterestLog;
import com.example.bankingapplication.Object.SavingAccount;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavingAccountFragment extends Fragment {
    TextView tv_balance, tv_balance_sub, tv_interest_rate;
    ImageView ivViewInterestHistory; // Added for the history icon

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
        ivViewInterestHistory = view.findViewById(R.id.iv_view_interest_history);
        
        // Initialize data
        initData();
        
        // Set click listener for history icon
        ivViewInterestHistory.setOnClickListener(v -> showInterestHistoryDialog());
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

    private void showInterestHistoryDialog() {
        Account currentAccount = GlobalVariables.getInstance().getCurrentAccount();
        if (currentAccount == null || currentAccount.getUID() == null || currentAccount.getUID().isEmpty()) {
            Toast.makeText(getContext(), "Không thể tải lịch sử tiết kiệm. ID tài khoản không hợp lệ.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        String accountId = currentAccount.getUID();
        
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
        double currentRate = currentAccount.getSaving() != null ? currentAccount.getSaving().getInterestRate() : 0.0;
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
        loadInterestHistory(accountId, new InterestHistoryCallback() {
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