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

        /** Opacity used the very first time the app runs, before any explicit choice is saved. */
        const val DEFAULT_GLASS_OPACITY = 0.55f

        /** The bar never goes fully invisible or fully opaque — both ends still read as "glass". */
        const val MIN_GLASS_OPACITY = 0.15f
        const val MAX_GLASS_OPACITY = 1f
    }

    val opacityFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        (preferences[NAV_BAR_OPACITY_KEY] ?: DEFAULT_GLASS_OPACITY)
            .coerceIn(MIN_GLASS_OPACITY, MAX_GLASS_OPACITY)
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
            preferences[NAV_BAR_OPACITY_KEY] = opacity.coerceIn(MIN_GLASS_OPACITY, MAX_GLASS_OPACITY)
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}
