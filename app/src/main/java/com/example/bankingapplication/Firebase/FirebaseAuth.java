package com.example.bankingapplication.Firebase;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class FirebaseAuth {
    private static com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance(); // là biến để thực hiện các phương thức của firebase
    private static FirebaseUser currentUser; // Là biến để lưu thông tin người dùng hiện tại
    FirebaseApp firebaseApp = FirebaseApp.getInstance(); // Lấy ra FirebaseApp đã được khởi tạo

    // lấy UID của người dùng hiện tại
    public static String getUID(){
        return currentUser.getUid();
    }

    // lấy email của người dùng hiện tại
    public static String getEmail(){
        return currentUser.getEmail();
    }

    // lấy tên hiển thị của người dùng hiện tại
    public static String getDisplayName(){
        return currentUser.getDisplayName();
    }

    // lấy ảnh đại diện của người dùng hiện tại
    public static String getPhotoUrl(){
        if (currentUser.getPhotoUrl() != null) {
            return currentUser.getPhotoUrl().toString();
        }
        return null;
    }

    // lấy số điện thoại của người dùng hiện tại
    public static String getPhoneNumber(){
        return currentUser.getPhoneNumber();
    }

    // lấy ID nhà cung cấp của người dùng hiện tại
    public static String getProviderId(){
        return currentUser.getProviderId();
    }

    // email đã được xác thực hay chưa
    public static boolean isEmailVerified(){
        return currentUser.isEmailVerified();
    }

    // phương thức đăng ký tài khoản mới với firebase sử dụng email và password
    public static Task<Boolean> signUp(String username, String password){
        // Ném ra exception FirebaseAuthWeakPasswordException nếu password quá yếu (dưới 6 ký tự)
        // Ném ra exception FirebaseAuthInvalidCredentialsException nếu email sai định dạng
        // Ném ra exception FirebaseAuthUserCollisionException nếu email đã tồn tại

        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        mAuth.createUserWithEmailAndPassword(username, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        // đăng ký thành công
                        Log.d("signUp", "signUp: success");
                        currentUser = mAuth.getCurrentUser(); // lấy thông tin người dùng hiện tại
                        currentUser.reload(); // cập nhật thông tin người dùng từ máy chủ firebase
                        taskCompletionSource.setResult(true);
                    }
                }).addOnFailureListener(e -> {
                    // lỗi gì đó
                    Log.e("signUp", "signUp: lỗi gì đó");
                    taskCompletionSource.setException(e); // ném lỗi ra để bắt ở nơi gọi
                });
        return taskCompletionSource.getTask();
    }

    // phương thức đăng nhập tài khoản đã đăng ký với firebase sử dụng email và password
    public static Task<Boolean> signIn(String username, String password){
        // Ném ra exception FirebaseAuthInvalidUserException nếu email không tồn tại
        // Ném ra exception FirebaseAuthInvalidCredentialsException nếu password không đúng

        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        // đăng nhập thành công
                        Log.d("signIn", "signIn: success");
                        currentUser = mAuth.getCurrentUser(); // lấy thông tin người dùng hiện tại
                        currentUser.reload(); // cập nhật thông tin người dùng từ máy chủ firebase
                        taskCompletionSource.setResult(true);
                    }
                }).addOnFailureListener(e -> {
                    // lỗi gì đó
                    Log.e("signIn", "signIn: " + e.getMessage());
                    taskCompletionSource.setException(e); // ném lỗi ra để bắt ở nơi gọi
                });
        return taskCompletionSource.getTask();
    }

    // Gửi email đặt lại mật khẩu đã quên
    public static Task<Boolean> sendResetPasswordEmail(String email) {
        // Ném ra exception FirebaseAuthInvalidUserException nếu email không tồn tại
        // Ném ra exception FirebaseAuthInvalidCredentialsException nếu email sai định dạng

        // Cần tắt tính năng Email enumeration protection của Firebase để hàm này đảm bảo hoạt động bình tường
        // và IntrusmentTest chạy được

        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // gửi email thành công
                        Log.d("sendResetPasswordEmail", "success");
                        taskCompletionSource.setResult(true);
                    }
                })
                .addOnFailureListener(e -> {
                    // lỗi gì đó
                    Log.e("sendResetPasswordEmailError", "sendResetPasswordEmail: " + e.getMessage());
                    taskCompletionSource.setException(e); // ném lỗi ra để bắt ở nơi gọi
                });
        return taskCompletionSource.getTask();
    }

    public static Task<Boolean> setDisplayName(String name){
        // Xử lý ngoại lệ khi người dùng chưa đăng nhập, xem logcat và error message

        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        if (currentUser != null){
            currentUser.updateProfile(
                    new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
            ).addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    // Đổi tên thành công
                    Log.d("sendSetDisplayNameResult", "success");
                    taskCompletionSource.setResult(true);
                }
            }).addOnFailureListener(e -> {
                // Có lỗi ? Ném cho chắc
                Log.e("sendSetDisplayNameError", "sendSetDisplayNameResult: " + e.getMessage());
                taskCompletionSource.setException(e);
            });
        } else {
            // Người dùng chưa đăng nhập
            String errorMessage = "User hasn't signed in yet";
            Log.e("sendSetDisplayNameError", errorMessage);
            taskCompletionSource.setException(new Exception(errorMessage));
        }
        return taskCompletionSource.getTask();
    }

    // Đăng xuất tài khoản
    public static void signOut(){
        mAuth.signOut();
    }
}
