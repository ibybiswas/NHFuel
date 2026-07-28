package com.nh.fuel.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nav_bar_prefs")

class NavBarPreferences(private val context: Context) {
    companion object {
        val NAV_BAR_OPACITY_KEY = floatPreferencesKey("nav_bar_opacity")
    }

    val opacityFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[NAV_BAR_OPACITY_KEY] ?: 0.85f // Default 85% opacity
    }

    suspend fun saveOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[NAV_BAR_OPACITY_KEY] = opacity
        }
    }
}
