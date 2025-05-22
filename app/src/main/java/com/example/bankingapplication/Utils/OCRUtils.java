package com.example.bankingapplication.Utils;

import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.HashMap;
import java.util.Map;

public class OCRUtils {

    public interface OcrCallback {
        void onSuccess(Map<String, String> extractedData);
        void onFailure(String errorMessage);
    }


    public static void extractTextFromCardFront(Bitmap bitmap, OcrCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String rawText = visionText.getText();
                    String[] lines = rawText.split("\\r?\\n");

                    // ✅ Khởi tạo map với key mặc định
                    Map<String, String> result = new HashMap<>();
                    result.put("nationalId", "");

                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].trim().toLowerCase();

                        // ✅ Số CCCD
                        if (line.contains("số") || line.contains("no")) {
                            int index = lines[i].indexOf(":");
                            if (index != -1 && lines[i].length() > index + 1) {
                                String candidate = lines[i].substring(index + 1).replaceAll("[^0-9]", "");
                                if (candidate.length() == 12) {
                                    result.put("nationalId", candidate);
                                }
                            }
                        }
                    }

                    // ✅ Nếu có giá trị đều rỗng thì xem là thất bại
                    boolean anyEmpty = result.values().stream().anyMatch(String::isEmpty);
                    if (anyEmpty) {
                        callback.onFailure("Không trích xuất được dữ liệu hợp lệ từ ảnh CCCD.");
                    } else {
                        callback.onSuccess(result);
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Lỗi nhận diện văn bản: " + e.getMessage());
                });

    }

}
