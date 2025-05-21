package com.example.bankingapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View; // Thêm nếu bạn dùng ProgressBar
import android.widget.ProgressBar; // Thêm nếu bạn dùng ProgressBar

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.bankingapplication.Firebase.Firestore; // <<<< IMPORT LỚP FIRESTORE
import com.example.bankingapplication.Object.Bill;      // <<<< IMPORT LỚP BILL
import com.example.bankingapplication.Object.User;
import com.google.android.material.button.MaterialButton;

import android.graphics.Color; // Thêm import này
import com.google.firebase.Timestamp; // Đảm bảo đã import

import java.text.SimpleDateFormat;
import java.util.Calendar; // Thêm import này
import java.util.Date;    // Thêm import này

import java.text.DecimalFormat;
import java.util.Locale;

public class ConfirmElectricityPaymentActivity extends AppCompatActivity {

    public static final String EXTRA_BILL_NUMBER = "extra_bill_number";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";
    public static final String EXTRA_BILL_TYPE = "extra_bill_type";
    private Toolbar toolbar;
    private TextView tvBillNumber, tvCustomerName, tvProvider, tvAmount, tvAmountInWords, tvBillStatusMessage, tvConfirmDueDate;
    private AutoCompleteTextView actvAuthMethod;
    private MaterialButton btnConfirmPayment;
    private ProgressBar pbLoadingConfirm; // ProgressBar cho màn hình này (nếu có)


    // private FirebaseFirestore db; // Không cần thiết nữa
    private String billNumberExtra, providerNameExtra,billTypeExtra;
    private static final String CONFIRM_TAG = "ConfirmElectricity";

    private final String[] AUTH_METHODS = new String[]{"Smart OTP", "Face ID", "SMS OTP"};

    // Các hàm tiện ích bạn đã thêm
    private String convertToCurrencyFormatHasUnit(int amount) {
        DecimalFormat formatter = new DecimalFormat("###,###,###.##");
        return formatter.format(amount) + " VND";
    }

    private String convertNumberToVietnameseWords(int number) {
        String[] chuSo = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
        String[] donViNhom = {"", "nghìn", "triệu", "tỷ"};

        if (number == 0) return "Không"; // Sửa lại một chút cho tự nhiên hơn

        long num = (long) Math.abs(number); // Lấy trị tuyệt đối để xử lý số âm nếu có
        String result = "";
        int i = 0;
        boolean firstGroup = true;

        if (num == 0 && number !=0) { // Xử lý trường hợp số thập phân nhỏ hơn 1
            return "Không";
        }


        while (num > 0) {
            int group = (int) (num % 1000);
            if (group != 0) {
                String groupText = convertThreeDigits(group, chuSo, !firstGroup && (num / 1000 > 0)); // Thêm cờ cho "không trăm"
                result = groupText + (donViNhom[i].isEmpty() ? "" : (" " + donViNhom[i])) + (result.isEmpty() ? "" : (" " + result));
            }
            num /= 1000;
            i++;
            firstGroup = false;
        }
        result = result.trim();
        if (result.isEmpty()) return "Không"; // Nếu kết quả rỗng (ví dụ số rất nhỏ)
        result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        return result; // Bỏ " đồng" ở đây, sẽ thêm ở nơi gọi
    }

