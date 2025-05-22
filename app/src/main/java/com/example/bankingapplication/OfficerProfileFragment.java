// OfficerProfileFragment.java
package com.example.bankingapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
// import android.widget.ProgressBar; // Nếu bạn thêm ProgressBar
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher; // Thêm
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.bankingapplication.Firebase.FirebaseAuth; // Lớp FirebaseAuth của bạn
import com.example.bankingapplication.Firebase.Firestore;   // Lớp Firestore của bạn
import com.example.bankingapplication.Object.User;
import com.example.bankingapplication.Utils.GlobalVariables;
import com.example.bankingapplication.Utils.TimeUtils;
import com.google.android.material.imageview.ShapeableImageView;
// import com.bumptech.glide.Glide; // Nếu bạn dùng Glide cho avatar

public class OfficerProfileFragment extends Fragment {

    private ShapeableImageView imageOfficerAvatar;
    private TextView textOfficerName, textOfficerId, textOfficerEmail, textOfficerRole;

    private TextView textOfficerIdentifier, textOfficerPhone, textOfficerDob, textOfficerGender, textOfficerAddress;
    private Button btnEditOfficerProfile, btnLogoutOfficer; // Thêm nút logout

    private ActivityResultLauncher<Intent> editOfficerProfileLauncher;
    // private ProgressBar progressBarOfficerProfile;

    public OfficerProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo ActivityResultLauncher
        editOfficerProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Thông tin đã được cập nhật, load lại dữ liệu
                        Log.d("OfficerProfile", "EditOfficerProfileActivity returned OK, refreshing profile...");
                        if (getContext() != null) Toast.makeText(getContext(), "Đang làm mới hồ sơ...", Toast.LENGTH_SHORT).show();
                        loadOfficerProfile();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_officer_profile, container, false);
        // ... ánh xạ các view cũ ...
        imageOfficerAvatar = view.findViewById(R.id.imageOfficerAvatar);
        textOfficerName = view.findViewById(R.id.textOfficerName);

        textOfficerEmail = view.findViewById(R.id.textOfficerEmail);
        textOfficerRole = view.findViewById(R.id.textOfficerRole);
        btnEditOfficerProfile = view.findViewById(R.id.btn_edit_officer_profile);
        btnLogoutOfficer = view.findViewById(R.id.btn_logout_officer);

        // Ánh xạ các TextView mới
        textOfficerPhone = view.findViewById(R.id.textOfficerPhone);
        textOfficerDob = view.findViewById(R.id.textOfficerDob);
        textOfficerGender = view.findViewById(R.id.textOfficerGender);
        textOfficerAddress = view.findViewById(R.id.textOfficerAddress);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // loadOfficerProfile(); // Bỏ load ở đây, chuyển sang onResume

        btnEditOfficerProfile.setOnClickListener(v -> {
            User currentOfficer = GlobalVariables.getInstance().getCurrentUser();
            if (currentOfficer != null && getActivity() != null) {
                Intent intent = new Intent(getActivity(), EditOfficerProfileActivity.class);
                // Truyền dữ liệu của nhân viên hiện tại sang màn hình sửa
                intent.putExtra("USER_ID", currentOfficer.getUID());
                intent.putExtra("USER_NAME", currentOfficer.getName());
                intent.putExtra("USER_EMAIL", currentOfficer.getEmail()); // Email thường không cho sửa
                intent.putExtra("USER_PHONE", currentOfficer.getPhone());
                intent.putExtra("USER_ADDRESS", currentOfficer.getAddress());
                intent.putExtra("USER_NATIONAL_ID", currentOfficer.getNationalId()); // CCCD cũng thường không cho sửa
                if (currentOfficer.getDateOfBirth() != null) {
                    intent.putExtra("USER_DOB_SECONDS", currentOfficer.getDateOfBirth().getSeconds());
                }
                if (currentOfficer.getGender() != null) {
                    intent.putExtra("USER_GENDER", currentOfficer.getGender());
                }
                // Không truyền officerId nếu bạn dùng UID làm mã chính hoặc email
                // intent.putExtra("USER_OFFICER_ID", currentOfficer.getOfficerId());
                editOfficerProfileLauncher.launch(intent); // Sử dụng launcher
            } else {
                Toast.makeText(getContext(), "Không thể tải thông tin để sửa.", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnLogoutOfficer != null) {
            btnLogoutOfficer.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Xác nhận đăng xuất");
        builder.setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản?");
        builder.setPositiveButton("Đăng xuất", (dialog, which) -> {
            performLogout();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void performLogout() {
        // Sign out from Firebase Auth
        FirebaseAuth.signOut();
        
        // Clear global variables if needed
        GlobalVariables.getInstance().setCurrentUser(null);
        GlobalVariables.getInstance().setCurrentAccount(null);

        // Navigate to SignInActivity and clear all activities in stack
        Intent intent = new Intent(getActivity(), SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void loadOfficerProfile() {
        String officerUid = com.example.bankingapplication.Firebase.FirebaseAuth.getUID();

        if (officerUid != null && !officerUid.isEmpty()) {
            Firestore.getUser(officerUid, user -> {
                if (user != null && getContext() != null) {
                    GlobalVariables.getInstance().setCurrentUser(user);

                    Log.d("OfficerProfile", "User loaded. Name: " + user.getName());
                    Log.d("OfficerProfile", "User loaded. Email: " + user.getEmail());
                    Log.d("OfficerProfile", "User loaded. Phone: " + user.getPhone());
                    Log.d("OfficerProfile", "User loaded. Role: " + user.getRole());
                    Log.d("OfficerProfile", "User loaded. Address: " + user.getAddress());
                    Log.d("OfficerProfile", "User loaded. Gender: " + user.getGender());
                    Log.d("OfficerProfile", "User loaded. DoB: " + (user.getDateOfBirth() != null ? user.getDateOfBirth().toDate() : "null"));

                    textOfficerName.setText(user.getName() != null ? user.getName() : "N/A");

                    // KHÔNG cần set text cho textOfficerIdentifier nữa nếu bạn đã ẩn nó trong XML
                    // Hoặc nếu bạn giữ lại TextView nhưng không muốn hiển thị UID:
                    // if (textOfficerIdentifier != null) {
                    //     textOfficerIdentifier.setText(""); // Để trống
                    //     // hoặc textOfficerIdentifier.setText(user.getEmail()); // Hiển thị email thay thế chẳng hạn
                    // }


                    textOfficerEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
                    textOfficerRole.setText(user.getRole() != null ? user.getRole() : "N/A");
                    textOfficerPhone.setText(user.getPhone() != null ? user.getPhone() : "Chưa cập nhật");
                    if (user.getDateOfBirth() != null) {
                        textOfficerDob.setText(TimeUtils.formatFirebaseTimestamp(user.getDateOfBirth()).substring(0,10));
                    } else {
                        textOfficerDob.setText("Chưa cập nhật");
                    }
                    textOfficerGender.setText(user.genderToString());
                    textOfficerAddress.setText(user.getAddress() != null && !user.getAddress().isEmpty() ? user.getAddress() : "Chưa cập nhật");

                    Log.d("OfficerProfile", "Profile re-loaded for: " + user.getName());
                } else if (getContext() != null) {
                    Toast.makeText(getContext(), "Không thể tải thông tin hồ sơ.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "Lỗi: Không xác định được người dùng.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load lại thông tin khi fragment resume nếu cần, ví dụ sau khi sửa thông tin
         loadOfficerProfile();
    }
}