package com.nh.fuel.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.fuel.data.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    record: DailyFuelRecord,
    allRecords: List<DailyFuelRecord> = emptyList(),
    allExpenses: List<ExpenseItem> = emptyList(),
    navBarOpacity: Float,
    themeMode: ThemeMode,
    onRecordChanged: (DailyFuelRecord) -> Unit,
    onDateSelected: (String) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAddOrUpdateExpense: (ExpenseItem) -> Unit = {},
    onDeleteExpense: (ExpenseItem) -> Unit = {}
) {
    var selectedMainTab by remember { mutableStateOf(0) }
    var currentTimeString by remember { mutableStateOf("") }
    var showThemeMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = SimpleDateFormat("EEE, dd MMM yyyy | hh:mm:ss a", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + HEADER_CONTENT_HEIGHT
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            NAV_BAR_HEIGHT + NAV_BAR_VERTICAL_MARGIN * 2

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedMainTab) {
                0 -> HomeScreenContent(
                    record = record,
                    onRecordChanged = onRecordChanged,
                    onDateSelected = onDateSelected,
                    topInset = topInset,
                    bottomInset = bottomInset
                )
                1 -> SalesScreen(
                    currentRecord = record,
                    allRecords = allRecords,
                    onRecordChanged = onRecordChanged,
                    onDateSelected = onDateSelected,
                    topInset = topInset,
                    bottomInset = bottomInset
                )
                2 -> PlaceholderTab("Reports & Analytics")
                3 -> ExpendScreen(
                    currentRecordDate = record.date,
                    allExpenses = allExpenses,
                    onAddOrUpdateExpense = onAddOrUpdateExpense,
                    onDeleteExpense = onDeleteExpense,
                    topInset = topInset,
                    bottomInset = bottomInset
                )
                4 -> SettingsScreen(
                    currentOpacity = navBarOpacity,
                    currentThemeMode = themeMode,
                    onOpacityChanged = onOpacityChanged,
                    onThemeModeChanged = onThemeModeChanged,
                    topInset = topInset,
                    bottomInset = bottomInset
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .cleanGlassBackground(
                    tint = MaterialTheme.colorScheme.surfaceContainer,
                    opacity = HEADER_GLASS_OPACITY
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(HEADER_CONTENT_HEIGHT)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NH FUEL STATION",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 1.1.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentTimeString,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = { showThemeMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = when (themeMode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.AUTO -> Icons.Default.BrightnessAuto
                            },
                            contentDescription = "Theme Switcher",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        Text(
                            text = "Appearance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DropdownMenuItem(
                            text = { Text("Light", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = if (themeMode == ThemeMode.LIGHT) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            onClick = {
                                onThemeModeChanged(ThemeMode.LIGHT)
                                showThemeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dark", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = if (themeMode == ThemeMode.DARK) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            onClick = {
                                onThemeModeChanged(ThemeMode.DARK)
                                showThemeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Auto (system default)", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = if (themeMode == ThemeMode.AUTO) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            onClick = {
                                onThemeModeChanged(ThemeMode.AUTO)
                                showThemeMenu = false
                            }
                        )
                    }
                }
            }
        }

        NHFuelBottomNav(
            selectedIndex = selectedMainTab,
            glassOpacity = navBarOpacity,
            onTabSelected = { selectedMainTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private const val HEADER_GLASS_OPACITY = 0.6f
private val HEADER_CONTENT_HEIGHT = 52.dp
private val NAV_BAR_HEIGHT = 64.dp
private val NAV_BAR_VERTICAL_MARGIN = 8.dp

@Composable
private fun NHFuelBottomNav(
    selectedIndex: Int,
    glassOpacity: Float,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("Home", Icons.Default.Home, 0),
        Triple("Sales", Icons.Default.Payments, 1),
        Triple("Report", Icons.Default.Assessment, 2),
        Triple("Expend", Icons.Default.AccountBalanceWallet, 3),
        Triple("Setting", Icons.Default.Settings, 4)
    )
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = NAV_BAR_VERTICAL_MARGIN)
            .height(NAV_BAR_HEIGHT)
            .clip(shape)
            .cleanGlassBackground(
                tint = MaterialTheme.colorScheme.surfaceContainer,
                opacity = glassOpacity
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), shape)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            val slotWidth = maxWidth / items.size
            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "navIndicatorOffset"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(slotWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEach { (label, icon, index) ->
                NHFuelBottomNavItem(
                    icon = icon,
                    label = label,
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun NHFuelBottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "navItemColor"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

internal fun Modifier.cleanGlassBackground(tint: Color, opacity: Float): Modifier {
    val clamped = opacity.coerceIn(AppPreferences.MIN_GLASS_OPACITY, AppPreferences.MAX_GLASS_OPACITY)
    return this.background(tint.copy(alpha = clamped))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    record: DailyFuelRecord,
    onRecordChanged: (DailyFuelRecord) -> Unit,
    onDateSelected: (String) -> Unit,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    var selectedShiftTab by remember { mutableStateOf(1) }
    var showDatePickerModal by remember { mutableStateOf(false) }
    var showSaveFullDayDialog by remember { mutableStateOf(false) }

    LaunchedEffect(record.date) {
        selectedShiftTab = 1
    }

    val isDark = isSystemInDarkTheme()
    val petrolColor = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
    val dieselColor = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)
    val stockColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(topInset + 4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Selected Date: ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = record.date,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { showDatePickerModal = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Date",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (showDatePickerModal) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePickerModal = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePickerModal = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            onDateSelected(sdf.format(Date(millis)))
                        }
                    }) { Text("Select Date", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerModal = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FuelTankCard(
                modifier = Modifier.weight(1f),
                title = "Petrol Tank Storage",
                color = petrolColor,
                stockColor = stockColor,
                exactStock = record.currentPetrolStorage,
                lastDipAmount = record.lastPetrolDipAmount,
                lastDipTime = record.lastPetrolDipTime,
                cumulativeRefill = record.petrolRefill,
                cumulativeVariation = record.petrolVariation,
                currentStorage = record.currentPetrolStorage,
                lastRefill = record.lastPetrolRefill,
                lastVariationAmount = record.lastPetrolVariationAmount,
                lastVariationTime = record.lastPetrolVariationTime,
                onConfirmExactStock = { confirmedVal, diff ->
                    val clampedVal = max(0.0, confirmedVal)
                    val nowStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                    val isFirstEntry = record.petrolRefill == 0.0 && record.petrolVariation == 0.0 && record.totalPetrolSell == 0.0

                    if (isFirstEntry) {
                        onRecordChanged(
                            record.copy(
                                petrolTotal = clampedVal,
                                lastPetrolDipAmount = clampedVal,
                                lastPetrolDipTime = nowStr
                            )
                        )
                    } else if (diff != 0.0) {
                        val newVariation = record.petrolVariation + diff
                        val requiredTotal = clampedVal + record.totalPetrolSell - record.petrolRefill - newVariation
                        onRecordChanged(
                            record.copy(
                                petrolTotal = max(0.0, requiredTotal),
                                petrolVariation = newVariation,
                                lastPetrolVariationAmount = diff,
                                lastPetrolVariationTime = nowStr,
                                lastPetrolDipAmount = clampedVal,
                                lastPetrolDipTime = nowStr
                            )
                        )
                    } else {
                        val requiredTotal = clampedVal + record.totalPetrolSell - record.petrolRefill - record.petrolVariation
                        onRecordChanged(
                            record.copy(
                                petrolTotal = max(0.0, requiredTotal),
                                lastPetrolDipAmount = clampedVal,
                                lastPetrolDipTime = nowStr
                            )
                        )
                    }
                },
                onUndoExactStock = {
                    onRecordChanged(
                        record.copy(
                            petrolVariation = record.petrolVariation - record.lastPetrolVariationAmount,
                            lastPetrolVariationAmount = 0.0,
                            lastPetrolVariationTime = "",
                            lastPetrolDipAmount = 0.0,
                            lastPetrolDipTime = ""
                        )
                    )
                },
                onAddRefill = { addedLitre ->
                    val nowStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                    val validAdded = max(0.0, addedLitre)
                    val newRefill = record.petrolRefill + validAdded
                    onRecordChanged(
                        record.copy(
                            petrolRefill = newRefill,
                            lastPetrolRefill = RefillEvent(amount = validAdded, timestamp = nowStr)
                        )
                    )
                },
                onUndoLastRefill = {
                    val lastAmount = record.lastPetrolRefill.amount
                    onRecordChanged(
                        record.copy(
                            petrolRefill = max(0.0, record.petrolRefill - lastAmount),
                            lastPetrolRefill = RefillEvent()
                        )
                    )
                }
            )

            FuelTankCard(
                modifier = Modifier.weight(1f),
                title = "Diesel Tank Storage",
                color = dieselColor,
                stockColor = stockColor,
                exactStock = record.currentDieselStorage,
                lastDipAmount = record.lastDieselDipAmount,
                lastDipTime = record.lastDieselDipTime,
                cumulativeRefill = record.dieselRefill,
                cumulativeVariation = record.dieselVariation,
                currentStorage = record.currentDieselStorage,
                lastRefill = record.lastDieselRefill,
                lastVariationAmount = record.lastDieselVariationAmount,
                lastVariationTime = record.lastDieselVariationTime,
                onConfirmExactStock = { confirmedVal, diff ->
                    val clampedVal = max(0.0, confirmedVal)
                    val nowStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                    val isFirstEntry = record.dieselRefill == 0.0 && record.dieselVariation == 0.0 && record.totalDieselSell == 0.0

                    if (isFirstEntry) {
                        onRecordChanged(
                            record.copy(
                                dieselTotal = clampedVal,
                                lastDieselDipAmount = clampedVal,
                                lastDieselDipTime = nowStr
                            )
                        )
                    } else if (diff != 0.0) {
                        val newVariation = record.dieselVariation + diff
                        val requiredTotal = clampedVal + record.totalDieselSell - record.dieselRefill - newVariation
                        onRecordChanged(
                            record.copy(
                                dieselTotal = max(0.0, requiredTotal),
                                dieselVariation = newVariation,
                                lastDieselVariationAmount = diff,
                                lastDieselVariationTime = nowStr,
                                lastDieselDipAmount = clampedVal,
                                lastDieselDipTime = nowStr
                            )
                        )
                    } else {
                        val requiredTotal = clampedVal + record.totalDieselSell - record.dieselRefill - record.dieselVariation
                        onRecordChanged(
                            record.copy(
                                dieselTotal = max(0.0, requiredTotal),
                                lastDieselDipAmount = clampedVal,
                                lastDieselDipTime = nowStr
                            )
                        )
                    }
                },
                onUndoExactStock = {
                    onRecordChanged(
                        record.copy(
                            dieselVariation = record.dieselVariation - record.lastDieselVariationAmount,
                            lastDieselVariationAmount = 0.0,
                            lastDieselVariationTime = "",
                            lastDieselDipAmount = 0.0,
                            lastDieselDipTime = ""
                        )
                    )
                },
                onAddRefill = { addedLitre ->
                    val nowStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                    val validAdded = max(0.0, addedLitre)
                    val newRefill = record.dieselRefill + validAdded
                    onRecordChanged(
                        record.copy(
                            dieselRefill = newRefill,
                            lastDieselRefill = RefillEvent(amount = validAdded, timestamp = nowStr)
                        )
                    )
                },
                onUndoLastRefill = {
                    val lastAmount = record.lastDieselRefill.amount
                    onRecordChanged(
                        record.copy(
                            dieselRefill = max(0.0, record.dieselRefill - lastAmount),
                            lastDieselRefill = RefillEvent()
                        )
                    )
                }
            )
        }

        TabRow(selectedTabIndex = selectedShiftTab - 1) {
            Tab(
                selected = selectedShiftTab == 1,
                onClick = { selectedShiftTab = 1 },
                text = { Text("Shift 1") }
            )
            Tab(
                selected = selectedShiftTab == 2,
                enabled = record.shift1.isComplete,
                onClick = { if (record.shift1.isComplete) selectedShiftTab = 2 },
                text = { Text(if (record.shift1.isComplete) "Shift 2" else "Shift 2  ") }
            )
            Tab(
                selected = selectedShiftTab == 3,
                enabled = record.shift2.isComplete,
                onClick = { if (record.shift2.isComplete) selectedShiftTab = 3 },
                text = { Text(if (record.shift2.isComplete) "Shift 3" else "Shift 3  ") }
            )
        }

        val activeShift = when (selectedShiftTab) {
            1 -> record.shift1
            2 -> record.shift2
            else -> record.shift3
        }

        ShiftInputBlock(
            shiftTitle = "Shift $selectedShiftTab Readings",
            shiftNumber = selectedShiftTab,
            shift = activeShift,
            petrolColor = petrolColor,
            dieselColor = dieselColor,
            onShiftUpdated = { updatedShift ->
                val newRecord = when (selectedShiftTab) {
                    1 -> {
                        val s2Mpd1P2 = if (updatedShift.mpd1.petrolN2.isClosed) updatedShift.mpd1.petrolN2.close else record.shift2.mpd1.petrolN2.open
                        val s2Mpd1P3 = if (updatedShift.mpd1.petrolN3.isClosed) updatedShift.mpd1.petrolN3.close else record.shift2.mpd1.petrolN3.open
                        val s2Mpd1D1 = if (updatedShift.mpd1.dieselN1.isClosed) updatedShift.mpd1.dieselN1.close else record.shift2.mpd1.dieselN1.open
                        val s2Mpd1D4 = if (updatedShift.mpd1.dieselN4.isClosed) updatedShift.mpd1.dieselN4.close else record.shift2.mpd1.dieselN4.open

                        val s2Mpd2P2 = if (updatedShift.mpd2.petrolN2.isClosed) updatedShift.mpd2.petrolN2.close else record.shift2.mpd2.petrolN2.open
                        val s2Mpd2P3 = if (updatedShift.mpd2.petrolN3.isClosed) updatedShift.mpd2.petrolN3.close else record.shift2.mpd2.petrolN3.open
                        val s2Mpd2D1 = if (updatedShift.mpd2.dieselN1.isClosed) updatedShift.mpd2.dieselN1.close else record.shift2.mpd2.dieselN1.open
                        val s2Mpd2D4 = if (updatedShift.mpd2.dieselN4.isClosed) updatedShift.mpd2.dieselN4.close else record.shift2.mpd2.dieselN4.open

                        val updatedShift2 = record.shift2.copy(
                            mpd1 = record.shift2.mpd1.copy(
                                petrolN2 = record.shift2.mpd1.petrolN2.copy(open = s2Mpd1P2),
                                petrolN3 = record.shift2.mpd1.petrolN3.copy(open = s2Mpd1P3),
                                dieselN1 = record.shift2.mpd1.dieselN1.copy(open = s2Mpd1D1),
                                dieselN4 = record.shift2.mpd1.dieselN4.copy(open = s2Mpd1D4)
                            ),
                            mpd2 = record.shift2.mpd2.copy(
                                petrolN2 = record.shift2.mpd2.petrolN2.copy(open = s2Mpd2P2),
                                petrolN3 = record.shift2.mpd2.petrolN3.copy(open = s2Mpd2P3),
                                dieselN1 = record.shift2.mpd2.dieselN1.copy(open = s2Mpd2D1),
                                dieselN4 = record.shift2.mpd2.dieselN4.copy(open = s2Mpd2D4)
                            )
                        )
                        record.copy(shift1 = updatedShift, shift2 = updatedShift2)
                    }
                    2 -> {
                        val s3Mpd1P2 = if (updatedShift.mpd1.petrolN2.isClosed) updatedShift.mpd1.petrolN2.close else record.shift3.mpd1.petrolN2.open
                        val s3Mpd1P3 = if (updatedShift.mpd1.petrolN3.isClosed) updatedShift.mpd1.petrolN3.close else record.shift3.mpd1.petrolN3.open
                        val s3Mpd1D1 = if (updatedShift.mpd1.dieselN1.isClosed) updatedShift.mpd1.dieselN1.close else record.shift3.mpd1.dieselN1.open
                        val s3Mpd1D4 = if (updatedShift.mpd1.dieselN4.isClosed) updatedShift.mpd1.dieselN4.close else record.shift3.mpd1.dieselN4.open

                        val s3Mpd2P2 = if (updatedShift.mpd2.petrolN2.isClosed) updatedShift.mpd2.petrolN2.close else record.shift3.mpd2.petrolN2.open
                        val s3Mpd2P3 = if (updatedShift.mpd2.petrolN3.isClosed) updatedShift.mpd2.petrolN3.close else record.shift3.mpd2.petrolN3.open
                        val s3Mpd2D1 = if (updatedShift.mpd2.dieselN1.isClosed) updatedShift.mpd2.dieselN1.close else record.shift3.mpd2.dieselN1.open
                        val s3Mpd2D4 = if (updatedShift.mpd2.dieselN4.isClosed) updatedShift.mpd2.dieselN4.close else record.shift3.mpd2.dieselN4.open

                        val updatedShift3 = record.shift3.copy(
                            mpd1 = record.shift3.mpd1.copy(
                                petrolN2 = record.shift3.mpd1.petrolN2.copy(open = s3Mpd1P2),
                                petrolN3 = record.shift3.mpd1.petrolN3.copy(open = s3Mpd1P3),
                                dieselN1 = record.shift3.mpd1.dieselN1.copy(open = s3Mpd1D1),
                                dieselN4 = record.shift3.mpd1.dieselN4.copy(open = s3Mpd1D4)
                            ),
                            mpd2 = record.shift3.mpd2.copy(
                                petrolN2 = record.shift3.mpd2.petrolN2.copy(open = s3Mpd2P2),
                                petrolN3 = record.shift3.mpd2.petrolN3.copy(open = s3Mpd2P3),
                                dieselN1 = record.shift3.mpd2.dieselN1.copy(open = s3Mpd2D1),
                                dieselN4 = record.shift3.mpd2.dieselN4.copy(open = s3Mpd2D4)
                            )
                        )
                        record.copy(shift2 = updatedShift, shift3 = updatedShift3)
                    }
                    else -> record.copy(shift3 = updatedShift)
                }
                onRecordChanged(newRecord)
            }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Shift Sales Breakdown:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shift 1", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("  Petrol: ${record.shift1.petrolSale} L", fontWeight = FontWeight.Bold, color = petrolColor, fontSize = 10.sp)
                        Text("  Diesel: ${record.shift1.dieselSale} L", fontWeight = FontWeight.Bold, color = dieselColor, fontSize = 10.sp)
                    }
                    if (record.shift1.isComplete || record.shift2.petrolSale > 0.0 || record.shift2.dieselSale > 0.0) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shift 2", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("  Petrol: ${record.shift2.petrolSale} L", fontWeight = FontWeight.Bold, color = petrolColor, fontSize = 10.sp)
                            Text("  Diesel: ${record.shift2.dieselSale} L", fontWeight = FontWeight.Bold, color = dieselColor, fontSize = 10.sp)
                        }
                    }
                    if (record.shift2.isComplete || record.shift3.petrolSale > 0.0 || record.shift3.dieselSale > 0.0) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shift 3", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("  Petrol: ${record.shift3.petrolSale} L", fontWeight = FontWeight.Bold, color = petrolColor, fontSize = 10.sp)
                            Text("  Diesel: ${record.shift3.dieselSale} L", fontWeight = FontWeight.Bold, color = dieselColor, fontSize = 10.sp)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text("Total 24H Full Day Sales (Shift 1 + 2 + 3):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("  Total Petrol Sold: ${record.totalPetrolSell} Litre", fontWeight = FontWeight.Bold, color = petrolColor, fontSize = 13.sp)
                Text("  Total Diesel Sold: ${record.totalDieselSell} Litre", fontWeight = FontWeight.Bold, color = dieselColor, fontSize = 13.sp)
            }
        }

        if (record.shift3.isComplete) {
            Button(
                onClick = { showSaveFullDayDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Save & Finalize Full Day Sales",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(bottomInset + 4.dp))
    }

    if (showSaveFullDayDialog) {
        AlertDialog(
            onDismissRequest = { showSaveFullDayDialog = false },
            title = {
                Text(
                    text = "Save Full Day Sales (${record.date})?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "This will finalize and lock the 24-hour cycle for ${record.date}.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Shift 3 close readings will automatically carry forward as opening meter values for the next business date.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveFullDayDialog = false
                        
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsedDate = try { sdf.parse(record.date) } catch (e: Exception) { Date() }
                        val cal = Calendar.getInstance().apply {
                            time = parsedDate ?: Date()
                            add(Calendar.DAY_OF_MONTH, 1)
                        }
                        val nextDateStr = sdf.format(cal.time)

                        val s3Mpd1 = record.shift3.mpd1
                        val s3Mpd2 = record.shift3.mpd2

                        val nextDayShift1 = DayShift(
                            shiftNumber = 1,
                            mpd1 = DispenserShift(
                                petrolN2 = NozzleShift(open = s3Mpd1.petrolN2.close),
                                petrolN3 = NozzleShift(open = s3Mpd1.petrolN3.close),
                                dieselN1 = NozzleShift(open = s3Mpd1.dieselN1.close),
                                dieselN4 = NozzleShift(open = s3Mpd1.dieselN4.close)
                            ),
                            mpd2 = DispenserShift(
                                petrolN2 = NozzleShift(open = s3Mpd2.petrolN2.close),
                                petrolN3 = NozzleShift(open = s3Mpd2.petrolN3.close),
                                dieselN1 = NozzleShift(open = s3Mpd2.dieselN1.close),
                                dieselN4 = NozzleShift(open = s3Mpd2.dieselN4.close)
                            )
                        )

                        val newNextDayRecord = DailyFuelRecord(
                            date = nextDateStr,
                            petrolTotal = record.currentPetrolStorage,
                            dieselTotal = record.currentDieselStorage,
                            petrolPrice = record.petrolPrice,
                            dieselPrice = record.dieselPrice,
                            shift1 = nextDayShift1
                        )

                        onRecordChanged(record)
                        onRecordChanged(newNextDayRecord)
                        onDateSelected(nextDateStr)
                    }
                ) {
                    Text("Confirm & Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveFullDayDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PlaceholderTab(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

@Composable
fun FuelTankCard(
    modifier: Modifier = Modifier,
    title: String,
    color: Color,
    stockColor: Color,
    exactStock: Double,
    lastDipAmount: Double,
    lastDipTime: String,
    cumulativeRefill: Double,
    cumulativeVariation: Double,
    currentStorage: Double,
    lastRefill: RefillEvent,
    lastVariationAmount: Double,
    lastVariationTime: String,
    onConfirmExactStock: (Double, Double) -> Unit,
    onUndoExactStock: () -> Unit,
    onAddRefill: (Double) -> Unit,
    onUndoLastRefill: () -> Unit
) {
    var isEditingExactStock by remember { mutableStateOf(false) }
    var pendingInput by remember(exactStock) {
        mutableStateOf(if (exactStock == 0.0) "" else if (exactStock % 1.0 == 0.0) exactStock.toLong().toString() else exactStock.toString())
    }
    var newRefillInput by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showUndoDipDialog by remember { mutableStateOf(false) }
    var showUndoRefillDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp)

            Column {
                Text("Exact Stock:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                if (!isEditingExactStock) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$exactStock L",
                            fontWeight = FontWeight.ExtraBold,
                            color = stockColor,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { isEditingExactStock = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Dip",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = pendingInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    pendingInput = input
                                }
                            },
                            label = { Text("Enter Dip", fontSize = 8.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val parsed = max(0.0, pendingInput.toDoubleOrNull() ?: 0.0)
                                if (parsed >= 0.0) {
                                    showConfirmationDialog = true
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Dip",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (lastDipTime.isNotBlank()) "Last Reading: $lastDipAmount L @ $lastDipTime" else "Last Reading: None",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (lastDipTime.isNotBlank()) {
                        IconButton(
                            onClick = { showUndoDipDialog = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo Exact Dip Reading",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = newRefillInput,
                    onValueChange = { newRefillInput = it },
                    label = { Text("Add Refill (+)", fontSize = 8.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val added = newRefillInput.toDoubleOrNull() ?: 0.0
                        if (added > 0.0) {
                            onAddRefill(added)
                            newRefillInput = ""
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) { Text("+", fontSize = 12.sp) }
            }
            Text("Total Refilled: $cumulativeRefill L", fontSize = 10.sp, color = stockColor)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Last Refill:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = if (lastRefill.timestamp.isNotBlank()) "${lastRefill.amount} L @ ${lastRefill.timestamp}" else "None",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (lastRefill.timestamp.isNotBlank()) {
                    IconButton(
                        onClick = { showUndoRefillDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo Last Refill",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Total Variation: ${if (cumulativeVariation > 0.0) "+" else ""}$cumulativeVariation L",
                fontSize = 10.sp,
                color = if (cumulativeVariation < 0.0) Color(0xFFFF5252) else stockColor
            )
            Column {
                Text("Last Variation:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = if (lastVariationTime.isNotBlank()) "${if (lastVariationAmount > 0.0) "+" else ""}$lastVariationAmount L @ $lastVariationTime" else "None",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Column {
                Text("Current Stock:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "$currentStorage L",
                    fontWeight = FontWeight.ExtraBold,
                    color = stockColor,
                    fontSize = 22.sp
                )
            }
        }
    }

    if (showConfirmationDialog) {
        val targetVal = max(0.0, pendingInput.toDoubleOrNull() ?: currentStorage)
        val diff = targetVal - currentStorage

        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Confirm Tank Dip Reading", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Entered Dip Reading: $targetVal Litres", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Expected Current Stock: $currentStorage Litres", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (diff > 0.0) {
                        Text(
                            text = "Variation Detected: +$diff Litres (surplus)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            fontSize = 12.sp
                        )
                    } else if (diff < 0.0) {
                        Text(
                            text = "Variation Detected: ${diff} Litres (shortage)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "No change detected in tank storage.",
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "This is recorded as a Variation only — it is not treated as a Refill.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Current & Exact Stock will be synced to $targetVal Litres.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        isEditingExactStock = false
                        onConfirmExactStock(targetVal, diff)
                    }
                ) { Text("Confirm & Sync", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        isEditingExactStock = false
                    }
                ) { Text("Cancel") }
            }
        )
    }

    if (showUndoDipDialog) {
        AlertDialog(
            onDismissRequest = { showUndoDipDialog = false },
            title = { Text("Undo Last Dip Reading?", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to revert the reading ($lastDipAmount L @ $lastDipTime)? This will remove its associated variation ($lastVariationAmount L) and restore previous stock states.",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUndoDipDialog = false
                        onUndoExactStock()
                    }
                ) { Text("Undo Reading", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showUndoDipDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showUndoRefillDialog) {
        AlertDialog(
            onDismissRequest = { showUndoRefillDialog = false },
            title = { Text("Undo Last Refill?", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to revert the last refill delivery of ${lastRefill.amount} L added on ${lastRefill.timestamp}? This will deduct ${lastRefill.amount} L from current stock and refilled total.",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUndoRefillDialog = false
                        onUndoLastRefill()
                    }
                ) { Text("Undo Refill", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showUndoRefillDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ShiftInputBlock(
    shiftTitle: String,
    shiftNumber: Int,
    shift: DayShift,
    petrolColor: Color,
    dieselColor: Color,
    onShiftUpdated: (DayShift) -> Unit
) {
    var showSkipWarningDialog by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(5) }

    val hasCloseValueEntered = shift.mpd1.petrolN2.close > 0.0 || shift.mpd1.petrolN3.close > 0.0 ||
        shift.mpd1.dieselN1.close > 0.0 || shift.mpd1.dieselN4.close > 0.0 ||
        shift.mpd2.petrolN2.close > 0.0 || shift.mpd2.petrolN3.close > 0.0 ||
        shift.mpd2.dieselN1.close > 0.0 || shift.mpd2.dieselN4.close > 0.0

    LaunchedEffect(showSkipWarningDialog) {
        if (showSkipWarningDialog) {
            countdown = 5
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = shiftTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (!hasCloseValueEntered && !shift.isComplete) {
                OutlinedButton(
                    onClick = { showSkipWarningDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Shift",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Skip Shift",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        DispenserShiftCard("MPD 1", shift.mpd1, petrolColor, dieselColor) { updatedMpd1 ->
            onShiftUpdated(shift.copy(mpd1 = updatedMpd1))
        }
        DispenserShiftCard("MPD 2", shift.mpd2, petrolColor, dieselColor) { updatedMpd2 ->
            onShiftUpdated(shift.copy(mpd2 = updatedMpd2))
        }
    }

    if (showSkipWarningDialog) {
        AlertDialog(
            onDismissRequest = { showSkipWarningDialog = false },
            title = {
                Text(
                    text = "Skip Shift $shiftNumber?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "This action is used for zero-sales shifts due to holidays or pump closure.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• All nozzle CLOSE meter readings will be set equal to OPEN readings.\n• Total fuel sales for Shift $shiftNumber will evaluate to 0 Litres.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = countdown == 0,
                    onClick = {
                        showSkipWarningDialog = false

                        val skippedMpd1 = shift.mpd1.copy(
                            petrolN2 = shift.mpd1.petrolN2.copy(close = shift.mpd1.petrolN2.open),
                            petrolN3 = shift.mpd1.petrolN3.copy(close = shift.mpd1.petrolN3.open),
                            dieselN1 = shift.mpd1.dieselN1.copy(close = shift.mpd1.dieselN1.open),
                            dieselN4 = shift.mpd1.dieselN4.copy(close = shift.mpd1.dieselN4.open)
                        )
                        val skippedMpd2 = shift.mpd2.copy(
                            petrolN2 = shift.mpd2.petrolN2.copy(close = shift.mpd2.petrolN2.open),
                            petrolN3 = shift.mpd2.petrolN3.copy(close = shift.mpd2.petrolN3.open),
                            dieselN1 = shift.mpd2.dieselN1.copy(close = shift.mpd2.dieselN1.open),
                            dieselN4 = shift.mpd2.dieselN4.copy(close = shift.mpd2.dieselN4.open)
                        )

                        onShiftUpdated(
                            shift.copy(
                                mpd1 = skippedMpd1,
                                mpd2 = skippedMpd2
                            )
                        )
                    }
                ) {
                    Text(
                        text = if (countdown > 0) "Confirm ($countdown s)" else "Confirm Skip",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DispenserShiftCard(
    dispenserTitle: String,
    dispenser: DispenserShift,
    petrolColor: Color,
    dieselColor: Color,
    onUpdate: (DispenserShift) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(dispenserTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Petrol (N2, N3)", fontWeight = FontWeight.Bold, color = petrolColor, fontSize = 11.sp)
                    NozzleRow("N2", dispenser.petrolN2) { updated -> onUpdate(dispenser.copy(petrolN2 = updated)) }
                    NozzleRow("N3", dispenser.petrolN3) { updated -> onUpdate(dispenser.copy(petrolN3 = updated)) }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Diesel (N1, N4)", fontWeight = FontWeight.Bold, color = dieselColor, fontSize = 11.sp)
                    NozzleRow("N1", dispenser.dieselN1) { updated -> onUpdate(dispenser.copy(dieselN1 = updated)) }
                    NozzleRow("N4", dispenser.dieselN4) { updated -> onUpdate(dispenser.copy(dieselN4 = updated)) }
                }
            }
        }
    }
}

@Composable
fun NozzleRow(
    nozzleLabel: String,
    nozzle: NozzleShift,
    onChange: (NozzleShift) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(nozzleLabel, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
        NumberField("Open", nozzle.open, openValue = nozzle.open, modifier = Modifier.weight(1f)) { onChange(nozzle.copy(open = it)) }
        NumberField("Close", nozzle.close, openValue = nozzle.open, modifier = Modifier.weight(1f)) { onChange(nozzle.copy(close = it)) }
    }
}

@Composable
fun NumberField(
    label: String,
    value: Double,
    openValue: Double = 0.0,
    modifier: Modifier = Modifier,
    onValueChange: (Double) -> Unit
) {
    var textValue by remember(value) {
        mutableStateOf(if (value == 0.0) "" else if (value % 1.0 == 0.0) value.toLong().toString() else value.toString())
    }
    val isInvalidClose = label == "Close" && value > 0.0 && value < openValue

    OutlinedTextField(
        value = textValue,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                textValue = input
                val parsed = input.toDoubleOrNull() ?: 0.0
                onValueChange(parsed)
            }
        },
        label = { Text(label, fontSize = 8.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        isError = isInvalidClose,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = if (isInvalidClose) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (isInvalidClose) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    )
}
