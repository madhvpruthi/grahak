package com.princesanitary.grahak.ui.theme

import android.content.Context
import android.content.SharedPreferences
import com.princesanitary.grahak.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

object ThemeManager {
    private const val PREFS_NAME = "grahak_theme_prefs"
    private const val KEY_THEME_MODE = "selected_theme_mode"

    private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name)
        _themeMode.value = try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.DARK.name)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }
    }

    fun setThemeMode(mode: AppThemeMode, context: Context? = null) {
        _themeMode.value = mode
        val ctx = context ?: try { ServiceLocator.application } catch (_: Exception) { null }
        ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_THEME_MODE, mode.name)
            ?.apply()
    }
}
