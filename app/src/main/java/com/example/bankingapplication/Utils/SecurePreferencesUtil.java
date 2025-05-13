package com.example.bankingapplication.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class SecurePreferencesUtil {

    private static final String FILE_NAME = "secure_prefs";

    public static EncryptedSharedPreferences getEncryptedPreferences(Context context) throws Exception {
        // Tạo master key an toàn cho EncryptedSharedPreferences
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        // Tạo EncryptedSharedPreferences
        return (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    public static void saveEmail(Context context, String email) throws Exception {
        EncryptedSharedPreferences sharedPreferences = getEncryptedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("USER_EMAIL", email);
        editor.apply();
    }

    public static String getEmail(Context context) throws Exception {
        EncryptedSharedPreferences sharedPreferences = getEncryptedPreferences(context);
        return sharedPreferences.getString("USER_EMAIL", null);
    }

    public static void deleteEmail(Context context) throws Exception {
        EncryptedSharedPreferences sharedPreferences = getEncryptedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("USER_EMAIL");
        editor.apply();
    }
}