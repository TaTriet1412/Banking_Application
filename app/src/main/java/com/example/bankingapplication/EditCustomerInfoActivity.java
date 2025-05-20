package com.example.bankingapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.User; // Cần để parse Timestamp
import com.example.bankingapplication.Utils.TimeUtils; // Giả sử bạn có hàm này
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp; // Import Timestamp
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditCustomerInfoActivity extends AppCompatActivity {

    private static final String TAG = "EditCustomerInfo";
    public static final String EXTRA_CUSTOMER_ID = "customer_id_to_edit";
    public static final String EXTRA_CUSTOMER_NAME = "customer_name";
    public static final String EXTRA_CUSTOMER_EMAIL = "customer_email";
    public static final String EXTRA_CUSTOMER_PHONE = "customer_phone";
    public static final String EXTRA_CUSTOMER_ADDRESS = "customer_address";
    public static final String EXTRA_CUSTOMER_NATIONAL_ID = "customer_national_id";
    public static final String EXTRA_CUSTOMER_DOB_SECONDS = "customer_dob_seconds"; // Lưu timestamp
    public static final String EXTRA_CUSTOMER_GENDER = "customer_gender"; // Lưu Boolean

    private TextInputEditText etName, etEmail, etPhone, etAddress, etNationalId, etDob;
    private AutoCompleteTextView actGender;
    private TextInputLayout layoutDob; // Để gán click listener cho icon
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    private String customerId;
    private Calendar selectedDateCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_customer_info);

        etName = findViewById(R.id.et_edit_customer_name);
        etEmail = findViewById(R.id.et_edit_customer_email);
        etPhone = findViewById(R.id.et_edit_customer_phone);
        etAddress = findViewById(R.id.et_edit_customer_address);
        etNationalId = findViewById(R.id.et_edit_customer_national_id);
        etDob = findViewById(R.id.et_edit_customer_dob);
        layoutDob = findViewById(R.id.layout_edit_customer_dob);
        actGender = findViewById(R.id.act_edit_customer_gender);
        btnSave = findViewById(R.id.btn_save_customer_info);
        progressBar = findViewById(R.id.progressBar_edit_customer);
        selectedDateCalendar = Calendar.getInstance();

        setupGenderDropdown();
        loadInitialData();

        etDob.setOnClickListener(v -> showDatePickerDialog());
        if (layoutDob.getEndIconDrawable() != null) { // Nếu có icon calendar
            layoutDob.setEndIconOnClickListener(v -> showDatePickerDialog());
        }

        btnSave.setOnClickListener(v -> saveCustomerInfo());
    }

    private void setupGenderDropdown() {
        String[] genders = new String[]{"Nam", "Nữ", "Khác"}; // Hoặc lấy từ strings.xml
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        actGender.setAdapter(adapter);
    }

    private void loadInitialData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_CUSTOMER_ID)) {
            customerId = intent.getStringExtra(EXTRA_CUSTOMER_ID);
            etName.setText(intent.getStringExtra(EXTRA_CUSTOMER_NAME));
            etEmail.setText(intent.getStringExtra(EXTRA_CUSTOMER_EMAIL)); // Cân nhắc không cho sửa email
            etPhone.setText(intent.getStringExtra(EXTRA_CUSTOMER_PHONE));
            etAddress.setText(intent.getStringExtra(EXTRA_CUSTOMER_ADDRESS));
            etNationalId.setText(intent.getStringExtra(EXTRA_CUSTOMER_NATIONAL_ID)); // Chỉ hiển thị

            if (intent.hasExtra(EXTRA_CUSTOMER_DOB_SECONDS)) {
                long dobSeconds = intent.getLongExtra(EXTRA_CUSTOMER_DOB_SECONDS, -1);
                if (dobSeconds != -1) {
                    Date dobDate = new Date(dobSeconds * 1000);
                    selectedDateCalendar.setTime(dobDate);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etDob.setText(dateFormat.format(dobDate));
                }
            }

            if (intent.hasExtra(EXTRA_CUSTOMER_GENDER)) {
                Boolean genderBoolean = (Boolean) intent.getSerializableExtra(EXTRA_CUSTOMER_GENDER); // Lấy Boolean
                if (genderBoolean != null) {
                    actGender.setText(genderBoolean ? "Nam" : "Nữ", false); // "false" để không filter dropdown
                } else {
                    actGender.setText("Khác", false); // Hoặc giá trị mặc định
                }
            }

        } else {
            Toast.makeText(this, "Lỗi: Không có thông tin khách hàng để sửa.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateCalendar.set(Calendar.YEAR, year);
                    selectedDateCalendar.set(Calendar.MONTH, month);
                    selectedDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etDob.setText(dateFormat.format(selectedDateCalendar.getTime()));
                },
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis()); // Không cho chọn ngày tương lai
        datePickerDialog.show();
    }


    private void saveCustomerInfo() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String dobString = etDob.getText().toString().trim();
        String genderString = actGender.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ tên, email và số điện thoại.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        // updates.put("email", email); // Cẩn thận khi cập nhật email nếu nó là duy nhất hoặc dùng để đăng nhập
        updates.put("phone", phone);
        updates.put("address", address);
        // Không cập nhật nationalId vì thường là cố định

        if (!TextUtils.isEmpty(dobString)) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date parsedDate = dateFormat.parse(dobString);
                if (parsedDate != null) {
                    updates.put("dateOfBirth", new Timestamp(parsedDate)); // Chuyển thành Firebase Timestamp
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing dateOfBirth: " + dobString, e);
                // Có thể thông báo lỗi cho người dùng
            }
        }

        Boolean genderValue = null;
        if ("Nam".equals(genderString)) {
            genderValue = true;
        } else if ("Nữ".equals(genderString)) {
            genderValue = false;
        }
        // Nếu "Khác" hoặc trống, genderValue sẽ là null (Firestore sẽ lưu null hoặc không có trường này)
        updates.put("gender", genderValue);


        Firestore.updateUserFields(customerId, updates, (isSuccess, e) -> {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            if (isSuccess) {
                Toast.makeText(EditCustomerInfoActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                String errorMessage = "Lỗi cập nhật thông tin.";
                if (e != null && e.getMessage() != null) errorMessage += ": " + e.getMessage();
                Log.e(TAG, "Error updating user: " + customerId, e);
                Toast.makeText(EditCustomerInfoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}