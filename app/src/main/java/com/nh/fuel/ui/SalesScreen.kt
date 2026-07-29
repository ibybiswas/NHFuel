package com.nh.fuel.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.fuel.data.DailyFuelRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PeriodFilter { DAY, WEEK, MONTH, YEAR, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    currentRecord: DailyFuelRecord,
    allRecords: List<DailyFuelRecord> = listOf(currentRecord),
    onRecordChanged: (DailyFuelRecord) -> Unit,
    onDateSelected: (String) -> Unit = {},
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val petrolColor = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
    val dieselColor = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)

    var selectedPeriod by remember { mutableStateOf(PeriodFilter.DAY) }
    var showDatePicker by remember { mutableStateOf(false) }

    var fromDateInput by remember { mutableStateOf(currentRecord.date) }
    var toDateInput by remember { mutableStateOf(currentRecord.date) }

    // Fixed cursor jumping: local string state initialized once per record date
    var petrolPriceText by remember(currentRecord.date) { 
        mutableStateOf(if (currentRecord.petrolPrice == 0.0) "" else currentRecord.petrolPrice.toString()) 
    }
    var dieselPriceText by remember(currentRecord.date) { 
        mutableStateOf(if (currentRecord.dieselPrice == 0.0) "" else currentRecord.dieselPrice.toString()) 
    }

    // Filter records based on active period tab
    val filteredRecords = remember(selectedPeriod, currentRecord, fromDateInput, toDateInput, allRecords) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val records = if (allRecords.none { it.date == currentRecord.date }) {
            allRecords + currentRecord
        } else allRecords

        val targetDate = try { sdf.parse(currentRecord.date) ?: Date() } catch (e: Exception) { Date() }
        val cal = Calendar.getInstance().apply { time = targetDate }

        when (selectedPeriod) {
            PeriodFilter.DAY -> records.filter { it.date == currentRecord.date }
            PeriodFilter.WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val startDate = cal.time
                records.filter { rec ->
                    val recDate = try { sdf.parse(rec.date) } catch (e: Exception) { null }
                    recDate != null && !recDate.before(startDate) && !recDate.after(targetDate)
                }
            }
            PeriodFilter.MONTH -> {
                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(targetDate)
                records.filter { it.date.startsWith(currentMonth) }
            }
            PeriodFilter.YEAR -> {
                val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(targetDate)
                records.filter { it.date.startsWith(currentYear) }
            }
            PeriodFilter.CUSTOM -> {
                val fromD = try { sdf.parse(fromDateInput) } catch (e: Exception) { null }
                val toD = try { sdf.parse(toDateInput) } catch (e: Exception) { null }
                if (fromD != null && toD != null) {
                    records.filter { rec ->
                        val d = try { sdf.parse(rec.date) } catch (e: Exception) { null }
                        d != null && !d.before(fromD) && !d.after(toD)
                    }
                } else records.filter { it.date == currentRecord.date }
            }
        }
    }

    val totalPetrolLitre = filteredRecords.sumOf { it.totalPetrolSell }
    val totalDieselLitre = filteredRecords.sumOf { it.totalDieselSell }
    val totalPetrolRevenue = filteredRecords.sumOf { it.totalPetrolRevenue }
    val totalDieselRevenue = filteredRecords.sumOf { it.totalDieselRevenue }
    val grandTotalRevenue = totalPetrolRevenue + totalDieselRevenue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(topInset + 4.dp))

        Text(
            text = "Sales & Revenue Dashboard",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Date selection header for DAY tab
        if (selectedPeriod == PeriodFilter.DAY) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Viewing Date: ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = currentRecord.date,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (showDatePicker) {
                var inputDate by remember { mutableStateOf(currentRecord.date) }
                AlertDialog(
                    onDismissRequest = { showDatePicker = false },
                    title = { Text("Select Date (YYYY-MM-DD)", color = MaterialTheme.colorScheme.onSurface) },
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
                        }) { Text("Load Date") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }
                )
            }
        }

        // Fuel Price Input Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Fuel Rates (₹ / Litre)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = petrolPriceText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                petrolPriceText = input
                                val valParsed = input.toDoubleOrNull() ?: 0.0
                                onRecordChanged(currentRecord.copy(petrolPrice = valParsed))
                            }
                        },
                        label = { Text("Petrol Price (₹)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = dieselPriceText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                dieselPriceText = input
                                val valParsed = input.toDoubleOrNull() ?: 0.0
                                onRecordChanged(currentRecord.copy(dieselPrice = valParsed))
                            }
                        },
                        label = { Text("Diesel Price (₹)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Period Filter Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedPeriod.ordinal,
            edgePadding = 0.dp
        ) {
            PeriodFilter.values().forEach { period ->
                Tab(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    text = { Text(period.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Custom Date Range Inputs
        if (selectedPeriod == PeriodFilter.CUSTOM) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fromDateInput,
                    onValueChange = { fromDateInput = it },
                    label = { Text("From Date (YYYY-MM-DD)", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = toDateInput,
                    onValueChange = { toDateInput = it },
                    label = { Text("To Date (YYYY-MM-DD)", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Shift Breakdown Cards for single Day view
        if (selectedPeriod == PeriodFilter.DAY) {
            Text(
                text = "Shift Breakdown (${currentRecord.date}):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ShiftSalesCard(
                    shiftTitle = "Shift 1",
                    petrolLitre = currentRecord.shift1.petrolSale,
                    petrolRupees = currentRecord.shift1PetrolRevenue,
                    dieselLitre = currentRecord.shift1.dieselSale,
                    dieselRupees = currentRecord.shift1DieselRevenue,
                    totalRupees = currentRecord.shift1TotalRevenue,
                    petrolColor = petrolColor,
                    dieselColor = dieselColor,
                    modifier = Modifier.weight(1f)
                )

                ShiftSalesCard(
                    shiftTitle = "Shift 2",
                    petrolLitre = currentRecord.shift2.petrolSale,
                    petrolRupees = currentRecord.shift2PetrolRevenue,
                    dieselLitre = currentRecord.shift2.dieselSale,
                    dieselRupees = currentRecord.shift2DieselRevenue,
                    totalRupees = currentRecord.shift2TotalRevenue,
                    petrolColor = petrolColor,
                    dieselColor = dieselColor,
                    modifier = Modifier.weight(1f)
                )

                ShiftSalesCard(
                    shiftTitle = "Shift 3",
                    petrolLitre = currentRecord.shift3.petrolSale,
                    petrolRupees = currentRecord.shift3PetrolRevenue,
                    dieselLitre = currentRecord.shift3.dieselSale,
                    dieselRupees = currentRecord.shift3DieselRevenue,
                    totalRupees = currentRecord.shift3TotalRevenue,
                    petrolColor = petrolColor,
                    dieselColor = dieselColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Total Sales Summary (${selectedPeriod.name}):",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• Petrol Sold: $totalPetrolLitre Litres → ₹ ${String.format(Locale.getDefault(), "%.2f", totalPetrolRevenue)}",
                    fontWeight = FontWeight.Bold,
                    color = petrolColor,
                    fontSize = 12.sp
                )
                Text(
                    text = "• Diesel Sold: $totalDieselLitre Litres → ₹ ${String.format(Locale.getDefault(), "%.2f", totalDieselRevenue)}",
                    fontWeight = FontWeight.Bold,
                    color = dieselColor,
                    fontSize = 12.sp
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    text = "Grand Total Revenue: ₹ ${String.format(Locale.getDefault(), "%.2f", grandTotalRevenue)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // CSV / Spreadsheet Exporter
        Button(
            onClick = { exportSalesToCSV(context, filteredRecords) },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Export Sales Report (.CSV / Excel)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(Modifier.height(bottomInset + 4.dp))
    }
}

@Composable
private fun ShiftSalesCard(
    shiftTitle: String,
    petrolLitre: Double,
    petrolRupees: Double,
    dieselLitre: Double,
    dieselRupees: Double,
    totalRupees: Double,
    petrolColor: Color,
    dieselColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(shiftTitle, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)

            Text("Petrol:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = petrolColor)
            Text("${petrolLitre}L", fontSize = 9.sp)
            Text("₹ ${String.format(Locale.getDefault(), "%.1f", petrolRupees)}", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = petrolColor)

            HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))

            Text("Diesel:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = dieselColor)
            Text("${dieselLitre}L", fontSize = 9.sp)
            Text("₹ ${String.format(Locale.getDefault(), "%.1f", dieselRupees)}", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = dieselColor)

            HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))

            Text("Total ₹:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
            Text("₹ ${String.format(Locale.getDefault(), "%.1f", totalRupees)}", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
        }
    }
}

private fun exportSalesToCSV(context: Context, records: List<DailyFuelRecord>) {
    val csvHeader = "Date,Petrol Sold (L),Petrol Rate (Rs),Petrol Revenue (Rs),Diesel Sold (L),Diesel Rate (Rs),Diesel Revenue (Rs),Total Revenue (Rs)\n"
    val csvBody = StringBuilder()

    records.forEach { record ->
        csvBody.append(
            "${record.date},${record.totalPetrolSell},${record.petrolPrice},${record.totalPetrolRevenue},${record.totalDieselSell},${record.dieselPrice},${record.totalDieselRevenue},${record.grandTotalRevenue}\n"
        )
    }

    try {
        val fileName = "NHFuel_Sales_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(csvHeader + csvBody.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "NH Fuel Sales Report")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Sales Report CSV"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
