// OfficerProfileFragment.java
package com.example.bankingapplication;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bankingapplication.Firebase.FirebaseAuth; // Lớp FirebaseAuth của bạn
import com.example.bankingapplication.Firebase.Firestore;   // Lớp Firestore của bạn
import com.example.bankingapplication.Object.User;
import com.google.android.material.imageview.ShapeableImageView;
// import com.bumptech.glide.Glide; // Nếu bạn dùng Glide cho avatar

public class OfficerProfileFragment extends Fragment {

    private ShapeableImageView imageOfficerAvatar;
    private TextView textOfficerName, textOfficerId, textOfficerEmail, textOfficerRole;
    private Button btnEditOfficerProfile, btnLogoutOfficer; // Thêm nút logout
    // private ProgressBar progressBarOfficerProfile;

    public OfficerProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_officer_profile, container, false);

        imageOfficerAvatar = view.findViewById(R.id.imageOfficerAvatar);
        textOfficerName = view.findViewById(R.id.textOfficerName);
        textOfficerId = view.findViewById(R.id.textOfficerId); // Mã nhân viên, có thể là UID hoặc một mã riêng
        textOfficerEmail = view.findViewById(R.id.textOfficerEmail);
        textOfficerRole = view.findViewById(R.id.textOfficerRole);
        btnEditOfficerProfile = view.findViewById(R.id.btn_edit_officer_profile);
        // progressBarOfficerProfile = view.findViewById(R.id.progressBar_officer_profile); // Nếu có

        // Giả sử bạn thêm một nút logout vào layout fragment_officer_profile.xml
        // btnLogoutOfficer = view.findViewById(R.id.btn_logout_officer);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadOfficerProfile();

        btnEditOfficerProfile.setOnClickListener(v -> {
            // TODO: Điều hướng đến màn hình sửa thông tin cá nhân của nhân viên
            // Ví dụ: Intent intent = new Intent(getActivity(), EditOfficerProfileActivity.class);
            // startActivity(intent);
            Toast.makeText(getContext(), "Chức năng sửa thông tin đang được phát triển.", Toast.LENGTH_SHORT).show();
        });

        // if (btnLogoutOfficer != null) {
        //     btnLogoutOfficer.setOnClickListener(v -> {
        //         FirebaseAuth.signOut();
        //         // Điều hướng về màn hình đăng nhập
        //         Intent intent = new Intent(getActivity(), SignInActivity.class);
        //         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa back stack
        //         startActivity(intent);
        //         if (getActivity() != null) {
        //             getActivity().finish();
        //         }
        //     });
        // }
    }

    private void loadOfficerProfile() {
        // if (progressBarOfficerProfile != null) progressBarOfficerProfile.setVisibility(View.VISIBLE);

        String officerUid = com.example.bankingapplication.Firebase.FirebaseAuth.getUID(); // Lấy UID của người dùng hiện tại

        if (officerUid != null && !officerUid.isEmpty()) {
            Firestore.getUser(officerUid, new Firestore.FirestoreGetUserCallback() {
                @Override
                public void onCallback(User user) {
                    // if (progressBarOfficerProfile != null) progressBarOfficerProfile.setVisibility(View.GONE);
                    if (user != null && getContext() != null) {
                        textOfficerName.setText(user.getName() != null ? user.getName() : "N/A");
                        textOfficerId.setText(user.getUID()); // Sử dụng UID làm mã nhân viên, hoặc một trường mã nhân viên riêng nếu có
                        textOfficerEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
                        textOfficerRole.setText(user.getRole() != null ? user.getRole() : "N/A");

                        // Load avatar nếu có
                        // if (user.getBiometricData() != null && user.getBiometricData().getFaceUrl() != null) {
                        //    Glide.with(getContext())
                        //         .load(user.getBiometricData().getFaceUrl())
                        //         .placeholder(R.drawable.ic_user) // Ảnh placeholder
                        //         .error(R.drawable.ic_user)       // Ảnh khi lỗi
                        //         .circleCrop()
                        //         .into(imageOfficerAvatar);
                        // } else {
                        //    imageOfficerAvatar.setImageResource(R.drawable.ic_user); // Ảnh mặc định
                        // }
                        Log.d("OfficerProfile", "Profile loaded for: " + user.getName());
                    } else if (getContext() != null) {
                        Toast.makeText(getContext(), "Không thể tải thông tin hồ sơ.", Toast.LENGTH_SHORT).show();
                        Log.e("OfficerProfile", "User data is null or context is null for UID: " + officerUid);
                    }
                }
            });
        } else {
            // if (progressBarOfficerProfile != null) progressBarOfficerProfile.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Lỗi: Không xác định được người dùng.", Toast.LENGTH_LONG).show();
            Log.e("OfficerProfile", "Officer UID is null or empty.");
            // Có thể điều hướng về màn hình đăng nhập ở đây nếu UID không hợp lệ
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load lại thông tin khi fragment resume nếu cần, ví dụ sau khi sửa thông tin
        // loadOfficerProfile();
    }
}