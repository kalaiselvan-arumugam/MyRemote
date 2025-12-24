package com.kalaiselvan.myremote

import android.content.Context
import android.content.SharedPreferences

class IrCodeRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ir_codes", Context.MODE_PRIVATE)

    // Default codes
    private val defaults = mapOf(
        "Power" to "0x00FF00FF",
        "Mode" to "0x00FF02FD",
        "Mute" to "0x00FF13EC",
        "Play/Pause" to "0x00FF01FE",
        "Prev" to "0x00FF0FF0",
        "Next" to "0x00FF10EF",
        "EQ" to "0x00FF12ED",
        "Vol-" to "0x00FF11EE",
        "Vol+" to "0x00FF0EF1",
        "0" to "0x00FF0CF3",
        "RPT" to "", // NA
        "U/SD" to "", // NA
        "1" to "0x00FF03FC",
        "2" to "0x00FF04FB",
        "3" to "0x00FF05FA",
        "4" to "0x00FF06F9",
        "5" to "0x00FF07F8",
        "6" to "0x00FF08F7",
        "7" to "0x00FF09F6",
        "8" to "0x00FF0AF5",
        "9" to "0x00FF0BF4"
    )

    fun getCode(key: String): String {
        return prefs.getString(key, defaults[key]) ?: defaults[key] ?: "0x00000000"
    }

    fun setCode(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getAllCodes(): Map<String, String> {
        val all = mutableMapOf<String, String>()
        defaults.keys.forEach { key ->
            all[key] = getCode(key)
        }
        return all
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
