package com.example.bankingapplication; // Hoặc package của bạn

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Hoặc MaterialButton
import android.widget.TextView;
// import android.widget.Toast; // Không dùng Toast trong code này hiện tại

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bankingapplication.Object.MortgageAccount;
import com.example.bankingapplication.R;
import com.example.bankingapplication.Utils.AccountUtils;
import com.example.bankingapplication.Utils.NumberFormat;
import com.example.bankingapplication.Utils.TimeUtils;
import com.google.firebase.Timestamp;

public class CustomerMortgageAccountFragment extends Fragment {

    private static final String TAG = "CustMortgageFrag";

    private MortgageAccount mortgageAccountData;
    // private String accountNumber; // Bạn có thể quyết định có cần hiển thị số TK chung ở đây không
    private String parentAccountId; // Để lưu ID của Account cha

    private TextView tvLoanAmountHeader, tvRemainingAmountValue, tvInterestRateValue;
    private TextView tvStartDateValue, tvEndDateValue, tvPaymentAmountValue, tvPaymentFrequencyValue;
    private Button btnMortgageDetailsIndicator; // Hoặc MaterialButton

    // Keys cho Bundle arguments
    private static final String ARG_LOAN_AMOUNT = "loanAmount";
    private static final String ARG_REMAINING_AMOUNT = "remainingAmount";
    private static final String ARG_INTEREST_RATE = "interestRateVal";
    private static final String ARG_PAYMENT_AMOUNT = "paymentAmountVal";
    private static final String ARG_PAYMENT_FREQUENCY = "paymentFrequencyVal";
    private static final String ARG_START_DATE_SECONDS = "startDateSeconds"; // Đổi key cho rõ ràng hơn
    private static final String ARG_START_DATE_NANOS = "startDateNanos";
    private static final String ARG_END_DATE_SECONDS = "endDateSeconds";   // Đổi key cho rõ ràng hơn
    private static final String ARG_END_DATE_NANOS = "endDateNanos";
    // private static final String ARG_ACCOUNT_NUMBER = "accountNumber"; // Nếu bạn muốn truyền và dùng số tài khoản
    private static final String ARG_PARENT_ACCOUNT_ID_MORTGAGE = "parent_account_id_mortgage";


