package com.nh.fuel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
        super.onCreate(savedInstanceState)

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

                val navBarOpacity by navBarPreferences.opacityFlow.collectAsState(initial = 0.85f)

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
