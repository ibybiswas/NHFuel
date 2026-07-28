package com.nh.fuel.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.fuel.data.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    record: DailyFuelRecord,
    navBarOpacity: Float,
    onRecordChanged: (DailyFuelRecord) -> Unit,
    onDateSelected: (String) -> Unit,
    onOpacityChanged: (Float) -> Unit
) {
    var selectedMainTab by remember { mutableStateOf(0) } // 0: Home, 1: Sell, 2: Report, 3: Expend, 4: Setting
    var currentTimeString by remember { mutableStateOf("") }

    // Live Clock
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = SimpleDateFormat("EEE, dd MMM yyyy | hh:mm:ss a", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    Scaffold(
        // Let the header/nav glass sit flush against the status/nav bars
        // instead of Scaffold reserving an opaque strip behind them.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            NHFuelHeader(currentTimeString = currentTimeString)
        },
        bottomBar = {
            NHFuelBottomNav(
                selectedIndex = selectedMainTab,
                glassOpacity = navBarOpacity,
                onTabSelected = { selectedMainTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedMainTab) {
                0 -> HomeScreenContent(
                    record = record,
                    onRecordChanged = onRecordChanged,
                    onDateSelected = onDateSelected
                )
                1 -> PlaceholderTab("Sell Dashboard")
                2 -> PlaceholderTab("Reports & Analytics")
                3 -> PlaceholderTab("Expenditure Tracker")
                4 -> SettingsScreen(
                    currentOpacity = navBarOpacity,
                    onOpacityChanged = onOpacityChanged
                )
            }
        }
    }
}

/**
 * Compact liquid-glass header: station name + live clock in a single tight
 * row, with a flat translucent glass background that extends up under the
 * status bar — matching ScanApp's header (see SettingsTopBar there) instead
 * of the taller, more heavily padded default Material3 TopAppBar.
 */
@Composable
private fun NHFuelHeader(currentTimeString: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cleanGlassBackground(
                tint = MaterialTheme.colorScheme.surfaceContainer,
                opacity = HEADER_GLASS_OPACITY
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "NH FUEL STATION",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                letterSpacing = 1.1.sp,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = currentTimeString,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}

/** Opacity of the header's glass background — fixed, not user-tunable (only the bottom nav's opacity is). */
private const val HEADER_GLASS_OPACITY = 0.6f

/**
 * Floating liquid-glass bottom navigation bar, styled after ScanApp's
 * ScanAppBottomNav: a flat translucent tint at a user-controlled
 * [glassOpacity] (persisted via [NavBarPreferences]), a thin hairline
 * border, and items hand-built as a centered icon+label column so they stay
 * dead-center in the bar regardless of its height — rather than the default
 * NavigationBarItem's fixed internal padding.
 */
@Composable
private fun NHFuelBottomNav(
    selectedIndex: Int,
    glassOpacity: Float,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        Triple("Home", Icons.Default.Home, 0),
        Triple("Sell", Icons.Default.ShoppingCart, 1),
        Triple("Report", Icons.Default.Assessment, 2),
        Triple("Expend", Icons.Default.AccountBalanceWallet, 3),
        Triple("Setting", Icons.Default.Settings, 4)
    )
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(68.dp)
            .clip(shape)
            .cleanGlassBackground(
                tint = MaterialTheme.colorScheme.surfaceContainer,
                opacity = glassOpacity
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = shape
            )
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

/** One tab: icon above label, both centered in the slot it's given. */
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
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = contentColor, fontSize = 10.sp, maxLines = 1)
    }
}

/**
 * Flat translucent tint used for every "liquid glass" surface in the app
 * (the header, the bottom nav): just the theme's container color at a
 * caller-chosen alpha. [opacity] is clamped to [NavBarPreferences]'s
 * allowed range so callers can pass a raw slider value straight through.
 */