    private String convertThreeDigits(int number, String[] chuSo, boolean needLeadingZeroHundredAndLe) {
        int hundred = number / 100;
        int ten = (number % 100) / 10;
        int unit = number % 10;
        String result = "";

        // Xử lý hàng trăm
        if (hundred != 0) {
            result += chuSo[hundred] + " trăm ";
        } else if (needLeadingZeroHundredAndLe && (ten != 0 || unit != 0)) {
            // Chỉ thêm "không trăm " nếu không phải nhóm đầu tiên (ví dụ: 1,000,123 -> một triệu KHÔNG TRĂM lẻ...)
            // và nhóm đó có giá trị (ten hoặc unit khác 0)
            result += "không trăm ";
        }

        // Xử lý hàng chục và đơn vị
        if (ten > 1) { // Hai mươi, Ba mươi,... Chín mươi
            result += chuSo[ten] + " mươi ";
            if (unit == 1) {
                result += "mốt"; // Hai mươi mốt
            } else if (unit == 4 && ten > 1) { // Bốn (hai mươi tư, ba mươi tư)
                result += "tư";
            } else if (unit == 5) {
                result += "lăm"; // Hai mươi lăm
            } else if (unit != 0) {
                result += chuSo[unit];
            }
        } else if (ten == 1) { // Mười, Mười một, Mười lăm
            result += "mười ";
            if (unit == 1) {
                result += "một";
            } else if (unit == 5) {
                result += "lăm"; // Khác với "năm" ở hàng đơn vị đứng một mình
            } else if (unit != 0) {
                result += chuSo[unit];
            }
        } else { // Hàng chục là 0
            if (unit != 0) {
                // Chỉ thêm "lẻ" nếu nó không phải là hàng trăm (ví dụ: 101 -> một trăm lẻ một)
                // và không phải là nhóm đầu tiên nếu nhóm đầu tiên < 10 (ví dụ: 005 -> năm, không phải không trăm lẻ năm)
                if (hundred !=0 || (needLeadingZeroHundredAndLe && result.contains("không trăm"))){ // đã có "trăm" hoặc "không trăm"
                    result += "lẻ ";
                }
                result += chuSo[unit];
            }
        }
        return result.trim();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_electricity_payment);

        toolbar = findViewById(R.id.toolbar_confirm_electricity);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tvBillNumber = findViewById(R.id.tv_confirm_bill_number);
        tvCustomerName = findViewById(R.id.tv_confirm_customer_name);
        tvProvider = findViewById(R.id.tv_confirm_provider);
        tvAmount = findViewById(R.id.tv_confirm_amount);
        tvAmountInWords = findViewById(R.id.tv_confirm_amount_in_words);
        actvAuthMethod = findViewById(R.id.actv_authentication_method);
        btnConfirmPayment = findViewById(R.id.btn_confirm_payment_final);
        // Giả sử bạn có ProgressBar trong layout activity_confirm_electricity_payment.xml
        // pbLoadingConfirm = findViewById(R.id.pb_loading_confirm);
        tvBillStatusMessage = findViewById(R.id.tv_confirm_bill_status_message);
        tvConfirmDueDate = findViewById(R.id.tv_confirm_due_date);
        Intent intent = getIntent();
        if (intent != null) {
            billNumberExtra = intent.getStringExtra(EXTRA_BILL_NUMBER);
            providerNameExtra = intent.getStringExtra(EXTRA_PROVIDER_NAME);
            billTypeExtra = intent.getStringExtra(EXTRA_BILL_TYPE);
            Log.d(CONFIRM_TAG, "Received Bill Number: " + billNumberExtra + ", Provider: " + providerNameExtra + ", Type: " + billTypeExtra);
        }

        if (billNumberExtra == null || providerNameExtra == null || billTypeExtra == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin hóa đơn đầu vào.", Toast.LENGTH_LONG).show();
            Log.e(CONFIRM_TAG, "Bill number or provider name from intent is null.");
            finish();
            return;
        }

        tvBillNumber.setText(billNumberExtra);
        tvProvider.setText(providerNameExtra);

        setupAuthMethodDropdown();
        loadBillDetailsUsingFirestoreClass(); // Gọi hàm mới

