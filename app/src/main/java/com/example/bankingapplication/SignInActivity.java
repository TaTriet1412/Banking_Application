package com.example.bankingapplication;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.messaging.FirebaseMessaging;
import com.example.bankingapplication.Firebase.FirebaseAuth;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.Account;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.AudioEffectUtils;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.VariablesUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignInActivity extends AppCompatActivity {
    TextInputEditText edtPassword, edtEmail;
    private static final String TAG = "SignInActivity";
    Button btnSignIn;
    Button btnSignUp;

    TextView tvError;

    FrameLayout progressOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        edtEmail = findViewById(R.id.txtEmail);
        edtPassword = findViewById(R.id.txtPass);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvError = findViewById(R.id.lblError);
        progressOverlay = findViewById(R.id.progress_overlay);

        btnSignIn.setOnClickListener(v -> {
            AudioEffectUtils.clickEffect(this);
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            // Validate email and password
            if (email.isEmpty()) {
                edtEmail.setBackgroundResource(R.drawable.sign_in_frame_error_view);
                tvError.setVisibility(View.VISIBLE);
                edtEmail.requestFocus();
                Toast.makeText(this, "Email không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                edtPassword.setBackgroundResource(R.drawable.sign_in_frame_error_view);
                tvError.setVisibility(View.VISIBLE);
                edtPassword.requestFocus();
                Toast.makeText(this, "Mật khẩu không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }
            progressOverlay.setVisibility(View.VISIBLE);
            FirebaseAuth.signIn(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Boolean isSuccess = task.getResult();
                    if (isSuccess != null && isSuccess) {
                        Firestore.getUser(FirebaseAuth.getUID(), new Firestore.FirestoreGetUserCallback() {
                            @Override
                            public void onCallback(User user) {
                                if (user != null) {
                                    GlobalVariables globalVariables = GlobalVariables.getInstance();
                                    globalVariables.setCurrentUser(user);

                                    if(Objects.equals(user.getRole(), VariablesUtils.CUSTOMER_ROLE)) {
                                        // <<<< GỌI LẤY VÀ LƯU TOKEN Ở ĐÂY >>>>
                                        getTokenAndSaveToFirestore(user.getUID());
                                        // <<<< KẾT THÚC GỌI >>>>

                                        Firestore.getAccountByUserId(user.getUID(), new Firestore.FirestoreGetAccountCallback() {
                                            @Override
                                            public void onCallback(Account account) {
                                                if (account != null) {
                                                    globalVariables.setCurrentAccount(account);
                                                    progressOverlay.setVisibility(View.GONE);
                                                    Intent intent = new Intent(SignInActivity.this, CustomerMainActivity.class);
                                                    startActivity(intent);
                                                    finish(); // Kết thúc SignInActivity sau khi điều hướng
                                                }   else {
                                                    progressOverlay.setVisibility(View.GONE);
                                                    FirebaseAuth.signOut(); // Đăng xuất nếu không tìm thấy tài khoản liên kết
                                                    GlobalVariables.getInstance().setCurrentUser(null); // Xóa user khỏi global
                                                    Toast.makeText(SignInActivity.this, "Không tìm thấy tài khoản ngân hàng liên kết.", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                    } else if (Objects.equals(user.getRole(), VariablesUtils.BANK_OFFICER_ROLE)) {
                                        progressOverlay.setVisibility(View.GONE);
                                        Intent intent = new Intent(SignInActivity.this, BankOfficerMainActivity.class);
                                        startActivity(intent);
                                        finish(); // Kết thúc SignInActivity
                                    } else {
                                        progressOverlay.setVisibility(View.GONE);
                                        FirebaseAuth.signOut();
                                        GlobalVariables.getInstance().setCurrentUser(null);
                                        Toast.makeText(SignInActivity.this, "Vai trò người dùng không xác định.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    progressOverlay.setVisibility(View.GONE);
                                    FirebaseAuth.signOut();
                                    Toast.makeText(SignInActivity.this, "Không thể tải thông tin người dùng.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else { // isSuccess là null hoặc false
                        FirebaseAuth.signOut();
                        progressOverlay.setVisibility(View.GONE);
                        Toast.makeText(this, "Đăng nhập thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    }
                } else { // task không thành công (ví dụ: sai mật khẩu, email không tồn tại)
                    FirebaseAuth.signOut();
                    progressOverlay.setVisibility(View.GONE);
                    // Xử lý lỗi
                    if (task.getException() instanceof FirebaseAuthInvalidUserException || task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        edtEmail.setBackgroundResource(R.drawable.sign_in_frame_error_view);
                        edtPassword.setBackgroundResource(R.drawable.sign_in_frame_error_view);
                        if (tvError != null) tvError.setVisibility(View.VISIBLE); // Kiểm tra tvError null
                        edtEmail.requestFocus();
                    } else {
                        Toast.makeText(SignInActivity.this, "Đã có lỗi xảy ra: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        edtEmail.requestFocus();
                    }
                }
            });
        });

        btnSignUp.setOnClickListener(v -> {
            AudioEffectUtils.clickEffect(this);
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            // Don't set CREATED_BY_OFFICER flag here since this is the normal user flow
            startActivity(intent);
        });
    }

    private void getTokenAndSaveToFirestore(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot get/save FCM token, userId is null or empty.");
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Registration Token for user " + userId + ": " + token);
                    saveTokenToFirestore(userId, token);
                });
    }

    private void saveTokenToFirestore(String userId, String token) {
        if (userId == null || token == null) return;

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fcmToken", token); // Giả sử bạn có trường fcmToken trong User object (và Firestore)

        // Sử dụng lớp Firestore của bạn (đảm bảo có hàm updateUserFields hoặc tương tự)
        // Hoặc dùng FirebaseFirestore.getInstance() trực tiếp nếu updateUserFields chỉ cập nhật các trường cụ thể
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(userId)
                .update(userUpdates) // Hoặc .set(userUpdates, SetOptions.merge()) để tạo trường nếu chưa có
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token successfully written/updated for user: " + userId))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing/updating FCM token for user: " + userId, e));
    }
}
