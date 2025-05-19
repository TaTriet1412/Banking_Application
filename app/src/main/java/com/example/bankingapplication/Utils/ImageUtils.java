package com.example.bankingapplication.Utils;

import android.net.Uri;

import com.example.bankingapplication.Firebase.FirebaseStorageManager;
import com.example.bankingapplication.Firebase.Firestore;

public class ImageUtils {
    public interface ImageUploadCallback {
        void onSuccess(String fileName);  // Thông báo khi upload thành công
        void onFailure(String errorMessage);  // Thông báo khi upload thất bại
    }

    // upload Image to Storage
    public static void uploadImage(String fieldName, String collectionName, String document,Uri imgUri, String folderPath, ImageUploadCallback callback) {
        // Tạo tên file ngẫu nhiên cho ảnh
        String fileName = "img" + System.currentTimeMillis() + (int) (Math.random() * 100000);

        // Gọi Firebase Storage Manager để upload file
        FirebaseStorageManager.uploadFile(imgUri, fileName, folderPath, 10).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult()) {
                // Upload ảnh thành công
                // Cập nhật link ảnh vào Firestore
                updateImageURLToFirestore(fieldName,collectionName,document,fileName, callback);
            } else {
                // Nếu upload thất bại
                if (callback != null) {
                    callback.onFailure("Lưu ảnh thất bại");
                }
            }
        });
    }

    // Update field URL to Firestore

    private static void updateImageURLToFirestore(String fieldName, String collectionName, String document, String fileName, ImageUploadCallback callback) {
        Firestore.updateImgUrlForDocument(fieldName,collectionName,document, fileName, isSuccess -> {
            if (isSuccess) {
                // Cập nhật ảnh thành công
                if (callback != null) {
                    callback.onSuccess(fileName);
                }
            } else {
                // Nếu cập nhật Firestore thất bại
                if (callback != null) {
                    callback.onFailure("Cập nhật ảnh vào Firestore thất bại");
                }
            }
        });
    }
}