    // newInstance giờ nhận 3 tham số
    public static CustomerMortgageAccountFragment newInstance(MortgageAccount mortgageAcc, String ignoredAccountNumber, String parentAccId) {
        CustomerMortgageAccountFragment fragment = new CustomerMortgageAccountFragment();
        Bundle args = new Bundle();
        if (mortgageAcc != null) {
            args.putInt(ARG_LOAN_AMOUNT, mortgageAcc.getLoanAmount() != null ? mortgageAcc.getLoanAmount() : 0);
            args.putInt(ARG_REMAINING_AMOUNT, mortgageAcc.getRemainingAmount() != null ? mortgageAcc.getRemainingAmount() : 0);
            args.putInt(ARG_INTEREST_RATE, mortgageAcc.getInterestRate());
            args.putInt(ARG_PAYMENT_AMOUNT, mortgageAcc.getPaymentAmount() != null ? mortgageAcc.getPaymentAmount() : 0);
            args.putString(ARG_PAYMENT_FREQUENCY, AccountUtils.translateTypeOfPaymentFrequency(mortgageAcc.getPaymentFrequency()));
            if (mortgageAcc.getStartDate() != null) {
                args.putLong(ARG_START_DATE_SECONDS, mortgageAcc.getStartDate().getSeconds());
                args.putInt(ARG_START_DATE_NANOS, mortgageAcc.getStartDate().getNanoseconds());
            }
            if (mortgageAcc.getEndDate() != null) {
                args.putLong(ARG_END_DATE_SECONDS, mortgageAcc.getEndDate().getSeconds());
                args.putInt(ARG_END_DATE_NANOS, mortgageAcc.getEndDate().getNanoseconds());
            }
        }
        // args.putString(ARG_ACCOUNT_NUMBER, ignoredAccountNumber); // Nếu bạn muốn lưu số tài khoản
        args.putString(ARG_PARENT_ACCOUNT_ID_MORTGAGE, parentAccId); // Lưu parentAccountId
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mortgageAccountData = new MortgageAccount(); // Luôn khởi tạo để tránh NullPointerException
        Bundle args = getArguments();
        if (args != null) {
            Log.d(TAG, "Arguments received in onCreate.");
            // Chỉ lấy dữ liệu nếu bundle chứa key, để tránh ghi đè giá trị mặc định của mortgageAccountData nếu mortgageAcc truyền vào newInstance là null
            if (args.containsKey(ARG_LOAN_AMOUNT)) { // Kiểm tra một key đại diện
                mortgageAccountData.setLoanAmount(args.getInt(ARG_LOAN_AMOUNT, 0));
                mortgageAccountData.setRemainingAmount(args.getInt(ARG_REMAINING_AMOUNT, 0));
                mortgageAccountData.setInterestRate(args.getInt(ARG_INTEREST_RATE, 0));
                mortgageAccountData.setPaymentAmount(args.getInt(ARG_PAYMENT_AMOUNT, 0));
                mortgageAccountData.setPaymentFrequency(args.getString(ARG_PAYMENT_FREQUENCY, "N/A"));

                if (args.containsKey(ARG_START_DATE_SECONDS)) {
                    long seconds = args.getLong(ARG_START_DATE_SECONDS);
                    int nanos = args.getInt(ARG_START_DATE_NANOS);
                    mortgageAccountData.setStartDate(new Timestamp(seconds, nanos));
                }
                if (args.containsKey(ARG_END_DATE_SECONDS)) {
                    long seconds = args.getLong(ARG_END_DATE_SECONDS);
                    int nanos = args.getInt(ARG_END_DATE_NANOS);
                    mortgageAccountData.setEndDate(new Timestamp(seconds, nanos));
                }
            } else {
                Log.w(TAG, "Arguments bundle does not seem to contain primary mortgage data keys (e.g., loanAmount). Mortgage object might be from a null source.");
            }
            // this.accountNumber = args.getString(ARG_ACCOUNT_NUMBER); // Lấy accountNumber nếu bạn đã lưu
            this.parentAccountId = args.getString(ARG_PARENT_ACCOUNT_ID_MORTGAGE); // LẤY parentAccountId
            Log.d(TAG, "parentAccountId in MortgageFragment: " + this.parentAccountId);

        } else {
            Log.w(TAG, "Arguments bundle is null in onCreate.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_mortgage_account_officer, container, false);
        tvLoanAmountHeader = view.findViewById(R.id.tv_officer_mortgage_loan_amount_header);
        tvRemainingAmountValue = view.findViewById(R.id.tv_officer_mortgage_remaining_amount);
        tvInterestRateValue = view.findViewById(R.id.tv_officer_mortgage_interest_rate);
        tvStartDateValue = view.findViewById(R.id.tv_officer_mortgage_start_date);
        tvEndDateValue = view.findViewById(R.id.tv_officer_mortgage_end_date);
        tvPaymentAmountValue = view.findViewById(R.id.tv_officer_mortgage_payment_amount);
        tvPaymentFrequencyValue = view.findViewById(R.id.tv_officer_mortgage_payment_frequency);
        btnMortgageDetailsIndicator = view.findViewById(R.id.btn_officer_mortgage_details_indicator);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called. Displaying info.");
        displayAccountInfo();
        setupClickListeners();
    }

    private void displayAccountInfo() {
        if (mortgageAccountData != null) {
            Log.d(TAG, "Displaying Mortgage - Loan: " + mortgageAccountData.getLoanAmount() +
                    ", StartDate: " + mortgageAccountData.getStartDate() +
                    ", PaymentFreq: " + mortgageAccountData.getPaymentFrequency());

            tvLoanAmountHeader.setText(NumberFormat.convertToCurrencyFormatHasUnit(
                    mortgageAccountData.getLoanAmount() != null ? mortgageAccountData.getLoanAmount() : 0));
            tvRemainingAmountValue.setText(NumberFormat.convertToCurrencyFormatHasUnit(
                    mortgageAccountData.getRemainingAmount() != null ? mortgageAccountData.getRemainingAmount() : 0));
            tvInterestRateValue.setText(String.valueOf(mortgageAccountData.getInterestRate()) + " %");

            // Đảm bảo các TextView này được cập nhật
            tvStartDateValue.setText(TimeUtils.formatFirebaseTimestamp(mortgageAccountData.getStartDate()));
            tvEndDateValue.setText(TimeUtils.formatFirebaseTimestamp(mortgageAccountData.getEndDate()));
            tvPaymentAmountValue.setText(NumberFormat.convertToCurrencyFormatHasUnit(
                    mortgageAccountData.getPaymentAmount() != null ? mortgageAccountData.getPaymentAmount() : 0));
            tvPaymentFrequencyValue.setText(mortgageAccountData.getPaymentFrequency() != null ? mortgageAccountData.getPaymentFrequency() : "N/A");
        } else {
            Log.w(TAG, "mortgageAccountData is NULL in displayAccountInfo. Displaying defaults.");
            // Hiển thị giá trị mặc định/trống nếu data null
            tvLoanAmountHeader.setText(NumberFormat.convertToCurrencyFormatHasUnit(0));
            // ... (set các TextView khác thành "N/A" hoặc giá trị mặc định)
            tvStartDateValue.setText("N/A");
            tvEndDateValue.setText("N/A");
            tvPaymentAmountValue.setText("0 VNĐ");
            tvPaymentFrequencyValue.setText("N/A");
        }
    }
    private void setupClickListeners() {
        if (btnMortgageDetailsIndicator != null) {
            btnMortgageDetailsIndicator.setOnClickListener(v -> { /* Chỉ là chỉ báo */ });
        }
    }
}