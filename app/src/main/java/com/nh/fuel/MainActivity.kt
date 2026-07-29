package com.nh.fuel

import android.graphics.Color
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
import androidx.core.view.WindowCompat
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

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

            LaunchedEffect(isDarkTheme) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }
            }

            val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colorScheme) {
                val coroutineScope = rememberCoroutineScope()
                var currentDate by remember {
                    mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                }

                val recordFlow = database.fuelDao().getRecordByDate(currentDate).collectAsState(initial = null)
                val currentRecord = recordFlow.value ?: DailyFuelRecord(date = currentDate)

                val expensesFlow = database.expenseDao().getAllExpenses().collectAsState(initial = emptyList())
                val allExpenses = expensesFlow.value

                val navBarOpacity by appPreferences.opacityFlow.collectAsState(
                    initial = AppPreferences.DEFAULT_GLASS_OPACITY
                )

                MainContainerScreen(
                    record = currentRecord,
                    allExpenses = allExpenses,
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
                    },
                    onAddOrUpdateExpense = { expenseItem ->
                        coroutineScope.launch {
                            database.expenseDao().insertOrUpdate(expenseItem)
                        }
                    },
                    onDeleteExpense = { expenseItem ->
                        coroutineScope.launch {
                            database.expenseDao().deleteExpense(expenseItem)
                        }
                    }
                )
            }
        }
    }
}
