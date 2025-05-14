package com.example.bankingapplication.Utils;

import android.content.Context;
import android.media.AudioManager;

public class AudioEffectUtils {
    public static void clickEffect(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f);
        }
    }
}