internal fun Modifier.cleanGlassBackground(tint: Color, opacity: Float): Modifier {
    val clamped = opacity.coerceIn(
        NavBarPreferences.MIN_GLASS_OPACITY,
        NavBarPreferences.MAX_GLASS_OPACITY
    )
    return this.background(tint.copy(alpha = clamped))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    record: DailyFuelRecord,
    onRecordChanged: (DailyFuelRecord) -> Unit,
    onDateSelected: (String) -> Unit
) {
    var selectedShiftTab by remember { mutableStateOf(1) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Header with Edit Pencil Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Selected Date: ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(record.date, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1565C0))
            }
            IconButton(onClick = { showDatePicker = true }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Date")
            }
        }

        if (showDatePicker) {
            var inputDate by remember { mutableStateOf(record.date) }
            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                title = { Text("Select Date (YYYY-MM-DD)") },
                text = {
                    OutlinedTextField(
                        value = inputDate,
                        onValueChange = { inputDate = it },
                        label = { Text("Date") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        if (inputDate.isNotBlank()) onDateSelected(inputDate)
                    }) {
                        Text("Load Date")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            )
        }

        // Tank Storage Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FuelTankCard(
                modifier = Modifier.weight(1f),
                title = "Petrol Tank Storage",
                color = Color(0xFFC62828),
                initialStock = record.petrolTotal,
                cumulativeRefill = record.petrolRefill,
                cumulativeShortage = record.petrolShortage,
                currentStorage = record.currentPetrolStorage,
                lastRefill = record.lastPetrolRefill,
                onInitialStockChange = { onRecordChanged(record.copy(petrolTotal = it)) },
                onAddRefill = { addedLitre ->
                    val nowStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                    onRecordChanged(
                        record.copy(
                            petrolRefill = record.petrolRefill + addedLitre,
                            lastPetrolRefill = RefillEvent(amount = addedLitre, timestamp = nowStr)
                        )
                    )
                },
                onAddShortage = { addedShortage ->
                    onRecordChanged(record.copy(petrolShortage = record.petrolShortage + addedShortage))
                },
                onUpdateLastRefill = { updatedRefill ->
                    onRecordChanged(record.copy(lastPetrolRefill = updatedRefill))
                }
            )

            FuelTankCard(
                modifier = Modifier.weight(1f),
                title = "Diesel Tank Storage",
                color = Color(0xFF1565C0),
                initialStock = record.dieselTotal,
                cumulativeRefill = record.dieselRefill,
                cumulativeShortage = record.dieselShortage,
                currentStorage = record.currentDieselStorage,
                lastRefill = record.lastDieselRefill,
                onInitialStockChange = { onRecordChanged(record.copy(dieselTotal = it)) },
                onAddRefill = { addedLitre ->
                    val nowStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                    onRecordChanged(
                        record.copy(
                            dieselRefill = record.dieselRefill + addedLitre,
                            lastDieselRefill = RefillEvent(amount = addedLitre, timestamp = nowStr)
                        )
                    )
                },
                onAddShortage = { addedShortage ->
                    onRecordChanged(record.copy(dieselShortage = record.dieselShortage + addedShortage))
                },
                onUpdateLastRefill = { updatedRefill ->
                    onRecordChanged(record.copy(lastDieselRefill = updatedRefill))
                }
            )
        }

        // Shift Selector Tabs
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
                text = { Text(if (record.shift1.isComplete) "Shift 2" else "Shift 2 🔒") }
            )
            Tab(
                selected = selectedShiftTab == 3,
                enabled = record.shift2.isComplete,
                onClick = { if (record.shift2.isComplete) selectedShiftTab = 3 },
                text = { Text(if (record.shift2.isComplete) "Shift 3" else "Shift 3 🔒") }
            )
        }

        val activeShift = when (selectedShiftTab) {
            1 -> record.shift1
            2 -> record.shift2
            else -> record.shift3
        }

        ShiftInputBlock(
            shiftTitle = "Shift $selectedShiftTab Readings",
            shift = activeShift,
            onShiftUpdated = { updatedShift ->
                val newRecord = when (selectedShiftTab) {
                    1 -> {
                        val s2Npd1P2 = if (updatedShift.npd1.petrolN2.isClosed) updatedShift.npd1.petrolN2.close else record.shift2.npd1.petrolN2.open
                        val s2Npd1P3 = if (updatedShift.npd1.petrolN3.isClosed) updatedShift.npd1.petrolN3.close else record.shift2.npd1.petrolN3.open
                        val s2Npd1D1 = if (updatedShift.npd1.dieselN1.isClosed) updatedShift.npd1.dieselN1.close else record.shift2.npd1.dieselN1.open
                        val s2Npd1D4 = if (updatedShift.npd1.dieselN4.isClosed) updatedShift.npd1.dieselN4.close else record.shift2.npd1.dieselN4.open

                        val s2Npd2P2 = if (updatedShift.npd2.petrolN2.isClosed) updatedShift.npd2.petrolN2.close else record.shift2.npd2.petrolN2.open
                        val s2Npd2P3 = if (updatedShift.npd2.petrolN3.isClosed) updatedShift.npd2.petrolN3.close else record.shift2.npd2.petrolN3.open
                        val s2Npd2D1 = if (updatedShift.npd2.dieselN1.isClosed) updatedShift.npd2.dieselN1.close else record.shift2.npd2.dieselN1.open
                        val s2Npd2D4 = if (updatedShift.npd2.dieselN4.isClosed) updatedShift.npd2.dieselN4.close else record.shift2.npd2.dieselN4.open

                        val updatedShift2 = record.shift2.copy(
                            npd1 = record.shift2.npd1.copy(
                                petrolN2 = record.shift2.npd1.petrolN2.copy(open = s2Npd1P2),
                                petrolN3 = record.shift2.npd1.petrolN3.copy(open = s2Npd1P3),
                                dieselN1 = record.shift2.npd1.dieselN1.copy(open = s2Npd1D1),
                                dieselN4 = record.shift2.npd1.dieselN4.copy(open = s2Npd1D4)
                            ),
                            npd2 = record.shift2.npd2.copy(
                                petrolN2 = record.shift2.npd2.petrolN2.copy(open = s2Npd2P2),
                                petrolN3 = record.shift2.npd2.petrolN3.copy(open = s2Npd2P3),
                                dieselN1 = record.shift2.npd2.dieselN1.copy(open = s2Npd2D1),
                                dieselN4 = record.shift2.npd2.dieselN4.copy(open = s2Npd2D4)
                            )
                        )
                        record.copy(shift1 = updatedShift, shift2 = updatedShift2)
                    }
                    2 -> {
                        val s3Npd1P2 = if (updatedShift.npd1.petrolN2.isClosed) updatedShift.npd1.petrolN2.close else record.shift3.npd1.petrolN2.open
                        val s3Npd1P3 = if (updatedShift.npd1.petrolN3.isClosed) updatedShift.npd1.petrolN3.close else record.shift3.npd1.petrolN3.open
                        val s3Npd1D1 = if (updatedShift.npd1.dieselN1.isClosed) updatedShift.npd1.dieselN1.close else record.shift3.npd1.dieselN1.open
                        val s3Npd1D4 = if (updatedShift.npd1.dieselN4.isClosed) updatedShift.npd1.dieselN4.close else record.shift3.npd1.dieselN4.open

                        val s3Npd2P2 = if (updatedShift.npd2.petrolN2.isClosed) updatedShift.npd2.petrolN2.close else record.shift3.npd2.petrolN2.open
                        val s3Npd2P3 = if (updatedShift.npd2.petrolN3.isClosed) updatedShift.npd2.petrolN3.close else record.shift3.npd2.petrolN3.open
                        val s3Npd2D1 = if (updatedShift.npd2.dieselN1.isClosed) updatedShift.npd2.dieselN1.close else record.shift3.npd2.dieselN1.open
                        val s3Npd2D4 = if (updatedShift.npd2.dieselN4.isClosed) updatedShift.npd2.dieselN4.close else record.shift3.npd2.dieselN4.open

                        val updatedShift3 = record.shift3.copy(
                            npd1 = record.shift3.npd1.copy(
                                petrolN2 = record.shift3.npd1.petrolN2.copy(open = s3Npd1P2),
                                petrolN3 = record.shift3.npd1.petrolN3.copy(open = s3Npd1P3),
                                dieselN1 = record.shift3.npd1.dieselN1.copy(open = s3Npd1D1),
                                dieselN4 = record.shift3.npd1.dieselN4.copy(open = s3Npd1D4)
                            ),
                            npd2 = record.shift3.npd2.copy(
                                petrolN2 = record.shift3.npd2.petrolN2.copy(open = s3Npd2P2),
                                petrolN3 = record.shift3.npd2.petrolN3.copy(open = s3Npd2P3),
                                dieselN1 = record.shift3.npd2.dieselN1.copy(open = s3Npd2D1),
                                dieselN4 = record.shift3.npd2.dieselN4.copy(open = s3Npd2D4)
                            )
                        )
                        record.copy(shift2 = updatedShift, shift3 = updatedShift3)
                    }
                    else -> record.copy(shift3 = updatedShift)
                }
                onRecordChanged(newRecord)
            }
        )

        // Shift Sales Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Shift Sales Breakdown:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shift 1", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("• Petrol: ${record.shift1.petrolSale} L", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 10.sp)
                        Text("• Diesel: ${record.shift1.dieselSale} L", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 10.sp)
                    }

                    if (record.shift1.isComplete || record.shift2.petrolSale > 0.0 || record.shift2.dieselSale > 0.0) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shift 2", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("• Petrol: ${record.shift2.petrolSale} L", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 10.sp)
                            Text("• Diesel: ${record.shift2.dieselSale} L", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 10.sp)
                        }
                    }

                    if (record.shift2.isComplete || record.shift3.petrolSale > 0.0 || record.shift3.dieselSale > 0.0) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shift 3", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("• Petrol: ${record.shift3.petrolSale} L", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 10.sp)
                            Text("• Diesel: ${record.shift3.dieselSale} L", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 10.sp)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Total 24H Full Day Sales (Shift 1 + 2 + 3):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("• Total Petrol Sold: ${record.totalPetrolSell} Litre", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C), fontSize = 13.sp)
                Text("• Total Diesel Sold: ${record.totalDieselSell} Litre", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun PlaceholderTab(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray)
    }
}

