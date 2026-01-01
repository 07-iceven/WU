package com.iceven.wu

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    LIGHT, DARK, SYSTEM
}

class ThemeStorage(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val key = "theme_mode"

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun getThemeMode(): AppThemeMode {
        val name = prefs.getString(key, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (e: IllegalArgumentException) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(key, mode.name).apply()
        _themeMode.value = mode
    }
}

@Composable
fun rememberThemeStorage(): ThemeStorage {
    val context = LocalContext.current
    return remember(context) { ThemeStorage(context) }
}

@Composable
fun shouldUseDarkTheme(themeStorage: ThemeStorage): Boolean {
    val themeMode by themeStorage.themeMode.collectAsState()
    return when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
}