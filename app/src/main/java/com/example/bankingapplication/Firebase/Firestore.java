package com.example.bankingapplication.Firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bankingapplication.Object.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public interface FirestoreAddCallback {
        void onCallback(boolean isSuccess);
    }


    public static void updateImgUrlForDocument(String fieldUrlName, String collectionName, String document, String imgUrl, FirestoreAddCallback callback) {
        Map<String, Object> data;

        if (fieldUrlName.contains(".")) {
            // Trường hợp field lồng nhau: bioData.faceUrl
            String[] keys = fieldUrlName.split("\\.");
            data = buildNestedMap(keys, imgUrl);
        } else {
            // Trường hợp field đơn
            data = new HashMap<>();
            data.put(fieldUrlName, imgUrl);
        }

        db.collection(collectionName).document(document).update(data)
                .addOnSuccessListener(aVoid -> callback.onCallback(true))
                .addOnFailureListener(e -> callback.onCallback(false));
    }

    private static Map<String, Object> buildNestedMap(String[] keys, Object value) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> current = result;

        for (int i = 0; i < keys.length - 1; i++) {
            Map<String, Object> next = new HashMap<>();
            current.put(keys[i], next);
            current = next;
        }

        current.put(keys[keys.length - 1], value);
        return result;
    }

    public interface FirestoreGetAllUserBaseCallBack {
        void onCallback(List<User> userList);
    }

    public static void getAllUserBase(FirestoreGetAllUserBaseCallBack callback) {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> userList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String email = document.getString("email");
                        String phone = document.getString("phone");
                        String nationalId = document.getString("nationalId");
                        userList.add(new User(email, phone, nationalId));
                    }
                    callback.onCallback(userList);
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi nếu cần
                    Log.e("Firestore", "Error getting documents: ", e);
                    callback.onCallback(null);
                });
    }



    public static void createUser(User user, FirestoreAddCallback callback) {
        db.collection("users")
                .document(user.getUID())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // Thêm thành công
                    callback.onCallback(true);
                })
                .addOnFailureListener(e -> {
                    // Thêm thất bại
                    callback.onCallback(false);
                });
    }
}