@Composable
fun FuelTankCard(
    modifier: Modifier = Modifier,
    title: String,
    color: Color,
    initialStock: Double,
    cumulativeRefill: Double,
    cumulativeShortage: Double,
    currentStorage: Double,
    lastRefill: RefillEvent,
    onInitialStockChange: (Double) -> Unit,
    onAddRefill: (Double) -> Unit,
    onAddShortage: (Double) -> Unit,
    onUpdateLastRefill: (RefillEvent) -> Unit
) {
    var newRefillInput by remember { mutableStateOf("") }
    var newShortageInput by remember { mutableStateOf("") }
    var showEditLastRefillDialog by remember { mutableStateOf(false) }

    Card(modifier = modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp)

            NumberField("Base Stock", initialStock) { onInitialStockChange(it) }

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
            Text("Total Refilled: $cumulativeRefill L", fontSize = 10.sp, color = Color(0xFF2E7D32))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = newShortageInput,
                    onValueChange = { newShortageInput = it },
                    label = { Text("Add Shortage (-)", fontSize = 8.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val added = newShortageInput.toDoubleOrNull() ?: 0.0
                        if (added > 0.0) {
                            onAddShortage(added)
                            newShortageInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) { Text("-", fontSize = 12.sp) }
            }
            Text("Total Shortage: $cumulativeShortage L", fontSize = 10.sp, color = Color(0xFFC62828))

            Divider(modifier = Modifier.padding(vertical = 2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Last Refill:", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Text(if (lastRefill.timestamp.isNotBlank()) "${lastRefill.amount} L @ ${lastRefill.timestamp}" else "None", fontSize = 9.sp)
                }
                IconButton(onClick = { showEditLastRefillDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Last Refill", modifier = Modifier.size(14.dp))
                }
            }

            Divider(modifier = Modifier.padding(vertical = 2.dp))

            Column {
                Text("Current Stock:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(
                    text = "$currentStorage L",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32),
                    fontSize = 22.sp
                )
            }
        }
    }

    if (showEditLastRefillDialog) {
        var editAmount by remember { mutableStateOf(lastRefill.amount.toString()) }
        var editTime by remember { mutableStateOf(lastRefill.timestamp) }

        AlertDialog(
            onDismissRequest = { showEditLastRefillDialog = false },
            title = { Text("Edit Last Refill Details", fontSize = 14.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editAmount, onValueChange = { editAmount = it }, label = { Text("Refill Litre") })
                    OutlinedTextField(value = editTime, onValueChange = { editTime = it }, label = { Text("Date & Time") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEditLastRefillDialog = false
                    onUpdateLastRefill(RefillEvent(amount = editAmount.toDoubleOrNull() ?: 0.0, timestamp = editTime))
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditLastRefillDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ShiftInputBlock(
    shiftTitle: String,
    shift: DayShift,
    onShiftUpdated: (DayShift) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(shiftTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        DispenserShiftCard("NPD 1", shift.npd1) { updatedNpd1 ->
            onShiftUpdated(shift.copy(npd1 = updatedNpd1))
        }

        DispenserShiftCard("NPD 2", shift.npd2) { updatedNpd2 ->
            onShiftUpdated(shift.copy(npd2 = updatedNpd2))
        }
    }
}

@Composable
fun DispenserShiftCard(
    dispenserTitle: String,
    dispenser: DispenserShift,
    onUpdate: (DispenserShift) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(dispenserTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Petrol (N2, N3)", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 11.sp)
                    NozzleRow("N2", dispenser.petrolN2) { updated -> onUpdate(dispenser.copy(petrolN2 = updated)) }
                    NozzleRow("N3", dispenser.petrolN3) { updated -> onUpdate(dispenser.copy(petrolN3 = updated)) }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Diesel (N1, N4)", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 11.sp)
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
        Text(nozzleLabel, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        NumberField("Open", nozzle.open, modifier = Modifier.weight(1f)) { onChange(nozzle.copy(open = it)) }
        NumberField("Close", nozzle.close, modifier = Modifier.weight(1f)) { onChange(nozzle.copy(close = it)) }
    }
}

@Composable
fun NumberField(
    label: String,
    value: Double,
    modifier: Modifier = Modifier,
    onValueChange: (Double) -> Unit
) {
    var textValue by remember(value) {
        mutableStateOf(if (value == 0.0) "" else if (value % 1.0 == 0.0) value.toLong().toString() else value.toString())
    }

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
        modifier = modifier
    )
}
