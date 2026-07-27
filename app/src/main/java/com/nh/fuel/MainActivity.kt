package com.nh.fuel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.room.Room
import com.nh.fuel.data.DailyFuelRecord
import com.nh.fuel.data.FuelDatabase
import com.nh.fuel.ui.HomeScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var database: FuelDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = Room.databaseBuilder(
            applicationContext,
            FuelDatabase::class.java,
            "nh_fuel_db"
        ).build()

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        setContent {
            MaterialTheme {
                val coroutineScope = rememberCoroutineScope()
                val recordFlow = database.fuelDao().getRecordByDate(todayDate).collectAsState(initial = null)

                val currentRecord = recordFlow.value ?: DailyFuelRecord(date = todayDate)

                HomeScreen(
                    record = currentRecord,
                    onRecordChanged = { updatedRecord ->
                        coroutineScope.launch {
                            database.fuelDao().insertOrUpdate(updatedRecord)
                        }
                    }
                )
            }
        }
    }
}
