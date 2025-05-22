package com.example.bankingapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.Utils.NumberFormat;
import com.example.bankingapplication.Utils.TimeUtils;
import com.google.firebase.Timestamp;

public class TransactionDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvTransactionId, tvTransactionDate, tvTransactionAmount, tvTransactionDescription;
    private String transactionIdExtra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        toolbar = findViewById(R.id.toolbar_transaction_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tvTransactionId = findViewById(R.id.tv_detail_transaction_id);
        tvTransactionDate = findViewById(R.id.tv_detail_transaction_date);
        tvTransactionAmount = findViewById(R.id.tv_detail_transaction_amount);
        tvTransactionDescription = findViewById(R.id.tv_detail_transaction_description);

        transactionIdExtra = getIntent().getStringExtra("TRANSACTION_ID");

        if (transactionIdExtra != null && !transactionIdExtra.isEmpty()) {
            loadTransactionDetails();
        } else {
            Toast.makeText(this, "Lỗi: Không có thông tin giao dịch.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadTransactionDetails() {
        Firestore.getTransactionById(transactionIdExtra, transaction -> {
            if (transaction != null) {
                // Hiển thị mã giao dịch: lấy 4 số đầu và 4 số cuối của UID
                String displayId = transaction.getUID();
                if (displayId.length() > 8) {
                    displayId = displayId.substring(0, 4) + " - " + displayId.substring(displayId.length() - 4);
                }
                tvTransactionId.setText(displayId);

                if (transaction.getCreatedAt() != null) {
                    tvTransactionDate.setText(TimeUtils.formatFirebaseTimestamp(transaction.getCreatedAt()).substring(0, 10));
                } else {
                    tvTransactionDate.setText("N/A");
                }

                if (transaction.getAmount() != null) {
                    tvTransactionAmount.setText(NumberFormat.convertToCurrencyFormatHasUnit(transaction.getAmount()));
                } else {
                    tvTransactionAmount.setText("N/A");
                }

                tvTransactionDescription.setText(transaction.getDescription() != null ? transaction.getDescription() : "Không có mô tả");

            } else {
                Toast.makeText(this, "Không thể tải chi tiết giao dịch.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
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