package com.example.bankingapplication.Firebase;

import com.example.bankingapplication.Object.User;
import com.google.firebase.firestore.FirebaseFirestore;

public class Firestore {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Interface để truyền kết quả trả về từ Firestore (true: thành công, false: thất bại)
    public interface FirestoreGetUserCallback {
        void onCallback(User user);
    }

    public static void getUser(String UID, FirestoreGetUserCallback callback) {
        db.collection("users")
                .document(UID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        assert user != null;
                        user.setUID(UID);
                        callback.onCallback(user);
                    } else {
                        // Nếu có lỗi xảy ra trong quá trình truy vấn, trả về false
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    // Nếu có lỗi xảy ra trong quá trình truy vấn, trả về false
                    callback.onCallback(null);
                });
    }
}