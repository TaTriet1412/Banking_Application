package com.example.bankingapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Object.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditOfficerProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditOfficerProfile";

    private TextInputEditText etName, etEmail, etPhone, etAddress, etNationalId, etDob;
    private AutoCompleteTextView actGender;
    private TextInputLayout layoutDob;
    private AppCompatButton btnSave;
    private FrameLayout progressOverlay;
    private Toolbar toolbar;
    private String officerUid;
    private Calendar selectedDateCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_officer_profile);

        toolbar = findViewById(R.id.toolbar_edit_officer_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        etName = findViewById(R.id.et_edit_officer_name);
        etEmail = findViewById(R.id.et_edit_officer_email);
        etPhone = findViewById(R.id.et_edit_officer_phone);
        etAddress = findViewById(R.id.et_edit_officer_address);
        etNationalId = findViewById(R.id.et_edit_officer_national_id);
        etDob = findViewById(R.id.et_edit_officer_dob);
        layoutDob = findViewById(R.id.layout_edit_officer_dob);
        actGender = findViewById(R.id.act_edit_officer_gender);
        btnSave = findViewById(R.id.btn_save_officer_info);
        progressOverlay = findViewById(R.id.progress_overlay);
        selectedDateCalendar = Calendar.getInstance();

        setupGenderDropdown();
        loadInitialData();

        etDob.setOnClickListener(v -> showDatePickerDialog());
        if (layoutDob.getEndIconDrawable() != null) {
            layoutDob.setEndIconOnClickListener(v -> showDatePickerDialog());
        }

        btnSave.setOnClickListener(v -> saveOfficerInfo());
    }

    private void setupGenderDropdown() {
        String[] genders = new String[]{"Nam", "Nữ", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        actGender.setAdapter(adapter);
    }

    private void loadInitialData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("USER_ID")) {
            officerUid = intent.getStringExtra("USER_ID");
            etName.setText(intent.getStringExtra("USER_NAME"));
            etEmail.setText(intent.getStringExtra("USER_EMAIL"));
            etPhone.setText(intent.getStringExtra("USER_PHONE"));
            etAddress.setText(intent.getStringExtra("USER_ADDRESS"));
            etNationalId.setText(intent.getStringExtra("USER_NATIONAL_ID"));

            etEmail.setEnabled(false);
            etNationalId.setEnabled(false);

            if (intent.hasExtra("USER_DOB_SECONDS")) {
                long dobSeconds = intent.getLongExtra("USER_DOB_SECONDS", -1);
                if (dobSeconds != -1) {
                    Date dobDate = new Date(dobSeconds * 1000);
                    selectedDateCalendar.setTime(dobDate);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etDob.setText(dateFormat.format(dobDate));
                }
            }

            if (intent.hasExtra("USER_GENDER")) {
                Boolean genderBoolean = (Boolean) intent.getSerializableExtra("USER_GENDER");
                if (genderBoolean != null) {
                    actGender.setText(genderBoolean ? "Nam" : "Nữ", false);
                } else {
                    actGender.setText("Khác", false);
                }
            }
        } else {
            Toast.makeText(this, "Lỗi: Không có thông tin nhân viên để sửa.", Toast.LENGTH_LONG).show();
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
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveOfficerInfo() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String dobString = etDob.getText().toString().trim();
        String genderString = actGender.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ tên và số điện thoại.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.length() != 10 || !phone.matches("\\d+")) {
            Toast.makeText(this, "Số điện thoại phải có 10 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        }
        btnSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);

        if (!TextUtils.isEmpty(dobString)) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date parsedDate = dateFormat.parse(dobString);
                if (parsedDate != null) {
                    updates.put("dateOfBirth", new Timestamp(parsedDate));
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing dateOfBirth: " + dobString, e);
            }
        }

        Boolean genderValue = null; // Khai báo ở đây
        if ("Nam".equals(genderString)) {
            genderValue = true;
        } else if ("Nữ".equals(genderString)) {
            genderValue = false;
        }
        updates.put("gender", genderValue);

        // Tạo một bản sao effectively final của genderValue để dùng trong lambda
        final Boolean finalGenderValueForLambda = genderValue;

        Firestore.updateUserFields(officerUid, updates, (isSuccess, e) -> {
            if (progressOverlay != null) {
                progressOverlay.setVisibility(View.GONE);
            }
            btnSave.setEnabled(true);

            if (isSuccess) {
                Toast.makeText(EditOfficerProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                User globalUser = GlobalVariables.getInstance().getCurrentUser();
                if (globalUser != null && globalUser.getUID().equals(officerUid)) {
                    globalUser.setName(name);
                    globalUser.setPhone(phone);
                    globalUser.setAddress(address);
                    globalUser.setGender(finalGenderValueForLambda); // Sử dụng bản sao effectively final
                    if (updates.containsKey("dateOfBirth")) {
                        globalUser.setDateOfBirth((Timestamp) updates.get("dateOfBirth"));
                    }
                }
                setResult(RESULT_OK);
                finish();
            } else {
                String errorMessage = "Lỗi cập nhật hồ sơ.";
                if (e != null && e.getMessage() != null) errorMessage += ": " + e.getMessage();
                Log.e(TAG, "Error updating officer profile: " + officerUid, e);
                Toast.makeText(EditOfficerProfileActivity.this, errorMessage, Toast.LENGTH_LONG).show();
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