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
import com.nh.fuel.data.DayShift
import com.nh.fuel.data.DispenserShift
import com.nh.fuel.data.FuelDatabase
import com.nh.fuel.data.NozzleShift
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
                val dbRecord = recordFlow.value

                val allRecordsFlow = database.fuelDao().getAllRecords().collectAsState(initial = emptyList())
                val allRecords = allRecordsFlow.value

                // Dynamic carry-forward logic for unfinalized/unfinished previous days
                val currentRecord = remember(dbRecord, currentDate, allRecords) {
                    if (dbRecord != null) {
                        dbRecord
                    } else {
                        val previousRecord = allRecords
                            .filter { it.date < currentDate }
                            .maxByOrNull { it.date }

                        val newRec = if (previousRecord != null) {
                            fun getLatestClose(
                                s3Close: Double,
                                s2Close: Double,
                                s1Close: Double,
                                s1Open: Double
                            ): Double {
                                return when {
                                    s3Close > 0.0 -> s3Close
                                    s2Close > 0.0 -> s2Close
                                    s1Close > 0.0 -> s1Close
                                    else -> s1Open
                                }
                            }

                            val p1Mpd1N2 = getLatestClose(
                                previousRecord.shift3.mpd1.petrolN2.close,
                                previousRecord.shift2.mpd1.petrolN2.close,
                                previousRecord.shift1.mpd1.petrolN2.close,
                                previousRecord.shift1.mpd1.petrolN2.open
                            )
                            val p1Mpd1N3 = getLatestClose(
                                previousRecord.shift3.mpd1.petrolN3.close,
                                previousRecord.shift2.mpd1.petrolN3.close,
                                previousRecord.shift1.mpd1.petrolN3.close,
                                previousRecord.shift1.mpd1.petrolN3.open
                            )
                            val d1Mpd1N1 = getLatestClose(
                                previousRecord.shift3.mpd1.dieselN1.close,
                                previousRecord.shift2.mpd1.dieselN1.close,
                                previousRecord.shift1.mpd1.dieselN1.close,
                                previousRecord.shift1.mpd1.dieselN1.open
                            )
                            val d1Mpd1N4 = getLatestClose(
                                previousRecord.shift3.mpd1.dieselN4.close,
                                previousRecord.shift2.mpd1.dieselN4.close,
                                previousRecord.shift1.mpd1.dieselN4.close,
                                previousRecord.shift1.mpd1.dieselN4.open
                            )

                            val p1Mpd2N2 = getLatestClose(
                                previousRecord.shift3.mpd2.petrolN2.close,
                                previousRecord.shift2.mpd2.petrolN2.close,
                                previousRecord.shift1.mpd2.petrolN2.close,
                                previousRecord.shift1.mpd2.petrolN2.open
                            )
                            val p1Mpd2N3 = getLatestClose(
                                previousRecord.shift3.mpd2.petrolN3.close,
                                previousRecord.shift2.mpd2.petrolN3.close,
                                previousRecord.shift1.mpd2.petrolN3.close,
                                previousRecord.shift1.mpd2.petrolN3.open
                            )
                            val d1Mpd2N1 = getLatestClose(
                                previousRecord.shift3.mpd2.dieselN1.close,
                                previousRecord.shift2.mpd2.dieselN1.close,
                                previousRecord.shift1.mpd2.dieselN1.close,
                                previousRecord.shift1.mpd2.dieselN1.open
                            )
                            val d1Mpd2N4 = getLatestClose(
                                previousRecord.shift3.mpd2.dieselN4.close,
                                previousRecord.shift2.mpd2.dieselN4.close,
                                previousRecord.shift1.mpd2.dieselN4.close,
                                previousRecord.shift1.mpd2.dieselN4.open
                            )

                            val carriedShift1 = DayShift(
                                shiftNumber = 1,
                                mpd1 = DispenserShift(
                                    petrolN2 = NozzleShift(open = p1Mpd1N2),
                                    petrolN3 = NozzleShift(open = p1Mpd1N3),
                                    dieselN1 = NozzleShift(open = d1Mpd1N1),
                                    dieselN4 = NozzleShift(open = d1Mpd1N4)
                                ),
                                mpd2 = DispenserShift(
                                    petrolN2 = NozzleShift(open = p1Mpd2N2),
                                    petrolN3 = NozzleShift(open = p1Mpd2N3),
                                    dieselN1 = NozzleShift(open = d1Mpd2N1),
                                    dieselN4 = NozzleShift(open = d1Mpd2N4)
                                )
                            )

                            DailyFuelRecord(
                                date = currentDate,
                                petrolTotal = previousRecord.currentPetrolStorage,
                                petrolRefill = previousRecord.petrolRefill,
                                petrolVariation = previousRecord.petrolVariation,
                                lastPetrolRefill = previousRecord.lastPetrolRefill,
                                lastPetrolVariationAmount = previousRecord.lastPetrolVariationAmount,
                                lastPetrolVariationTime = previousRecord.lastPetrolVariationTime,
                                lastPetrolDipAmount = previousRecord.lastPetrolDipAmount,
                                lastPetrolDipTime = previousRecord.lastPetrolDipTime,
                                dieselTotal = previousRecord.currentDieselStorage,
                                dieselRefill = previousRecord.dieselRefill,
                                dieselVariation = previousRecord.dieselVariation,
                                lastDieselRefill = previousRecord.lastDieselRefill,
                                lastDieselVariationAmount = previousRecord.lastDieselVariationAmount,
                                lastDieselVariationTime = previousRecord.lastDieselVariationTime,
                                lastDieselDipAmount = previousRecord.lastDieselDipAmount,
                                lastDieselDipTime = previousRecord.lastDieselDipTime,
                                petrolPrice = previousRecord.petrolPrice,
                                dieselPrice = previousRecord.dieselPrice,
                                shift1 = carriedShift1
                            )
                        } else {
                            DailyFuelRecord(date = currentDate)
                        }

                        // Auto-persist initial carry-forward record to DB immediately
                        coroutineScope.launch {
                            database.fuelDao().insertOrUpdate(newRec)
                        }
                        newRec
                    }
                }

                val expensesFlow = database.expenseDao().getAllExpenses().collectAsState(initial = emptyList())
                val allExpenses = expensesFlow.value

                val creditsFlow = database.creditDao().getAllCredits().collectAsState(initial = emptyList())
                val allCredits = creditsFlow.value

                val navBarOpacity by appPreferences.opacityFlow.collectAsState(
                    initial = AppPreferences.DEFAULT_GLASS_OPACITY
                )

                MainContainerScreen(
                    record = currentRecord,
                    allRecords = allRecords,
                    allExpenses = allExpenses,
                    allCredits = allCredits,
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
                    },
                    onAddOrUpdateCredit = { creditRecord ->
                        coroutineScope.launch {
                            database.creditDao().insertOrUpdate(creditRecord)
                        }
                    },
                    onDeleteCredit = { creditRecord ->
                        coroutineScope.launch {
                            database.creditDao().deleteCredit(creditRecord)
                        }
                    }
                )
            }
        }
    }
}
