package com.example.bankingapplication.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bankingapplication.Object.InterestLog;
import com.example.bankingapplication.R;
import com.example.bankingapplication.Utils.NumberFormat;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class InterestHistoryAdapter extends RecyclerView.Adapter<InterestHistoryAdapter.InterestViewHolder> {

    private List<InterestLog> interestLogs;

    public InterestHistoryAdapter(List<InterestLog> interestLogs) {
        this.interestLogs = interestLogs;
        // Sort by period (newest first)
        sortByNewestFirst();
    }

    private void sortByNewestFirst() {
        Collections.sort(interestLogs, new Comparator<InterestLog>() {
            @Override
            public int compare(InterestLog log1, InterestLog log2) {
                if (log1.getPeriod() == null && log2.getPeriod() == null) return 0;
                if (log1.getPeriod() == null) return 1;  // null values last
                if (log2.getPeriod() == null) return -1; // null values last
                // Compare timestamps in reverse order (newest first)
                return -log1.getPeriod().compareTo(log2.getPeriod());
            }
        });
    }

    @NonNull
    @Override
    public InterestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_interest_history, parent, false);
        return new InterestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InterestViewHolder holder, int position) {
        InterestLog log = interestLogs.get(position);
        
        // Format period as Month/Year
        if (log.getPeriod() != null) {
            Timestamp timestamp = log.getPeriod();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp.getSeconds() * 1000);
            
            // Format for display name (Month/Year)
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
            String formattedPeriod = "Tháng " + monthYearFormat.format(calendar.getTime());
            holder.tvInterestPeriod.setText(formattedPeriod);
            
            // Format for exact date
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvInterestDate.setText(dateFormat.format(calendar.getTime()));
        } else {
            holder.tvInterestPeriod.setText("N/A");
            holder.tvInterestDate.setText("");
        }
        
        // Format interest amount
        if (log.getInterestAmount() != null) {
            String formattedAmount = "+ " + NumberFormat.convertToCurrencyFormatHasUnit(log.getInterestAmount());
            holder.tvInterestAmount.setText(formattedAmount);
        } else {
            holder.tvInterestAmount.setText("N/A");
        }
        
        // Display interest rate
        if (log.getInterestRate() != null) {
            String formattedRate = String.format(Locale.getDefault(), "Lãi suất: %.2f%%", log.getInterestRate());
            holder.tvInterestRate.setText(formattedRate);
        } else {
            holder.tvInterestRate.setText("Lãi suất: N/A");
        }
    }

    @Override
    public int getItemCount() {
        return interestLogs != null ? interestLogs.size() : 0;
    }
    
    public void updateData(List<InterestLog> newLogs) {
        this.interestLogs = newLogs;
        sortByNewestFirst();
        notifyDataSetChanged();
    }

    static class InterestViewHolder extends RecyclerView.ViewHolder {
        TextView tvInterestPeriod;
        TextView tvInterestAmount;
        TextView tvInterestRate;
        TextView tvInterestDate;

        public InterestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInterestPeriod = itemView.findViewById(R.id.tv_interest_period);
            tvInterestAmount = itemView.findViewById(R.id.tv_interest_amount);
            tvInterestRate = itemView.findViewById(R.id.tv_interest_rate);
            tvInterestDate = itemView.findViewById(R.id.tv_interest_date);
        }
    }
}
