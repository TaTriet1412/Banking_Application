package com.example.bankingapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransactionActivity extends AppCompatActivity {

    private static final String TAG = "TransactionActivity";
    private static final int MIN_AMOUNT = 1000;
    private static final int MAX_AMOUNT = 100000000;

    TextInputEditText etRecipientAccount, etRecipientName, etAmount, etDescription;
    TextInputLayout tilRecipientName;
    AppCompatButton btnContinue;
    Toolbar toolbar;
    FrameLayout progressOverlay;

    TextView tvSourceAccountNumber, tvBalanceAmount;
    
    // Store the list of available accounts
    private List<Account> availableAccounts = new ArrayList<>();
    private Account currentAccount;

    private Account recipientAccountObject = null; // Add this field to store the found account

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transaction);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, v.getPaddingTop(), systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize views
        tvSourceAccountNumber = findViewById(R.id.tvSourceAccountNumber);
        tvBalanceAmount = findViewById(R.id.tvBalanceAmount);
        etRecipientAccount = findViewById(R.id.etRecipientAccount);
        tilRecipientName = findViewById(R.id.tilRecipientName);
        etRecipientName = findViewById(R.id.etRecipientName);
        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        btnContinue = findViewById(R.id.btnContinue);
        progressOverlay = findViewById(R.id.progress_overlay);

        // Get current account from global variables
        currentAccount = GlobalVariables.getInstance().getCurrentAccount();
        if (currentAccount == null) {
            Toast.makeText(this, "Lỗi: Không thể tải thông tin tài khoản", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set current account info
        tvSourceAccountNumber.setText(currentAccount.getAccountNumber());
        if (currentAccount.getChecking() != null && currentAccount.getChecking().getBalance() != null) {
            tvBalanceAmount.setText(NumberFormat.convertToCurrencyFormatHasUnit(currentAccount.getChecking().getBalance()));
        } else {
            tvBalanceAmount.setText("0 VND");
        }
        
        // Set default description with current user's name
        User currentUser = GlobalVariables.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getName() != null) {
            etDescription.setText(currentUser.getName() + " chuyển tiền");
        }

        // Load available accounts for transfer
        loadAvailableAccounts();

        etRecipientAccount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String accountNumber = s.toString().trim();
                // Only check recipient account if it's exactly 10 digits
                if (accountNumber.length() == 10) {
                    checkRecipientAccount(accountNumber);
                } else {
                    tilRecipientName.setVisibility(View.GONE);
                    etRecipientName.setText("");
                }
            }
        });

        btnContinue.setOnClickListener(v_continue -> {
            String recipientAccount = etRecipientAccount.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (recipientAccount.isEmpty()) {
                etRecipientAccount.setError("Vui lòng nhập số tài khoản người nhận");
                etRecipientAccount.requestFocus();
                return;
            }

            if (tilRecipientName.getVisibility() != View.VISIBLE) {
                Toast.makeText(TransactionActivity.this, "Số tài khoản không hợp lệ", Toast.LENGTH_SHORT).show();
                etRecipientAccount.requestFocus();
                return;
            }

            if (amountStr.isEmpty()) {
                etAmount.setError("Vui lòng nhập số tiền");
                etAmount.requestFocus();
                return;
            }
            
            // Parse and validate amount
            int amount;
            try {
                amount = Integer.parseInt(amountStr);
            } catch (NumberFormatException e) {
                etAmount.setError("Số tiền không hợp lệ");
                etAmount.requestFocus();
                return;
            }
            
            // Check minimum amount
            if (amount < MIN_AMOUNT) {
                etAmount.setError("Số tiền tối thiểu là " + NumberFormat.convertToCurrencyFormatHasUnit(MIN_AMOUNT));
                etAmount.requestFocus();
                return;
            }
            
            // Check maximum amount
            if (amount > MAX_AMOUNT) {
                etAmount.setError("Số tiền tối đa là " + NumberFormat.convertToCurrencyFormatHasUnit(MAX_AMOUNT));
                etAmount.requestFocus();
                return;
            }
            
            // Check if account has sufficient balance
            if (currentAccount.getChecking() == null || 
                currentAccount.getChecking().getBalance() == null || 
                currentAccount.getChecking().getBalance() < amount) {
                
                Toast.makeText(TransactionActivity.this, 
                    "Số dư không đủ để thực hiện giao dịch này", 
                    Toast.LENGTH_SHORT).show();
                etAmount.requestFocus();
                return;
            }

            if (description.isEmpty()) {
                etDescription.setError("Vui lòng nhập nội dung chuyển tiền");
                etDescription.requestFocus();
                return;
            }

            String recipientName = etRecipientName.getText().toString().trim();
            if (recipientName.isEmpty()) {
                Toast.makeText(TransactionActivity.this, "Không tìm thấy thông tin người nhận", Toast.LENGTH_SHORT).show();
                return;
            }

            if (recipientAccountObject == null) {
                Toast.makeText(TransactionActivity.this, "Không tìm thấy thông tin tài khoản người nhận", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(TransactionActivity.this, "Đang xử lý chuyển tiền cho " + recipientName + "...", Toast.LENGTH_SHORT).show();
            
            Intent confirmIntent = new Intent(TransactionActivity.this, ConfirmTransactionActivity.class);
            confirmIntent.putExtra("SOURCE_ACCOUNT", tvSourceAccountNumber.getText().toString());
            confirmIntent.putExtra("RECIPIENT_ACCOUNT", recipientAccount);
            confirmIntent.putExtra("RECIPIENT_NAME", recipientName);
            confirmIntent.putExtra("RECIPIENT_BANK_NAME", "3T Banking");
            confirmIntent.putExtra("CONTENT", description);
            confirmIntent.putExtra("AMOUNT", amountStr);
            confirmIntent.putExtra("TO_ACCOUNT_ID", recipientAccountObject.getUID()); // Pass the recipient account ID
            startActivity(confirmIntent);
        });
    }

    private void loadAvailableAccounts() {
        progressOverlay.setVisibility(View.VISIBLE);
        
        Firestore.getAllAccounts(new Firestore.FirestoreGetAllAccountsCallback() {
            @Override
            public void onCallback(List<Account> accountList, Exception e) {
                progressOverlay.setVisibility(View.GONE);
                
                if (e != null) {
                    Log.e(TAG, "Error loading accounts: ", e);
                    Toast.makeText(TransactionActivity.this, "Lỗi khi tải danh sách tài khoản", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (accountList != null) {
                    // Filter out the current account
                    for (Account account : accountList) {
                        if (account != null && !account.getUID().equals(currentAccount.getUID())) {
                            availableAccounts.add(account);
                        }
                    }
                    
                    Log.d(TAG, "Loaded " + availableAccounts.size() + " available accounts for transfer");
                }
            }
        });
    }

    private void checkRecipientAccount(String accountNumber) {
        // First check in the locally loaded accounts
        for (Account account : availableAccounts) {
            if (account.getAccountNumber().equals(accountNumber)) {
                // Found a matching account
                recipientAccountObject = account; // Store the account object
                showRecipientInfo(account);
                return;
            }
        }

        // If not found locally, try to fetch from Firestore
        progressOverlay.setVisibility(View.VISIBLE);
        
        // Using a custom query to find the account by account number
        Firestore.getAccountByAccountNumber(accountNumber, new Firestore.FirestoreGetAccountCallback() {
            @Override
            public void onCallback(Account account) {
                progressOverlay.setVisibility(View.GONE);
                
                if (account != null && !account.getUID().equals(currentAccount.getUID())) {
                    // Add to local cache for future lookups
                    recipientAccountObject = account; // Store the account object
                    if (!availableAccounts.contains(account)) {
                        availableAccounts.add(account);
                    }
                    showRecipientInfo(account);
                } else {
                    tilRecipientName.setVisibility(View.GONE);
                    etRecipientName.setText("");
                    recipientAccountObject = null;
                }
            }
        });
    }

    private void showRecipientInfo(Account account) {
        // Find the user associated with this account
        Firestore.getUser(account.getUserId(), user -> {
            if (user != null) {
                tilRecipientName.setVisibility(View.VISIBLE);
                etRecipientName.setText(user.getName());
            } else {
                tilRecipientName.setVisibility(View.GONE);
                etRecipientName.setText("");
                Toast.makeText(TransactionActivity.this, "Không tìm thấy thông tin người nhận", Toast.LENGTH_SHORT).show();
            }
        });
    }
}