package com.nh.fuel.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.fuel.data.DailyFuelRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    record: DailyFuelRecord,
    onRecordChanged: (DailyFuelRecord) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NH FUEL STATION", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = { }, label = { Text("Home") }, icon = {})
                NavigationBarItem(selected = false, onClick = { }, label = { Text("Sell") }, icon = {})
                NavigationBarItem(selected = false, onClick = { }, label = { Text("Report") }, icon = {})
                NavigationBarItem(selected = false, onClick = { }, label = { Text("Expend") }, icon = {})
                NavigationBarItem(selected = false, onClick = { }, label = { Text("Setting") }, icon = {})
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Date: ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = record.date,
                    onValueChange = { onRecordChanged(record.copy(date = it)) },
                    modifier = Modifier.width(160.dp),
                    singleLine = true
                )
            }

            // Storage Row (Petrol & Diesel)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Petrol Storage Card
                Card(modifier = Modifier.weight(1f).border(1.dp, Color.Gray, RoundedCornerShape(8.dp))) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Petrol Storage", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        NumberField("Total Litre", record.petrolTotal) { onRecordChanged(record.copy(petrolTotal = it)) }
                        NumberField("Refill (+)", record.petrolRefill) { onRecordChanged(record.copy(petrolRefill = it)) }
                        NumberField("Shortage (-)", record.petrolShortage) { onRecordChanged(record.copy(petrolShortage = it)) }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Current: ${record.currentPetrolStorage} L", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Diesel Storage Card
                Card(modifier = Modifier.weight(1f).border(1.dp, Color.Gray, RoundedCornerShape(8.dp))) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Diesel Storage", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        NumberField("Total Litre", record.dieselTotal) { onRecordChanged(record.copy(dieselTotal = it)) }
                        NumberField("Refill (+)", record.dieselRefill) { onRecordChanged(record.copy(dieselRefill = it)) }
                        NumberField("Shortage (-)", record.dieselShortage) { onRecordChanged(record.copy(dieselShortage = it)) }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Current: ${record.currentDieselStorage} L", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // NPD 1 Block
            NpdCard(
                title = "NPD 1",
                petrolN2Open = record.npd1PetrolN2Open, petrolN2Close = record.npd1PetrolN2Close,
                petrolN3Open = record.npd1PetrolN3Open, petrolN3Close = record.npd1PetrolN3Close,
                dieselN1Open = record.npd1DieselN1Open, dieselN1Close = record.npd1DieselN1Close,
                dieselN4Open = record.npd1DieselN4Open, dieselN4Close = record.npd1DieselN4Close,
                onUpdate = { p2O, p2C, p3O, p3C, d1O, d1C, d4O, d4C ->
                    onRecordChanged(record.copy(
                        npd1PetrolN2Open = p2O, npd1PetrolN2Close = p2C,
                        npd1PetrolN3Open = p3O, npd1PetrolN3Close = p3C,
                        npd1DieselN1Open = d1O, npd1DieselN1Close = d1C,
                        npd1DieselN4Open = d4O, npd1DieselN4Close = d4C
                    ))
                }
            )

            // NPD 2 Block
            NpdCard(
                title = "NPD 2",
                petrolN2Open = record.npd2PetrolN2Open, petrolN2Close = record.npd2PetrolN2Close,
                petrolN3Open = record.npd2PetrolN3Open, petrolN3Close = record.npd2PetrolN3Close,
                dieselN1Open = record.npd2DieselN1Open, dieselN1Close = record.npd2DieselN1Close,
                dieselN4Open = record.npd2DieselN4Open, dieselN4Close = record.npd2DieselN4Close,
                onUpdate = { p2O, p2C, p3O, p3C, d1O, d1C, d4O, d4C ->
                    onRecordChanged(record.copy(
                        npd2PetrolN2Open = p2O, npd2PetrolN2Close = p2C,
                        npd2PetrolN3Open = p3O, npd2PetrolN3Close = p3C,
                        npd2DieselN1Open = d1O, npd2DieselN1Close = d1C,
                        npd2DieselN4Open = d4O, npd2DieselN4Close = d4C
                    ))
                }
            )

            // Summary Totals
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Total Petrol Sell (NPD1+NPD2): ${record.totalPetrolSell} Litre",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFB71C1C)
                    )
                    Text(
                        "Total Diesel Sell (NPD1+NPD2): ${record.totalDieselSell} Litre",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0D47A1)
                    )
                }
            }
        }
    }
}

@Composable
fun NpdCard(
    title: String,
    petrolN2Open: Double, petrolN2Close: Double,
    petrolN3Open: Double, petrolN3Close: Double,
    dieselN1Open: Double, dieselN1Close: Double,
    dieselN4Open: Double, dieselN4Close: Double,
    onUpdate: (Double, Double, Double, Double, Double, Double, Double, Double) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray, RoundedCornerShape(8.dp))) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Petrol Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("Petrol", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 13.sp)
                    NozzleRow("N2", petrolN2Open, petrolN2Close) { o, c -> onUpdate(o, c, petrolN3Open, petrolN3Close, dieselN1Open, dieselN1Close, dieselN4Open, dieselN4Close) }
                    NozzleRow("N3", petrolN3Open, petrolN3Close) { o, c -> onUpdate(petrolN2Open, petrolN2Close, o, c, dieselN1Open, dieselN1Close, dieselN4Open, dieselN4Close) }
                }
                // Diesel Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("Diesel", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), fontSize = 13.sp)
                    NozzleRow("N1", dieselN1Open, dieselN1Close) { o, c -> onUpdate(petrolN2Open, petrolN2Close, petrolN3Open, petrolN3Close, o, c, dieselN4Open, dieselN4Close) }
                    NozzleRow("N4", dieselN4Open, dieselN4Close) { o, c -> onUpdate(petrolN2Open, petrolN2Close, petrolN3Open, petrolN3Close, dieselN1Open, dieselN1Close, o, c) }
                }
            }
        }
    }
}

@Composable
fun NozzleRow(
    nozzleLabel: String,
    openVal: Double,
    closeVal: Double,
    onChange: (Double, Double) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(nozzleLabel, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        NumberField("Open", openVal, modifier = Modifier.weight(1f)) { onChange(it, closeVal) }
        NumberField("Close", closeVal, modifier = Modifier.weight(1f)) { onChange(openVal, it) }
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
        label = { Text(label, fontSize = 9.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}
