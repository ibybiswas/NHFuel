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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.nh.fuel.data.DayShift
import com.nh.fuel.data.DispenserShift
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

    var petrolPriceText by remember(currentRecord.date) { 
        mutableStateOf(if (currentRecord.petrolPrice == 0.0) "" else currentRecord.petrolPrice.toString()) 
    }
    var dieselPriceText by remember(currentRecord.date) { 
        mutableStateOf(if (currentRecord.dieselPrice == 0.0) "" else currentRecord.dieselPrice.toString()) 
    }

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

    val totalCashCollected = filteredRecords.sumOf { it.dailyCashCollected }
    val totalDigitalCollected = filteredRecords.sumOf { it.dailyDigitalCollected }
    val totalCollected = filteredRecords.sumOf { it.dailyTotalCollected }
    val totalMismatch = filteredRecords.sumOf { it.dailyMismatch }

    fun navigateDate(daysOffset: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = try { sdf.parse(currentRecord.date) ?: Date() } catch (e: Exception) { Date() }
        val cal = Calendar.getInstance().apply {
            time = parsedDate
            add(Calendar.DAY_OF_MONTH, daysOffset)
        }
        onDateSelected(sdf.format(cal.time))
    }

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

        if (selectedPeriod == PeriodFilter.DAY) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { navigateDate(-1) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", modifier = Modifier.size(16.dp))
                    Text("Prev Day", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentRecord.date,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { navigateDate(1) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Next Day", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", modifier = Modifier.size(16.dp))
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

        if (selectedPeriod == PeriodFilter.DAY) {
            Text(
                text = "Shift Breakdown & Payment Collections (${currentRecord.date}):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            ShiftDetailedSalesBlock(
                shiftTitle = "Shift 1",
                shift = currentRecord.shift1,
                petrolPrice = currentRecord.petrolPrice,
                dieselPrice = currentRecord.dieselPrice,
                petrolColor = petrolColor,
                dieselColor = dieselColor,
                onShiftUpdated = { updatedShift ->
                    onRecordChanged(currentRecord.copy(shift1 = updatedShift))
                }
            )

            ShiftDetailedSalesBlock(
                shiftTitle = "Shift 2",
                shift = currentRecord.shift2,
                petrolPrice = currentRecord.petrolPrice,
                dieselPrice = currentRecord.dieselPrice,
                petrolColor = petrolColor,
                dieselColor = dieselColor,
                onShiftUpdated = { updatedShift ->
                    onRecordChanged(currentRecord.copy(shift2 = updatedShift))
                }
            )

            ShiftDetailedSalesBlock(
                shiftTitle = "Shift 3",
                shift = currentRecord.shift3,
                petrolPrice = currentRecord.petrolPrice,
                dieselPrice = currentRecord.dieselPrice,
                petrolColor = petrolColor,
                dieselColor = dieselColor,
                onShiftUpdated = { updatedShift ->
                    onRecordChanged(currentRecord.copy(shift3 = updatedShift))
                }
            )
        }

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
                    text = "• Petrol Sold: $totalPetrolLitre L → ₹ ${formatCurrency(totalPetrolRevenue)}",
                    fontWeight = FontWeight.Bold,
                    color = petrolColor,
                    fontSize = 12.sp
                )
                Text(
                    text = "• Diesel Sold: $totalDieselLitre L → ₹ ${formatCurrency(totalDieselRevenue)}",
                    fontWeight = FontWeight.Bold,
                    color = dieselColor,
                    fontSize = 12.sp
                )
                Text(
                    text = "• Grand Calculated Revenue: ₹ ${formatCurrency(grandTotalRevenue)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    text = "• Total Cash Collected: ₹ ${formatCurrency(totalCashCollected)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Total Digital Collected: ₹ ${formatCurrency(totalDigitalCollected)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Total Payment Collected: ₹ ${formatCurrency(totalCollected)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                val mismatchColor = when {
                    totalMismatch > 0.0 -> Color(0xFF2E7D32)
                    totalMismatch < 0.0 -> Color(0xFFC62828)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    text = "Net Mismatch: ${formatSignedCurrency(totalMismatch)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = mismatchColor
                )
            }
        }

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
private fun ShiftDetailedSalesBlock(
    shiftTitle: String,
    shift: DayShift,
    petrolPrice: Double,
    dieselPrice: Double,
    petrolColor: Color,
    dieselColor: Color,
    onShiftUpdated: (DayShift) -> Unit
) {
    val shiftRevenue = shift.getRevenue(petrolPrice, dieselPrice)
    val shiftMismatch = shift.getMismatch(petrolPrice, dieselPrice)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(shiftTitle, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "Shift Revenue: ₹ ${formatCurrency(shiftRevenue)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MpdSalesColumn(
                    mpdTitle = "MPD 1",
                    dispenser = shift.mpd1,
                    petrolPrice = petrolPrice,
                    dieselPrice = dieselPrice,
                    petrolColor = petrolColor,
                    dieselColor = dieselColor,
                    modifier = Modifier.weight(1f),
                    onDispenserUpdated = { updatedMpd1 ->
                        onShiftUpdated(shift.copy(mpd1 = updatedMpd1))
                    }
                )

                MpdSalesColumn(
                    mpdTitle = "MPD 2",
                    dispenser = shift.mpd2,
                    petrolPrice = petrolPrice,
                    dieselPrice = dieselPrice,
                    petrolColor = petrolColor,
                    dieselColor = dieselColor,
                    modifier = Modifier.weight(1f),
                    onDispenserUpdated = { updatedMpd2 ->
                        onShiftUpdated(shift.copy(mpd2 = updatedMpd2))
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)

            val mismatchColor = when {
                shiftMismatch > 0.0 -> Color(0xFF2E7D32)
                shiftMismatch < 0.0 -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.onSurface
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shift Collected: ₹ ${formatCurrency(shift.totalCollected)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Shift Mismatch: ${formatSignedCurrency(shiftMismatch)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = mismatchColor
                )
            }
        }
    }
}

@Composable
private fun MpdSalesColumn(
    mpdTitle: String,
    dispenser: DispenserShift,
    petrolPrice: Double,
    dieselPrice: Double,
    petrolColor: Color,
    dieselColor: Color,
    modifier: Modifier = Modifier,
    onDispenserUpdated: (DispenserShift) -> Unit
) {
    val petrolRev = dispenser.petrolSale * petrolPrice
    val dieselRev = dispenser.dieselSale * dieselPrice
    val mpdRevenue = petrolRev + dieselRev
    val mpdMismatch = dispenser.getMismatch(petrolPrice, dieselPrice)

    var cashText by remember(dispenser.cashCollected) {
        mutableStateOf(if (dispenser.cashCollected == 0.0) "" else dispenser.cashCollected.toString())
    }
    var digitalText by remember(dispenser.digitalCollected) {
        mutableStateOf(if (dispenser.digitalCollected == 0.0) "" else dispenser.digitalCollected.toString())
    }

    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(mpdTitle, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)

            Text("Petrol (${dispenser.petrolSale} L): ₹ ${formatCurrency(petrolRev)}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = petrolColor)
            Text("Diesel (${dispenser.dieselSale} L): ₹ ${formatCurrency(dieselRev)}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = dieselColor)
            Text("MPD Revenue: ₹ ${formatCurrency(mpdRevenue)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))

            OutlinedTextField(
                value = cashText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        cashText = input
                        val parsed = input.toDoubleOrNull() ?: 0.0
                        onDispenserUpdated(dispenser.copy(cashCollected = parsed))
                    }
                },
                label = { Text("Cash ₹", fontSize = 8.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = digitalText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        digitalText = input
                        val parsed = input.toDoubleOrNull() ?: 0.0
                        onDispenserUpdated(dispenser.copy(digitalCollected = parsed))
                    }
                },
                label = { Text("Digital ₹", fontSize = 8.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val mismatchColor = when {
                mpdMismatch > 0.0 -> Color(0xFF2E7D32)
                mpdMismatch < 0.0 -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.onSurface
            }

            Text(
                text = "Collected: ₹ ${formatCurrency(dispenser.totalCollected)}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Mismatch: ${formatSignedCurrency(mpdMismatch)}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = mismatchColor
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return String.format(Locale.getDefault(), "%.2f", amount)
}

private fun formatSignedCurrency(amount: Double): String {
    val sign = if (amount > 0.0) "+" else ""
    return "$sign₹ ${String.format(Locale.getDefault(), "%.2f", amount)}"
}

/** Fixed CSV Export: Prepends UTF-8 BOM (\uFEFF) and outputs raw decimal values for mismatch */
private fun exportSalesToCSV(context: Context, records: List<DailyFuelRecord>) {
    val utf8Bom = "\uFEFF"
    val csvHeader = "Date,Petrol Sold (L),Petrol Rate (Rs),Petrol Revenue (Rs),Diesel Sold (L),Diesel Rate (Rs),Diesel Revenue (Rs),Grand Total Revenue (Rs),Cash Collected (Rs),Digital Collected (Rs),Total Payment Collected (Rs),Total Mismatch (Rs)\n"
    val csvBody = StringBuilder()

    records.forEach { record ->
        val rawMismatch = String.format(Locale.US, "%.2f", record.dailyMismatch)
        csvBody.append(
            "${record.date},${record.totalPetrolSell},${record.petrolPrice},${formatCurrency(record.totalPetrolRevenue)},${record.totalDieselSell},${record.dieselPrice},${formatCurrency(record.totalDieselRevenue)},${formatCurrency(record.grandTotalRevenue)},${formatCurrency(record.dailyCashCollected)},${formatCurrency(record.dailyDigitalCollected)},${formatCurrency(record.dailyTotalCollected)},${rawMismatch}\n"
        )
    }

    try {
        val fileName = "NHFuel_Sales_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(utf8Bom + csvHeader + csvBody.toString(), Charsets.UTF_8)

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
