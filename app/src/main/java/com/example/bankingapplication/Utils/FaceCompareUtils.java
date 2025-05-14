package com.example.bankingapplication.Utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.dikamahard.facecomparison.FaceCompare;

public class FaceCompareUtils {
    public interface FaceCompareCallback {
        void onMatch();
        void onMismatch();
        void onError(String message);
    }

    public void compareFaces(Bitmap avatar, Bitmap nationalIdFront, Context context, FaceCompareCallback callback) {
        if (avatar == null || nationalIdFront == null) {
            callback.onError("Ảnh cá nhân và ảnh CCCD không được để trống"); // ✅ Callback lỗi
            return;
        }
        try {
            FaceCompare faceCompare = new FaceCompare(context.getAssets());
            faceCompare.compareFaces(avatar, nationalIdFront, isMatch -> {
                ((Activity) context).runOnUiThread(() -> {
                    if (isMatch) {
                        callback.onMatch(); // ✅ Callback khi đúng
                    } else {
                        Toast.makeText(context, "Khuôn mặt không trùng khớp", Toast.LENGTH_SHORT).show();
                        callback.onMismatch(); // ✅ Callback khi sai
                    }
                });
                return null;
            });
        } catch (Exception e) {
            callback.onError("Lỗi so sánh khuôn mặt: " + e.getMessage()); // ✅ Callback lỗi
            Log.e("FaceCompareUtils", "compareFaces: ", e);
        }
    }
}