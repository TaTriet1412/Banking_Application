package com.example.bankingapplication.Utils;
import android.graphics.Bitmap;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.common.InputImage;
public class FaceDetectionUtils {
    /**
     * Kiểm tra ảnh có phải là mặt người hay không
     * @param bitmap: Ảnh cần kiểm tra
     * @param listener: Callback để nhận kết quả (true/false)
     */
    public static void isFacePresent(Bitmap bitmap, OnFaceDetectionListener listener) {
        // Chuyển Bitmap thành InputImage cho ML Kit
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // Cấu hình FaceDetectorOptions
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)  // Chế độ nhanh
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)  // Nhận diện tất cả các điểm đặc trưng
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Phân loại tất cả các khuôn mặt
                .build();

        // Khởi tạo FaceDetector
        FaceDetector detector = FaceDetection.getClient(options);

        // Xử lý ảnh
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() > 0) {
                        // Nếu có khuôn mặt trong ảnh
                        listener.onFaceDetected(true);
                    } else {
                        // Nếu không có khuôn mặt trong ảnh
                        listener.onFaceDetected(false);
                    }
                })
                .addOnFailureListener(e -> {
                    // Lỗi khi nhận diện
                    listener.onFaceDetected(false);
                });
    }

    // Callback interface
    public interface OnFaceDetectionListener {
        void onFaceDetected(boolean isFacePresent);
    }
}
