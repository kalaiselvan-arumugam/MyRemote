package com.kalaiselvan.myremote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // StateFlows for reactive UI updates
    private val _darkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _animationsEnabled = MutableStateFlow(prefs.getBoolean("animations_enabled", true))
    val animationsEnabled: StateFlow<Boolean> = _animationsEnabled.asStateFlow()

    private val _physicalButtonsEnabled = MutableStateFlow(prefs.getBoolean("physical_buttons_enabled", true))
    val physicalButtonsEnabled: StateFlow<Boolean> = _physicalButtonsEnabled.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean("dark_theme", enabled).apply()
        _darkTheme.value = enabled
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("animations_enabled", enabled).apply()
        _animationsEnabled.value = enabled
    }

    fun setPhysicalButtonsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("physical_buttons_enabled", enabled).apply()
        _physicalButtonsEnabled.value = enabled
    }
}
