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

import com.example.bankingapplication.Firebase.FirebaseAuth;
import com.example.bankingapplication.Firebase.Firestore;
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.AudioEffectUtils;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.SecurePreferencesUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class SignInActivity extends AppCompatActivity {
    TextInputEditText edtPassword, edtEmail;

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

//                                    try {
//                                        SecurePreferencesUtil.saveEmail(SignInActivity.this, email); // Lưu email vào SharedPreferences
//                                    } catch (Exception e) {}
                                    progressOverlay.setVisibility(View.GONE); // Ẩn loading overlay
                                    Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                    startActivity(intent);

                                } else {
                                    // User data retrieval failed
                                }
                            }
                        });
                    }
                    // Navigate to the next activity
                } else {
                    // Sign in failed
                    FirebaseAuth.signOut();
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Không thể lấy thông tin tài khoản", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                progressOverlay.setVisibility(View.GONE); // Ẩn loading overlay
                // Xử lý lỗi
                if (e instanceof FirebaseAuthInvalidUserException || e instanceof FirebaseAuthInvalidCredentialsException) {
                    edtEmail.setBackgroundResource(R.drawable.sign_in_frame_error_view);
                    edtPassword.setBackgroundResource(R.drawable.sign_in_frame_error_view);
                    tvError.setVisibility(View.VISIBLE);
                    edtEmail.requestFocus();
                    return;
                }
                else {
                    Toast.makeText(SignInActivity.this, "Đã có lỗi xảy ra, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    edtEmail.requestFocus();
                    return;
                }
            });
        });

        btnSignUp.setOnClickListener(v -> {
            AudioEffectUtils.clickEffect(this);
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
}
