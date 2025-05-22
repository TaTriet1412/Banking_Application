package com.example.bankingapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.example.bankingapplication.Adapter.TransactionHistoryAdapter;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.Query; // Cần import Query
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    private static final String TAG = "TransactionHistory";
    private Toolbar toolbar;
    private TextView tvUserName, tvAccountNumber, tvBalance, tvNoTransactions;
    private ImageView ivCopyAccountNumber, ivAccountDetailsArrow;
    private TabLayout tabLayout;
    private RecyclerView recyclerViewTransactions;
    private ProgressBar progressBar;

    private TransactionHistoryAdapter adapter;
    private List<TransactionData> allTransactions = new ArrayList<>();
    private List<TransactionData> filteredTransactions = new ArrayList<>();

    private Account currentAccount;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        toolbar = findViewById(R.id.toolbar_transaction_history);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tvUserName = findViewById(R.id.tv_user_name_history);
        tvAccountNumber = findViewById(R.id.tv_account_number_history);
        tvBalance = findViewById(R.id.tv_balance_history);
        ivCopyAccountNumber = findViewById(R.id.iv_copy_account_number_history);
        ivAccountDetailsArrow = findViewById(R.id.iv_account_details_arrow_history);
        tabLayout = findViewById(R.id.tab_layout_history);
        recyclerViewTransactions = findViewById(R.id.recycler_view_transactions);
        progressBar = findViewById(R.id.progress_bar_history);
        tvNoTransactions = findViewById(R.id.tv_no_transactions);

        currentAccount = GlobalVariables.getInstance().getCurrentAccount();
        currentUser = GlobalVariables.getInstance().getCurrentUser();

        if (currentAccount == null || currentUser == null) {
            Toast.makeText(this, "Lỗi: Không thể tải thông tin tài khoản.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupAccountInfo();
        setupRecyclerView();
        setupTabs();
        loadTransactionHistory();

        ivCopyAccountNumber.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Số tài khoản", currentAccount.getAccountNumber());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã sao chép số tài khoản", Toast.LENGTH_SHORT).show();
        });

        ivAccountDetailsArrow.setOnClickListener(v -> {
            // Mở màn hình chi tiết tài khoản (AccountActivity) nếu cần
            Intent intent = new Intent(this, AccountActivity.class);
            startActivity(intent);
        });
    }

    private void setupAccountInfo() {
        tvUserName.setText(currentUser.getName());
        tvAccountNumber.setText(currentAccount.getAccountNumber());
        if (currentAccount.getChecking() != null && currentAccount.getChecking().getBalance() != null) {
            tvBalance.setText(NumberFormat.convertToCurrencyFormatHasUnit(currentAccount.getChecking().getBalance()));
        } else {
            tvBalance.setText("N/A");
        }
    }

    private void setupRecyclerView() {
        adapter = new TransactionHistoryAdapter(this, filteredTransactions, currentAccount.getUID());
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTransactions.setAdapter(adapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerViewTransactions.getContext(),
                ((LinearLayoutManager) recyclerViewTransactions.getLayoutManager()).getOrientation());
        recyclerViewTransactions.addItemDecoration(dividerItemDecoration);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterTransactions(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        // Chọn tab "Toàn bộ" làm mặc định
        TabLayout.Tab defaultTab = tabLayout.getTabAt(0);
        if (defaultTab != null) {
            defaultTab.select();
        }
    }

    private void loadTransactionHistory() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoTransactions.setVisibility(View.GONE);
        recyclerViewTransactions.setVisibility(View.GONE);

        String accountUID = currentAccount.getUID();

        // Query for transactions where this account is the sender
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("transactions")
                .whereEqualTo("frontAccountId", accountUID)
                .orderBy("createdAt", Query.Direction.DESCENDING) // Sắp xếp mới nhất trước
                .get()
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task1.getResult()) {
                            TransactionData transaction = document.toObject(TransactionData.class);
                            transaction.setUID(document.getId());
                            allTransactions.add(transaction);
                        }
                        // Query for transactions where this account is the receiver
                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("transactions")
                                .whereEqualTo("toAccountId", accountUID)
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .get()
                                .addOnCompleteListener(task2 -> {
                                    progressBar.setVisibility(View.GONE);
                                    if (task2.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task2.getResult()) {
                                            TransactionData transaction = document.toObject(TransactionData.class);
                                            transaction.setUID(document.getId());
                                            // Tránh thêm trùng lặp nếu tài khoản tự chuyển cho chính mình
                                            // và giao dịch đó đã được lấy ở query đầu tiên
                                            boolean alreadyAdded = false;
                                            for (TransactionData existing : allTransactions) {
                                                if (existing.getUID().equals(transaction.getUID())) {
                                                    alreadyAdded = true;
                                                    break;
                                                }
                                            }
                                            if (!alreadyAdded) {
                                                allTransactions.add(transaction);
                                            }
                                        }
                                        // Sắp xếp lại toàn bộ danh sách theo thời gian giảm dần
                                        Collections.sort(allTransactions, (t1, t2) -> {
                                            if (t1.getCreatedAt() == null && t2.getCreatedAt() == null) return 0;
                                            if (t1.getCreatedAt() == null) return 1; // nulls last
                                            if (t2.getCreatedAt() == null) return -1;
                                            return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                                        });
                                        filterTransactions(tabLayout.getSelectedTabPosition());
                                    } else {
                                        Log.e(TAG, "Error getting incoming transactions: ", task2.getException());
                                        Toast.makeText(TransactionHistoryActivity.this, "Lỗi tải lịch sử giao dịch.", Toast.LENGTH_SHORT).show();
                                        tvNoTransactions.setVisibility(View.VISIBLE);
                                    }
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error getting outgoing transactions: ", task1.getException());
                        Toast.makeText(TransactionHistoryActivity.this, "Lỗi tải lịch sử giao dịch.", Toast.LENGTH_SHORT).show();
                        tvNoTransactions.setVisibility(View.VISIBLE);
                    }
                });
    }


    private void filterTransactions(int tabPosition) {
        filteredTransactions.clear();
        String accountUID = currentAccount.getUID();

        if (allTransactions.isEmpty() && tabPosition != -1) {
            recyclerViewTransactions.setVisibility(View.GONE);
            tvNoTransactions.setVisibility(View.VISIBLE);
            tvNoTransactions.setText("Không có giao dịch nào.");
            adapter.notifyDataSetChanged();
            return;
        }

        for (TransactionData transaction : allTransactions) {
            boolean isFailed = transaction.getStatus() != null && "failed".equalsIgnoreCase(transaction.getStatus());

            // Xử lý tab "Thất bại" trước tiên
            if (tabPosition == 3) { // Tab "Thất bại"
                if (isFailed) {
                    filteredTransactions.add(transaction);
                }
                continue; // Chuyển sang giao dịch tiếp theo sau khi xử lý tab "Thất bại"
            }

            // Nếu không phải tab "Thất bại" và giao dịch này bị thất bại, bỏ qua nó
            if (isFailed) {
                continue;
            }

            // Logic cho các tab còn lại (chỉ áp dụng cho giao dịch KHÔNG THẤT BẠI)
            boolean isCurrentAccountSender = accountUID.equals(transaction.getFrontAccountId());
            boolean isCurrentAccountReceiver = accountUID.equals(transaction.getToAccountId());
            String transactionType = transaction.getType() != null ? transaction.getType().toLowerCase() : "";

            switch (tabPosition) {
                case 0: // Toàn bộ (chỉ giao dịch không thất bại)
                    filteredTransactions.add(transaction);
                    break;
                case 1: // Tiền vào (chỉ giao dịch không thất bại)
                    // Là tiền vào nếu:
                    // 1. Tài khoản hiện tại là người nhận VÀ KHÔNG phải là người gửi (chuyển tiền từ người khác)
                    // 2. Hoặc loại giao dịch là "deposit" (nạp tiền) VÀ tài khoản hiện tại là người nhận
                    if ((isCurrentAccountReceiver && !isCurrentAccountSender) ||
                            ("deposit".equals(transactionType) && isCurrentAccountReceiver)) {
                        filteredTransactions.add(transaction);
                    }
                    break;
                case 2: // Tiền ra (chỉ giao dịch không thất bại)
                    // Là tiền ra nếu:
                    // 1. Tài khoản hiện tại là người gửi (bất kể người nhận là ai, kể cả chính mình cho một số loại giao dịch)
                    if (isCurrentAccountSender) {
                        filteredTransactions.add(transaction);
                    }
                    break;
            }
        }

        if (filteredTransactions.isEmpty()) {
            recyclerViewTransactions.setVisibility(View.GONE);
            tvNoTransactions.setVisibility(View.VISIBLE);
            if (tabPosition == 3) {
                tvNoTransactions.setText("Không có giao dịch thất bại nào.");
            } else {
                tvNoTransactions.setText("Không có giao dịch nào.");
            }
        } else {
            recyclerViewTransactions.setVisibility(View.VISIBLE);
            tvNoTransactions.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}