        btnConfirmPayment.setOnClickListener(v -> {
            String authMethod = actvAuthMethod.getText().toString();
            if (authMethod.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn phương thức xác thực", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Xác nhận thanh toán với: " + authMethod, Toast.LENGTH_LONG).show();
        });
    }

    private void showConfirmLoading(boolean isLoading) {
        // Implement an actual ProgressBar if you have one in this layout
        // For now, just log
        Log.d(CONFIRM_TAG, "showConfirmLoading: " + isLoading);
        if (pbLoadingConfirm != null) {
            pbLoadingConfirm.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnConfirmPayment.setEnabled(!isLoading);
        }
    }

    private void setupAuthMethodDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, AUTH_METHODS);
        actvAuthMethod.setAdapter(adapter);
        if (AUTH_METHODS.length > 0) {
            actvAuthMethod.setText(AUTH_METHODS[0], false);
        }
    }

    private void loadBillDetailsUsingFirestoreClass() {
        showConfirmLoading(true);
        Firestore.getElectricityBillByDetails(providerNameExtra, billNumberExtra, billTypeExtra, (bill, e) -> {
            showConfirmLoading(false);
            if (e != null) {
                Log.e(CONFIRM_TAG, "Error loading bill details from Firestore class: ", e);
                Toast.makeText(this, "Lỗi tải thông tin hóa đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                displayEmptyBillInfo();
                return;
            }

            if (bill != null) {
                Log.d(CONFIRM_TAG, "Bill details loaded: UID=" + bill.getUID() + ", Amount=" + bill.getAmount() + ", UserID=" + bill.getUserId() + ", DueDate=" + bill.getDueDate() + ", Type=" + bill.getType());
                // Lấy số tiền - amount trong Bill.java là Integer
                Integer amountInteger = bill.getAmount(); // Lấy đối tượng Integer

                if (amountInteger != null) { // Kiểm tra xem đối tượng Integer có null không
                    tvAmount.setText(convertToCurrencyFormatHasUnit(amountInteger));
                    tvAmountInWords.setText(convertNumberToVietnameseWords(amountInteger) + " đồng");
                    Log.d(CONFIRM_TAG, "Amount processed: " + amountInteger);
                } else {
                    tvAmount.setText("N/A (amount is null)");
                    tvAmountInWords.setText("");
                    Log.w(CONFIRM_TAG, "Amount (Integer) is null in Bill object.");
                }
                // <<<< HIỂN THỊ NGÀY HẾT HẠN >>>>
                Timestamp dueDateTimestamp = bill.getDueDate();
                if (dueDateTimestamp != null) {
                    Date dueDate = dueDateTimestamp.toDate();
                    // Bạn có thể tùy chỉnh định dạng ngày giờ ở đây
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    tvConfirmDueDate.setText(sdf.format(dueDate));
                } else {
                    tvConfirmDueDate.setText("N/A");
                    Log.w(CONFIRM_TAG, "DueDate is null in Bill object.");
                }
                // Lấy trạng thái hóa đơn
                checkAndDisplayBillStatus(bill);
                // Lấy tên khách hàng

                if (bill.getUserId() != null && !bill.getUserId().isEmpty()) {
                    loadCustomerNameUsingFirestoreClass(bill.getUserId());
                } else {
                    tvCustomerName.setText("Không xác định (no userId)");
                    Log.w(CONFIRM_TAG, "UserID is null or empty in the loaded bill.");
                }
            } else {
                Log.w(CONFIRM_TAG, "Bill not found by Firestore.getElectricityBillByDetails");
                Toast.makeText(this, "Không tìm thấy thông tin hóa đơn chi tiết.", Toast.LENGTH_SHORT).show();
                displayEmptyBillInfo();
            }
        });
    }
    private void checkAndDisplayBillStatus(Bill bill) {
        if (bill == null) return;

        Timestamp dueDateTimestamp = bill.getDueDate();
        String status = bill.getStatus();

        // Mặc định ẩn thông báo
        if (tvBillStatusMessage != null) { // Kiểm tra null cho TextView mới
            tvBillStatusMessage.setVisibility(View.GONE);
        }


        if (dueDateTimestamp != null && status != null) {
            Date dueDate = dueDateTimestamp.toDate();
            Date currentDate = Calendar.getInstance().getTime();

            // So sánh dueDate với currentDate (chỉ quan tâm đến ngày, không phải giờ phút giây cụ thể)
            Calendar calDueDate = Calendar.getInstance();
            calDueDate.setTime(dueDate);
            Calendar calCurrentDate = Calendar.getInstance();
            calCurrentDate.setTime(currentDate);

            // Đặt giờ, phút, giây, mili giây về 0 để so sánh ngày chính xác
            clearTime(calDueDate);
            clearTime(calCurrentDate);

            // Định nghĩa các trạng thái hoàn thành (có thể có nhiều)
            // Chuyển về chữ thường để so sánh không phân biệt hoa thường
            String lowerCaseStatus = status.toLowerCase();
            boolean isCompleted = lowerCaseStatus.equals("completed") ||
                    lowerCaseStatus.equals("paid") ||
                    lowerCaseStatus.equals("đã thanh toán"); // Thêm các trạng thái hoàn thành khác nếu có

            if (calDueDate.before(calCurrentDate) && !isCompleted) {
                // Hóa đơn đã quá hạn VÀ chưa hoàn thành
                String overdueMessage = "Hóa đơn này đã quá hạn thanh toán!";
                Log.w(CONFIRM_TAG, overdueMessage + " Due: " + dueDate.toString() + ", Status: " + status);
                if (tvBillStatusMessage != null) {
                    tvBillStatusMessage.setText(overdueMessage);
                    tvBillStatusMessage.setTextColor(Color.RED); // Hoặc màu cảnh báo khác
                    tvBillStatusMessage.setVisibility(View.VISIBLE);
                }
                // Bạn có thể muốn vô hiệu hóa nút "Xác nhận" hoặc thực hiện hành động khác
                btnConfirmPayment.setEnabled(false);
                actvAuthMethod.setEnabled(false); // Vô hiệu hóa chọn phương thức xác thực
            } else if (isCompleted) {
                String paidMessage = "Hóa đơn này đã được thanh toán.";
                Log.i(CONFIRM_TAG, paidMessage + " Status: " + status);
                if (tvBillStatusMessage != null) {
                    tvBillStatusMessage.setText(paidMessage);
                    tvBillStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                    // Màu xanh (thành công)
                    tvBillStatusMessage.setVisibility(View.VISIBLE);
                    // Có thể vô hiệu hóa nút xác nhận vì đã thanh toán
                    btnConfirmPayment.setEnabled(false);
                    actvAuthMethod.setEnabled(false); // Vô hiệu hóa chọn phương thức xác thực
                }
            } else {
                // Hóa đơn chưa đến hạn hoặc chưa có trạng thái đặc biệt
                String paidMessage = "Hóa đơn này chưa thanh toán.";
                Log.i(CONFIRM_TAG, "Bill is not overdue or status is normal. Due: " + dueDate.toString() + ", Status: " + status);
                if (tvBillStatusMessage != null) {
                    tvBillStatusMessage.setText(paidMessage);
                    tvBillStatusMessage.setVisibility(View.VISIBLE);
                    // tvBillStatusMessage.setVisibility(View.GONE); // Ẩn nếu không có thông báo đặc biệt
                }
            }
        } else {
            Log.w(CONFIRM_TAG, "Due date or status is null in bill object. Cannot check status.");
            if (tvBillStatusMessage != null) {
                tvBillStatusMessage.setVisibility(View.GONE);
            }
        }
    }

    // Hàm tiện ích để xóa thông tin thời gian (giờ, phút, giây, mili giây) khỏi Calendar
    private void clearTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
    private void loadCustomerNameUsingFirestoreClass(String customerId) {
        // showConfirmLoading(true); // Không cần nếu đang trong luồng loading của bill
        Firestore.getUser(customerId, user -> { // Sử dụng Firestore.getUser
            // showConfirmLoading(false);
            if (user != null) {
                if (user.getName() != null && !user.getName().isEmpty()) {
                    tvCustomerName.setText(user.getName());
                    Log.d(CONFIRM_TAG, "Customer name loaded: " + user.getName());
                } else {
                    tvCustomerName.setText("Không có tên");
                    Log.w(CONFIRM_TAG, "User name is null or empty for ID: " + customerId);
                }
            } else {
                tvCustomerName.setText("Không tìm thấy KH");
                Log.w(CONFIRM_TAG, "User not found by Firestore.getUser for ID: " + customerId);
            }
        });
    }

    private void displayEmptyBillInfo() {
        tvAmount.setText("N/A");
        tvAmountInWords.setText("");
        tvCustomerName.setText("N/A");
        tvConfirmDueDate.setText("N/A");
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