package com.nh.fuel.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

enum class ThemeMode {
    LIGHT, DARK, AUTO
}

class AppPreferences(private val context: Context) {
    companion object {
        val NAV_BAR_OPACITY_KEY = floatPreferencesKey("nav_bar_opacity")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    val opacityFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[NAV_BAR_OPACITY_KEY] ?: 0.85f
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[THEME_MODE_KEY]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.AUTO
        }
    }

    suspend fun saveOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[NAV_BAR_OPACITY_KEY] = opacity
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}
