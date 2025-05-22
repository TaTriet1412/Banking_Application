package com.example.bankingapplication.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bankingapplication.Object.TransactionData;
import com.example.bankingapplication.R;
import com.example.bankingapplication.TransactionDetailActivity; // Sẽ tạo sau
import com.example.bankingapplication.Utils.NumberFormat;
import com.example.bankingapplication.Utils.TimeUtils; // Bạn cần tạo class này
import com.google.firebase.Timestamp;
import java.util.List;

public class TransactionHistoryAdapter extends RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder> {

    private List<TransactionData> transactionList;
    private Context context;
    private String currentAccountId; // ID của tài khoản hiện tại để xác định tiền vào/ra

    public TransactionHistoryAdapter(Context context, List<TransactionData> transactionList, String currentAccountId) {
        this.context = context;
        this.transactionList = transactionList;
        this.currentAccountId = currentAccountId;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction_history, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionData transaction = transactionList.get(position);

        // Hiển thị ngày
        if (transaction.getCreatedAt() != null) {
            holder.tvTransactionDate.setText(TimeUtils.formatFirebaseTimestamp(transaction.getCreatedAt()).substring(0, 10));
        } else {
            holder.tvTransactionDate.setText("N/A");
        }

        // Hiển thị mô tả
        holder.tvTransactionDescription.setText(transaction.getDescription() != null ? transaction.getDescription() : "Không có mô tả");

        // Kiểm tra trạng thái thất bại
        boolean isFailed = transaction.getStatus() != null && "failed".equalsIgnoreCase(transaction.getStatus());

        if (isFailed) {
            holder.tvTransactionStatusTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvTransactionStatusTag.setVisibility(View.GONE);
        }

        // Hiển thị số tiền và màu sắc
        Integer amount = transaction.getAmount();
        if (amount != null) {
            if (isFailed) {
                // Giao dịch thất bại
                holder.tvTransactionAmount.setText(NumberFormat.convertToCurrencyFormatHasUnit(amount));
                holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary)); // Màu xám
            } else {
                // Giao dịch không thất bại
                boolean isCurrentAccountSender = currentAccountId.equals(transaction.getFrontAccountId());
                boolean isCurrentAccountReceiver = currentAccountId.equals(transaction.getToAccountId());
                String transactionType = transaction.getType() != null ? transaction.getType().toLowerCase() : "";

                if (isCurrentAccountReceiver && !isCurrentAccountSender) { // Tiền vào thuần túy
                    holder.tvTransactionAmount.setText("+ " + NumberFormat.convertToCurrencyFormatHasUnit(amount));
                    holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.green_primary));
                } else if (isCurrentAccountSender && !isCurrentAccountReceiver) { // Tiền ra thuần túy
                    holder.tvTransactionAmount.setText("- " + NumberFormat.convertToCurrencyFormatHasUnit(amount));
                    holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.red_error));
                } else if (isCurrentAccountSender && isCurrentAccountReceiver) { // Tự chuyển cho chính mình hoặc các loại giao dịch đặc biệt
                    if ("payment".equals(transactionType) || "withdrawal".equals(transactionType) || "phone_recharge".equals(transactionType)) { // Các loại tiền ra
                        holder.tvTransactionAmount.setText("- " + NumberFormat.convertToCurrencyFormatHasUnit(amount));
                        holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.red_error));
                    } else if ("deposit".equals(transactionType)) { // Nạp tiền vào tài khoản
                        holder.tvTransactionAmount.setText("+ " + NumberFormat.convertToCurrencyFormatHasUnit(amount));
                        holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.green_primary));
                    } else { // Trường hợp tự chuyển tiền không rõ (hiếm)
                        holder.tvTransactionAmount.setText(NumberFormat.convertToCurrencyFormatHasUnit(amount));
                        holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
                    }
                } else if ("deposit".equals(transactionType) && isCurrentAccountReceiver){ // Nạp tiền vào tài khoản từ nguồn không xác định rõ là tài khoản khác trong hệ thống
                    holder.tvTransactionAmount.setText("+ " + NumberFormat.convertToCurrencyFormatHasUnit(amount));
                    holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.green_primary));
                } else if (("payment".equals(transactionType) || "withdrawal".equals(transactionType) || "phone_recharge".equals(transactionType)) && isCurrentAccountSender) { // Thanh toán từ tài khoản này
                    holder.tvTransactionAmount.setText("- " + NumberFormat.convertToCurrencyFormatHasUnit(amount));
                    holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.red_error));
                }
                else { // Trường hợp không xác định rõ chiều (cần xem lại logic nếu gặp)
                    holder.tvTransactionAmount.setText(NumberFormat.convertToCurrencyFormatHasUnit(amount));
                    holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
                    Log.w("TransactionAdapter", "Giao dịch không rõ chiều: ID " + transaction.getUID() + ", Type: " + transaction.getType() + ", From: " + transaction.getFrontAccountId() + ", To: " + transaction.getToAccountId());
                }
            }
        } else {
            holder.tvTransactionAmount.setText("N/A");
            holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
        }

        // Xử lý click item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TransactionDetailActivity.class);
            intent.putExtra("TRANSACTION_ID", transaction.getUID());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public void updateData(List<TransactionData> newTransactions) {
        this.transactionList.clear();
        this.transactionList.addAll(newTransactions);
        notifyDataSetChanged();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTransactionDate, tvTransactionDescription, tvTransactionAmount;
        TextView tvTransactionStatusTag;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionDate = itemView.findViewById(R.id.tv_transaction_date);
            tvTransactionDescription = itemView.findViewById(R.id.tv_transaction_description);
            tvTransactionAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvTransactionStatusTag = itemView.findViewById(R.id.tv_transaction_status_tag);
        }
    }
}