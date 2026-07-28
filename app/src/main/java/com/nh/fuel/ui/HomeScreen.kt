package com.nh.fuel.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun HomeScreen(
    record: DailyFuelRecord,
    onRecordChanged: (DailyFuelRecord) -> Unit,
    onDateSelected: (String) -> Unit
) {
    var selectedShiftTab by remember { mutableStateOf(1) }
    var currentTimeString by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Live Clock Engine
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = SimpleDateFormat("EEE, dd MMM yyyy | hh:mm:ss a", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NH FUEL STATION", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(currentTimeString, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, label = { Text("Home") }, icon = {})
                NavigationBarItem(selected = false, onClick = {}, label = { Text("Sell") }, icon = {})
                NavigationBarItem(selected = false, onClick = {}, label = { Text("Report") }, icon = {})
                NavigationBarItem(selected = false, onClick = {}, label = { Text("Expend") }, icon = {})
                NavigationBarItem(selected = false, onClick = {}, label = { Text("Setting") }, icon = {})
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Date / Go Back to Older Records")
                }
            }

            // Date Selection Dialog
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

            // Tank Storage Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Petrol Tank Card
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

                // Diesel Tank Card
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

            // Shift Selector Tabs (Shift 1, Shift 2, Shift 3)
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

            // Active Shift View
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

            // Shift Sales Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Shift $selectedShiftTab Sales:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• Petrol Sold (NPD1 + NPD2): ${activeShift.petrolSale} L", color = Color(0xFFC62828), fontSize = 12.sp)
                    Text("• Diesel Sold (NPD1 + NPD2): ${activeShift.dieselSale} L", color = Color(0xFF1565C0), fontSize = 12.sp)

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Total 24H Full Day Sales (Shift 1 + 2 + 3):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• Total Petrol Sell: ${record.totalPetrolSell} Litre", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C), fontSize = 13.sp)
                    Text("• Total Diesel Sell: ${record.totalDieselSell} Litre", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1), fontSize = 13.sp)
                }
            }
        }
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

            // Cumulative Refill Input
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

            // Cumulative Shortage Input
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

            // Last Refill Timestamp & Edit Option
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Last Refill:", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Text(if (lastRefill.timestamp.isNotBlank()) "${lastRefill.amount} L @ ${lastRefill.timestamp}" else "None", fontSize = 9.sp)
                }
                IconButton(onClick = { showEditLastRefillDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Last Refill", modifier = Modifier.size(14.dp))
                }
            }

            Divider(modifier = Modifier.padding(vertical = 2.dp))

            Text("Current Stock: $currentStorage L", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }

    // Dialog to Edit Last Refill
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
                // Petrol Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("Petrol (N2, N3)", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 11.sp)
                    NozzleRow("N2", dispenser.petrolN2) { updated -> onUpdate(dispenser.copy(petrolN2 = updated)) }
                    NozzleRow("N3", dispenser.petrolN3) { updated -> onUpdate(dispenser.copy(petrolN3 = updated)) }
                }

                // Diesel Column
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
    OutlinedTextField(
        value = if (value == 0.0) "" else value.toString(),
        onValueChange = { onValueChange(it.toDoubleOrNull() ?: 0.0) },
        label = { Text(label, fontSize = 8.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}
