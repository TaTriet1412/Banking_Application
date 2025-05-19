package com.example.bankingapplication.Api;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VeriphoneApi {

    private static final String API_KEY = "7169F3F6569C4F319A6642A87A94F672";
    private static final String BASE_URL = "https://api.veriphone.io/v2/verify?phone=";

    public interface VeriphoneCallback {
        void onResult(String carrierName); // trả về tên nhà mạng hoặc null nếu không hợp lệ
    }


    public static void checkPhoneNumberWithVeriphone(String phoneNumber, VeriphoneCallback callback) {
        String formattedPhone = phoneNumber.startsWith("0") ? "84" + phoneNumber.substring(1) : phoneNumber;
        String url = BASE_URL + formattedPhone + "&key=" + API_KEY;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onResult(null); // lỗi kết nối
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onResult(null); // lỗi HTTP
                    return;
                }

                String responseBody = response.body().string();

                try {
                    JSONObject json = new JSONObject(responseBody);
                    boolean isValid = json.optBoolean("phone_valid", false);
                    String carrier = json.optString("carrier", null);

                    if (isValid && carrier != null && !carrier.isEmpty()) {
                        callback.onResult(carrier);
                    } else {
                        callback.onResult(null); // không hợp lệ
                    }
                } catch (JSONException e) {
                    callback.onResult(null); // lỗi JSON
                }
            }
        });
    }
}
