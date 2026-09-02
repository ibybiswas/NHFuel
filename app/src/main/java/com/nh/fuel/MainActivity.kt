package com.nh.fuel

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.nh.fuel.data.ActivityLogger
import com.nh.fuel.data.AppUserSession
import com.nh.fuel.data.DailyFuelRecord
import com.nh.fuel.data.DayShift
import com.nh.fuel.data.DispenserShift
import com.nh.fuel.data.FirestoreRepository
import com.nh.fuel.data.KeyStatus
import com.nh.fuel.data.NozzleShift
import com.nh.fuel.data.RefillEvent
import com.nh.fuel.data.Role
import com.nh.fuel.data.StaffAccessKey
import com.nh.fuel.data.UserSessionManager
import com.nh.fuel.ui.AppPreferences
import com.nh.fuel.ui.LoginScreen
import com.nh.fuel.ui.MainContainerScreen
import com.nh.fuel.ui.ThemeMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
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
                val context = LocalContext.current
                val firestoreRepository = remember { FirestoreRepository() }

                var currentSession by remember { mutableStateOf<AppUserSession?>(null) }
                var isCheckingSession by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    currentSession = UserSessionManager.getSavedSession(context)
                    isCheckingSession = false
                }

                // --- REAL-TIME ACCESS KEY & PRIVILEGE SYNCHRONIZER ---
                LaunchedEffect(currentSession?.emailOrKey) {
                    val session = currentSession ?: return@LaunchedEffect
                    if (!session.isOwnerLogin) {
                        val db = FirebaseFirestore.getInstance()
                        val cleanCode = session.emailOrKey.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
                        db.collection("access_keys")
                            .addSnapshotListener { snapshot, _ ->
                                if (snapshot != null) {
                                    val matchingDoc = snapshot.documents.find { doc ->
                                        val keyObj = doc.toObject(StaffAccessKey::class.java)
                                        keyObj?.accessCode?.replace(Regex("[^A-Za-z0-9]"), "")?.uppercase() == cleanCode
                                    }
                                    val keyObj = matchingDoc?.toObject(StaffAccessKey::class.java)

                                    if (matchingDoc == null || keyObj?.status != KeyStatus.ACTIVE) {
                                        coroutineScope.launch {
                                            UserSessionManager.clearSession(context)
                                            currentSession = null
                                        }
                                    } else {
                                        val updatedSession = session.copy(
                                            canEditPastDates = keyObj.canEditPastDates,
                                            canEditFinancePastDates = keyObj.canEditFinancePastDates,
                                            role = keyObj.role,
                                            isReadOnly = keyObj.isReadOnly,
                                            displayName = keyObj.nickname
                                        )
                                        if (updatedSession.isReadOnly != currentSession?.isReadOnly ||
                                            updatedSession.canEditPastDates != currentSession?.canEditPastDates ||
                                            updatedSession.canEditFinancePastDates != currentSession?.canEditFinancePastDates ||
                                            updatedSession.role != currentSession?.role ||
                                            updatedSession.displayName != currentSession?.displayName
                                        ) {
                                            currentSession = updatedSession
                                            coroutineScope.launch {
                                                UserSessionManager.saveSession(context, updatedSession)
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }

                if (isCheckingSession) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (currentSession == null) {
                    LoginScreen(
                        onLoginSuccess = { session ->
                            currentSession = session
                            coroutineScope.launch {
                                UserSessionManager.saveSession(context, session)
                            }
                        }
                    )
                } else {
                    val activeSession = currentSession!!

                    val allRecordsFlow = firestoreRepository.observeAllFuelRecords().collectAsState(initial = emptyList())
                    val allRecords = allRecordsFlow.value
                    val allExpensesFlow = firestoreRepository.observeAllExpenses().collectAsState(initial = emptyList())
                    val allExpenses = allExpensesFlow.value
                    val allCreditsFlow = firestoreRepository.observeAllCredits().collectAsState(initial = emptyList())
                    val allCredits = allCreditsFlow.value

                    var activeBusinessDate by remember {
                        mutableStateOf(
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )
                    }

                    var hasAnchoredInitialDate by remember { mutableStateOf(false) }

                    LaunchedEffect(allRecords) {
                        if (!hasAnchoredInitialDate && allRecords.isNotEmpty()) {
                            val unfinalizedRecord = allRecords.sortedBy { it.date }.find { !it.shift3.isComplete }
                            if (unfinalizedRecord != null) {
                                activeBusinessDate = unfinalizedRecord.date
                            } else {
                                val maxDate = allRecords.maxByOrNull { it.date }?.date
                                if (maxDate != null) activeBusinessDate = maxDate
                            }
                            hasAnchoredInitialDate = true
                        }
                    }

                    val dbRecord = allRecords.find { it.date == activeBusinessDate }

                    val currentRecord = remember(dbRecord, activeBusinessDate, allRecords) {
                        if (dbRecord != null) {
                            dbRecord
                        } else {
                            val previousRecord = allRecords
                                .filter { it.date < activeBusinessDate }
                                .maxByOrNull { it.date }

                            if (previousRecord != null) {
                                fun getLatestClose(s3: Double, s2: Double, s1: Double, s1Open: Double): Double {
                                    return when {
                                        s3 > 0.0 -> s3
                                        s2 > 0.0 -> s2
                                        s1 > 0.0 -> s1
                                        else -> s1Open
                                    }
                                }

                                val carriedShift1 = DayShift(
                                    shiftNumber = 1,
                                    mpd1 = DispenserShift(
                                        petrolN2 = NozzleShift(open = getLatestClose(previousRecord.shift3.mpd1.petrolN2.close, previousRecord.shift2.mpd1.petrolN2.close, previousRecord.shift1.mpd1.petrolN2.close, previousRecord.shift1.mpd1.petrolN2.open)),
                                        petrolN3 = NozzleShift(open = getLatestClose(previousRecord.shift3.mpd1.petrolN3.close, previousRecord.shift2.mpd1.petrolN3.close, previousRecord.shift1.mpd1.petrolN3.close, previousRecord.shift1.mpd1.petrolN3.open)),
                                        dieselN1 = NozzleShift(open = getLatestClose(previousRecord.shift3.mpd1.dieselN1.close, previousRecord.shift2.mpd1.dieselN1.close, previousRecord.shift1.mpd1.dieselN1.close, previousRecord.shift1.mpd1.dieselN1.open)),
                                        dieselN4 = NozzleShift(open = getLatestClose(previousRecord.shift3.mpd1.dieselN4.close, previousRecord.shift2.mpd1.dieselN4.close, previousRecord.shift1.mpd1.dieselN4.close, previousRecord.shift1.mpd1.dieselN4.open))
                                    ),
                                    mpd2 = DispenserShift(
                                        petrolN2 = NozzleShift(open = getLatestClose(previousRecord.shift3.mpd2.petrolN2.close, previousRecord.shift2.mpd2.petrolN2.close, previousRecord.shift1.mpd2.petrolN2.close, previousRecord.shift1.mpd2.petrolN2.open)),
                                        petrolN3 = NozzleShift(open = getLatestClose(previousRecord.shift3.mpd2.petrolN3.close, previousRecord.shift2.mpd2.petrolN3.close, previousRecord.shift1.mpd2.petrolN3.close, previousRecord.shift1.mpd2.petrolN3.open)),
                                        dieselN1 = NozzleShift(open = getLatestClose(previousRecord.shift3.mpd2.dieselN1.close, previousRecord.shift2.mpd2.dieselN1.close, previousRecord.shift1.mpd2.dieselN1.close, previousRecord.shift1.mpd2.dieselN1.open)),
                                        dieselN4 = NozzleShift(open = getLatestClose(previousRecord.shift3.mpd2.dieselN4.close, previousRecord.shift2.mpd2.dieselN4.close, previousRecord.shift1.mpd2.dieselN4.close, previousRecord.shift1.mpd2.dieselN4.open))
                                    )
                                )

                                DailyFuelRecord(
                                    date = activeBusinessDate,
                                    petrolTotal = previousRecord.currentPetrolStorage,
                                    petrolRefill = 0.0,
                                    petrolVariation = 0.0,
                                    lastPetrolRefill = RefillEvent(),
                                    lastPetrolVariationAmount = 0.0,
                                    lastPetrolVariationTime = "",
                                    lastPetrolDipAmount = previousRecord.lastPetrolDipAmount,
                                    lastPetrolDipTime = previousRecord.lastPetrolDipTime,
                                    dieselTotal = previousRecord.currentDieselStorage,
                                    dieselRefill = 0.0,
                                    dieselVariation = 0.0,
                                    lastDieselRefill = RefillEvent(),
                                    lastDieselVariationAmount = 0.0,
                                    lastDieselVariationTime = "",
                                    lastDieselDipAmount = previousRecord.lastDieselDipAmount,
                                    lastDieselDipTime = previousRecord.lastDieselDipTime,
                                    petrolPrice = previousRecord.petrolPrice,
                                    dieselPrice = previousRecord.dieselPrice,
                                    shift1 = carriedShift1
                                )
                            } else {
                                DailyFuelRecord(date = activeBusinessDate)
                            }
                        }
                    }

                    val navBarOpacity by appPreferences.opacityFlow.collectAsState(
                        initial = AppPreferences.DEFAULT_GLASS_OPACITY
                    )

                    val activityLogEnabled by appPreferences.activityLogEnabledFlow.collectAsState(
                        initial = AppPreferences.DEFAULT_ACTIVITY_LOG_ENABLED
                    )

                    // Keep the (in-memory) ActivityLogger master switch in sync with the
                    // persisted preference so log() calls anywhere in the app respect it.
                    LaunchedEffect(activityLogEnabled) {
                        ActivityLogger.setEnabled(activityLogEnabled)
                    }

                    MainContainerScreen(
                        session = activeSession,
                        record = currentRecord,
                        allRecords = allRecords,
                        allExpenses = allExpenses,
                        allCredits = allCredits,
                        navBarOpacity = navBarOpacity,
                        themeMode = themeMode,
                        activityLogEnabled = activityLogEnabled,
                        onActivityLogEnabledChanged = { enabled ->
                            coroutineScope.launch {
                                appPreferences.saveActivityLogEnabled(enabled)
                            }
                        },
                        onDeleteDayData = { targetDate ->
                            val isSuperAdmin = activeSession.isOwnerLogin || activeSession.role == Role.SUPER_ADMIN
                            if (isSuperAdmin) {
                                coroutineScope.launch {
                                    try {
                                        allExpenses.filter { it.date == targetDate }.forEach { expense ->
                                            firestoreRepository.deleteExpense(expense)
                                        }
                                        allCredits.filter { it.date == targetDate }.forEach { credit ->
                                            firestoreRepository.deleteCredit(credit)
                                        }
                                        firestoreRepository.deleteFuelRecord(targetDate)
                                        Toast.makeText(context, "All data for $targetDate has been deleted.", Toast.LENGTH_LONG).show()
                                        ActivityLogger.log(activeSession, "deleted ALL data (sales, expenses & credits) for $targetDate")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to delete day data: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onRecordChanged = { updatedRecord ->
                            if (!activeSession.isReadOnly) {
                                coroutineScope.launch {
                                    try {
                                        firestoreRepository.saveFuelRecord(updatedRecord)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to save record: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onDateSelected = { selectedDate ->
                            activeBusinessDate = selectedDate
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
                            if (!activeSession.isReadOnly) {
                                coroutineScope.launch {
                                    try {
                                        firestoreRepository.saveExpense(expenseItem)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to save expense: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onDeleteExpense = { expenseItem ->
                            if (!activeSession.isReadOnly) {
                                coroutineScope.launch {
                                    try {
                                        firestoreRepository.deleteExpense(expenseItem)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to delete expense: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onAddOrUpdateCredit = { creditRecord ->
                            if (!activeSession.isReadOnly) {
                                coroutineScope.launch {
                                    try {
                                        firestoreRepository.saveCredit(creditRecord)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to save credit ledger: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onDeleteCredit = { creditRecord ->
                            if (!activeSession.isReadOnly) {
                                coroutineScope.launch {
                                    try {
                                        firestoreRepository.deleteCredit(creditRecord)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to delete credit ledger: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onLogout = {
                            coroutineScope.launch {
                                UserSessionManager.clearSession(context)
                                currentSession = null
                            }
                        }
                    )
                }
            }
        }
    }
}
