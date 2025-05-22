package com.example.bankingapplication;

import android.app.DatePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Utils.GlobalVariables;
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

public class EditUserProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditUserProfile";
    
    private TextInputEditText etName, etEmail, etPhone, etAddress, etDob, etNationalId;
    private AutoCompleteTextView actGender;
    private TextInputLayout layoutDob;
    private AppCompatButton btnSave;
    private FrameLayout progressOverlay;
    private Toolbar toolbar;
    private String userId;
    private Calendar selectedDateCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply system window insets properly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | 
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        
        setContentView(R.layout.activity_edit_user_profile);

        // Set up the toolbar with a back button
        toolbar = findViewById(R.id.toolbar_edit_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Cập nhật thông tin cá nhân");
        }

        // Initialize views
        etName = findViewById(R.id.et_edit_customer_name);
        etEmail = findViewById(R.id.et_edit_customer_email);
        etPhone = findViewById(R.id.et_edit_customer_phone);
        etAddress = findViewById(R.id.et_edit_customer_address);
        etNationalId = findViewById(R.id.et_edit_customer_national_id);
        etDob = findViewById(R.id.et_edit_customer_dob);
        layoutDob = findViewById(R.id.layout_edit_customer_dob);
        actGender = findViewById(R.id.act_edit_customer_gender);
        btnSave = findViewById(R.id.btn_save_customer_info);
        progressOverlay = findViewById(R.id.progress_overlay);
        selectedDateCalendar = Calendar.getInstance();
        
        // Make national ID not editable as it's a fixed identifier
        if (etNationalId != null) {
            etNationalId.setEnabled(false);
        }

        // Set up gender dropdown
        setupGenderDropdown();
        
        // Get user data from intent extras
        loadUserData();

        // Set up click listeners
        etDob.setOnClickListener(v -> showDatePickerDialog());
        if (layoutDob.getEndIconDrawable() != null) {
            layoutDob.setEndIconOnClickListener(v -> showDatePickerDialog());
        }

        btnSave.setOnClickListener(v -> saveUserInfo());
    }

    private void setupGenderDropdown() {
        String[] genders = new String[]{"Nam", "Nữ", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        actGender.setAdapter(adapter);
    }

    private void loadUserData() {
        // Get data from intent
        userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            // If userId is not provided in intent, get from GlobalVariables
            if (GlobalVariables.getInstance().getCurrentUser() != null) {
                userId = GlobalVariables.getInstance().getCurrentUser().getUID();
            } else {
                Toast.makeText(this, "Không thể xác định người dùng", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        // Set fields from intent extras if available
        etName.setText(getIntent().getStringExtra("USER_NAME"));
        etEmail.setText(getIntent().getStringExtra("USER_EMAIL"));
        etEmail.setEnabled(false); // Don't allow changing email as it's used for authentication
        etPhone.setText(getIntent().getStringExtra("USER_PHONE"));
        etAddress.setText(getIntent().getStringExtra("USER_ADDRESS"));
        
        // Set national ID if available from intent, otherwise get from GlobalVariables
        String nationalId = getIntent().getStringExtra("USER_NATIONAL_ID");
        if (nationalId == null && GlobalVariables.getInstance().getCurrentUser() != null) {
            nationalId = GlobalVariables.getInstance().getCurrentUser().getNationalId();
        }
        if (etNationalId != null) {
            etNationalId.setText(nationalId);
        }

        // Set date of birth if available
        if (getIntent().hasExtra("USER_DOB_SECONDS")) {
            long dobSeconds = getIntent().getLongExtra("USER_DOB_SECONDS", -1);
            if (dobSeconds != -1) {
                Date dobDate = new Date(dobSeconds * 1000);
                selectedDateCalendar.setTime(dobDate);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etDob.setText(dateFormat.format(dobDate));
            }
        }

        // Set gender if available
        if (getIntent().hasExtra("USER_GENDER")) {
            Boolean genderBoolean = (Boolean) getIntent().getSerializableExtra("USER_GENDER");
            if (genderBoolean != null) {
                actGender.setText(genderBoolean ? "Nam" : "Nữ", false);
            } else {
                actGender.setText("Khác", false);
            }
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

    private void saveUserInfo() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String dobString = etDob.getText().toString().trim();
        String genderString = actGender.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ tên và số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() != 10 || !phone.matches("\\d+")) {
            Toast.makeText(this, "Số điện thoại phải có 10 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show the progress overlay
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
                Log.e(TAG, "Error parsing date: " + dobString, e);
            }
        }

        Boolean genderValue;
        if ("Nam".equals(genderString)) {
            genderValue = true;
        } else if ("Nữ".equals(genderString)) {
            genderValue = false;
        } else {
            genderValue = null;
        }
        updates.put("gender", genderValue);

        Firestore.updateUserFields(userId, updates, (isSuccess, e) -> {
            // Hide the progress overlay when the operation completes
            if (progressOverlay != null) {
                progressOverlay.setVisibility(View.GONE);
            }
            btnSave.setEnabled(true);
            
            if (isSuccess) {
                Toast.makeText(this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                
                // Update current user in GlobalVariables with all fields
                if (GlobalVariables.getInstance().getCurrentUser() != null) {
                    GlobalVariables.getInstance().getCurrentUser().setName(name);
                    GlobalVariables.getInstance().getCurrentUser().setPhone(phone);
                    GlobalVariables.getInstance().getCurrentUser().setAddress(address);
                    
                    // Update gender in the global user object
                    GlobalVariables.getInstance().getCurrentUser().setGender(genderValue);
                    
                    // Update date of birth in the global user object if it was set
                    if (!TextUtils.isEmpty(dobString)) {
                        try {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            Date parsedDate = dateFormat.parse(dobString);
                            if (parsedDate != null) {
                                GlobalVariables.getInstance().getCurrentUser().setDateOfBirth(new Timestamp(parsedDate));
                            }
                        } catch (ParseException e1) {
                            Log.e(TAG, "Error parsing date for GlobalVariables update: " + dobString, e1);
                        }
                    }
                }
                
                setResult(RESULT_OK);
                finish();
            } else {
                String errorMessage = "Lỗi khi cập nhật thông tin";
                if (e != null && e.getMessage() != null) {
                    errorMessage += ": " + e.getMessage();
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
