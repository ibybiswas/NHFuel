package com.nh.fuel

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.room.Room
import com.nh.fuel.data.DailyFuelRecord
import com.nh.fuel.data.FuelDatabase
import com.nh.fuel.ui.MainContainerScreen
import com.nh.fuel.ui.NavBarPreferences
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var database: FuelDatabase
    private lateinit var navBarPreferences: NavBarPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Lets our Compose content draw behind the status bar and nav bar
        // (transparent system bars) instead of the OS reserving opaque
        // strips for them — this must run before super.onCreate(). The
        // header/bottom nav in HomeScreen.kt already use
        // Modifier.statusBarsPadding()/.windowInsetsPadding(navigationBars)
        // to avoid overlapping the system icons; without this call those
        // modifiers had nothing to react to, since the OS was consuming the
        // inset space itself and drawing opaque bars on top of our content.
        // The app's theme is always light (MaterialTheme() below never
        // switches to a dark color scheme), so the bars are pinned to dark
        // icons via SystemBarStyle.light() rather than .auto().
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        // Pin dark status/nav bar icons explicitly too, in case a device
        // resets bar appearance on a config change after launch.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        database = Room.databaseBuilder(
            applicationContext,
            FuelDatabase::class.java,
            "nh_fuel_db"
        ).fallbackToDestructiveMigration().build()

        navBarPreferences = NavBarPreferences(applicationContext)

        setContent {
            MaterialTheme {
                val coroutineScope = rememberCoroutineScope()
                var currentDate by remember {
                    mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                }

                val recordFlow = database.fuelDao().getRecordByDate(currentDate).collectAsState(initial = null)
                val currentRecord = recordFlow.value ?: DailyFuelRecord(date = currentDate)

                val navBarOpacity by navBarPreferences.opacityFlow.collectAsState(
                    initial = NavBarPreferences.DEFAULT_GLASS_OPACITY
                )

                MainContainerScreen(
                    record = currentRecord,
                    navBarOpacity = navBarOpacity,
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
                            navBarPreferences.saveOpacity(newOpacity)
                        }
                    }
                )
            }
        }
    }
}
