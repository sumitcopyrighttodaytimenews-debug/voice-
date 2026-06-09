package com.sumit.paymentalert.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("PaymentAlertPrefs", Context.MODE_PRIVATE)

    var upiId: String
        get() = prefs.getString("upi_id", "7488625014@ybl") ?: "7488625014@ybl"
        set(value) = prefs.edit().putString("upi_id", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "Sumit Kumar") ?: "Sumit Kumar"
        set(value) = prefs.edit().putString("user_name", value).apply()

    var ttsSpeed: Float
        get() = prefs.getFloat("tts_speed", 0.95f)
        set(value) = prefs.edit().putFloat("tts_speed", value).apply()

    var ttsPitch: Float
        get() = prefs.getFloat("tts_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("tts_pitch", value).apply()

    var isChimeEnabled: Boolean
        get() = prefs.getBoolean("is_chime_enabled", true)
        set(value) = prefs.edit().putBoolean("is_chime_enabled", value).apply()

    var ttsLanguage: String
        get() = prefs.getString("tts_language", "Mixed") ?: "Mixed"
        set(value) = prefs.edit().putString("tts_language", value).apply()
}
