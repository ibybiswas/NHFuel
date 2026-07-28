package com.nh.fuel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.room.Room
import com.nh.fuel.data.DailyFuelRecord
import com.nh.fuel.data.FuelDatabase
import com.nh.fuel.ui.AppPreferences
import com.nh.fuel.ui.MainContainerScreen
import com.nh.fuel.ui.ThemeMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var database: FuelDatabase
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge drawing under status & navigation bars with complete transparency
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        database = Room.databaseBuilder(
            applicationContext,
            FuelDatabase::class.java,
            "nh_fuel_db"
        ).fallbackToDestructiveMigration().build()

        appPreferences = AppPreferences(applicationContext)

        setContent {
            val themeMode by appPreferences.themeModeFlow.collectAsState(initial = ThemeMode.AUTO)
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                val coroutineScope = rememberCoroutineScope()
                var currentDate by remember {
                    mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                }

                val recordFlow = database.fuelDao().getRecordByDate(currentDate).collectAsState(initial = null)
                val currentRecord = recordFlow.value ?: DailyFuelRecord(date = currentDate)

                val navBarOpacity by appPreferences.opacityFlow.collectAsState(initial = 0.85f)

                MainContainerScreen(
                    record = currentRecord,
                    navBarOpacity = navBarOpacity,
                    themeMode = themeMode,
                    onRecordChanged = { updatedRecord ->
                        coroutineScope.launch {
                            database.fuelDao().insertOrUpdate(updatedRecord)
                        }
                    },
                    onDateSelected = { selectedDate ->
                        currentDate = selectedDate
                    },
                    onOpacityChanged = { newOpacity ->
                        coroutineScope.launch {
                            appPreferences.saveOpacity(newOpacity)
                        }
                    },
                    onThemeModeChanged = { newTheme ->
                        coroutineScope.launch {
                            appPreferences.saveThemeMode(newTheme)
                        }
                    }
                )
            }
        }
    }
}
