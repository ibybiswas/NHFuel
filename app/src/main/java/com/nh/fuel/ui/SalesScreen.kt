package com.nh.fuel.ui

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.Lock
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
import androidx.core.content.FileProvider
import com.nh.fuel.data.ActivityLogger
import com.nh.fuel.data.AppUserSession
import com.nh.fuel.data.DailyFuelRecord
import com.nh.fuel.data.DayShift
import com.nh.fuel.data.DispenserShift
import com.nh.fuel.data.ExpenseItem
import com.nh.fuel.data.Role
import com.nh.fuel.data.round2
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PeriodFilter { DAY, WEEK, MONTH, YEAR, CUSTOM }
enum class ExportFormat { XLS, CSV, PDF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    session: AppUserSession,
    currentRecord: DailyFuelRecord,
    allRecords: List<DailyFuelRecord> = listOf(currentRecord),
    allExpenses: List<ExpenseItem> = emptyList(),
    onRecordChanged: (DailyFuelRecord) -> Unit,
    onDateSelected: (String) -> Unit = {},
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val petrolColor = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
    val dieselColor = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isPastDate = currentRecord.date < todayStr
    val isDayFinalized = currentRecord.shift3.isComplete

    val isAdminOrOwner = session.isOwnerLogin || session.role == Role.SUPER_ADMIN || session.role == Role.ADMIN

    val canEdit = if (isAdminOrOwner) {
        true
    } else {
        !session.isReadOnly && (!isDayFinalized || (!isPastDate || session.canEditPastDates))
    }

    var selectedPeriod by remember { mutableStateOf(PeriodFilter.DAY) }
    var showDatePickerModal by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var fromDateInput by remember { mutableStateOf(currentRecord.date) }
    var toDateInput by remember { mutableStateOf(currentRecord.date) }

    // Persistent rate input text states
    var petrolPriceText by remember(currentRecord.date, currentRecord.petrolPrice) {
        mutableStateOf(if (currentRecord.petrolPrice == 0.0) "" else if (currentRecord.petrolPrice % 1.0 == 0.0) currentRecord.petrolPrice.toLong().toString() else currentRecord.petrolPrice.toString())
    }
    var dieselPriceText by remember(currentRecord.date, currentRecord.dieselPrice) {
        mutableStateOf(if (currentRecord.dieselPrice == 0.0) "" else if (currentRecord.dieselPrice % 1.0 == 0.0) currentRecord.dieselPrice.toLong().toString() else currentRecord.dieselPrice.toString())
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
                }.sortedBy { it.date }
            }
            PeriodFilter.MONTH -> {
                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(targetDate)
                records.filter { it.date.startsWith(currentMonth) }.sortedBy { it.date }
            }
            PeriodFilter.YEAR -> {
                val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(targetDate)
                records.filter { it.date.startsWith(currentYear) }.sortedBy { it.date }
            }
            PeriodFilter.CUSTOM -> {
                val fromD = try { sdf.parse(fromDateInput) } catch (e: Exception) { null }
                val toD = try { sdf.parse(toDateInput) } catch (e: Exception) { null }
                if (fromD != null && toD != null) {
                    records.filter { rec ->
                        val d = try { sdf.parse(rec.date) } catch (e: Exception) { null }
                        d != null && !d.before(fromD) && !d.after(toD)
                    }.sortedBy { it.date }
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
    val totalCreditCollected = filteredRecords.sumOf { it.dailyCreditCollected }
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

    val periodDesc = remember(selectedPeriod, currentRecord.date, fromDateInput, toDateInput) {
        when (selectedPeriod) {
            PeriodFilter.DAY -> "Day: ${currentRecord.date}"
            PeriodFilter.WEEK -> "Week Ending: ${currentRecord.date}"
            PeriodFilter.MONTH -> "Month: ${currentRecord.date.take(7)}"
            PeriodFilter.YEAR -> "Year: ${currentRecord.date.take(4)}"
            PeriodFilter.CUSTOM -> "Custom Period: $fromDateInput to $toDateInput"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(topInset + 4.dp))

        // READ-ONLY / LOCK WARNING BANNER
        if (!canEdit) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            session.isReadOnly -> "Read-Only Mode Active: Editing and collections entry are disabled."
                            isDayFinalized -> "Day Finalized: Sales entries for ${currentRecord.date} are locked for Managers."
                            else -> "Past Date Locked: You do not have permission to edit past sales data (${currentRecord.date})."
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

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
                        onClick = { showDatePickerModal = true },
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
                            if (canEdit && (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$")))) {
                                petrolPriceText = input
                                val valParsed = round2(input.toDoubleOrNull() ?: 0.0)
                                onRecordChanged(currentRecord.copy(petrolPrice = valParsed))
                                ActivityLogger.log(session, "updated petrol rate to ₹$valParsed for ${currentRecord.date}")
                            }
                        },
                        label = { Text("Petrol Price (₹)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = canEdit,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = dieselPriceText,
                        onValueChange = { input ->
                            if (canEdit && (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$")))) {
                                dieselPriceText = input
                                val valParsed = round2(input.toDoubleOrNull() ?: 0.0)
                                onRecordChanged(currentRecord.copy(dieselPrice = valParsed))
                                ActivityLogger.log(session, "updated diesel rate to ₹$valParsed for ${currentRecord.date}")
                            }
                        },
                        label = { Text("Diesel Price (₹)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = canEdit,
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
                    onValueChange = { if (canEdit) fromDateInput = it },
                    label = { Text("From Date (YYYY-MM-DD)", fontSize = 9.sp) },
                    singleLine = true,
                    enabled = canEdit,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = toDateInput,
                    onValueChange = { if (canEdit) toDateInput = it },
                    label = { Text("To Date (YYYY-MM-DD)", fontSize = 9.sp) },
                    singleLine = true,
                    enabled = canEdit,
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
                session = session,
                currentRecordDate = currentRecord.date,
                shiftTitle = "Shift 1",
                shift = currentRecord.shift1,
                petrolPrice = currentRecord.petrolPrice,
                dieselPrice = currentRecord.dieselPrice,
                petrolColor = petrolColor,
                dieselColor = dieselColor,
                canEdit = canEdit,
                onShiftUpdated = { updatedShift ->
                    if (canEdit) {
                        onRecordChanged(currentRecord.copy(shift1 = updatedShift))
                        ActivityLogger.log(session, "updated Shift 1 collection entries for ${currentRecord.date}")
                    }
                }
            )

            ShiftDetailedSalesBlock(
                session = session,
                currentRecordDate = currentRecord.date,
                shiftTitle = "Shift 2",
                shift = currentRecord.shift2,
                petrolPrice = currentRecord.petrolPrice,
                dieselPrice = currentRecord.dieselPrice,
                petrolColor = petrolColor,
                dieselColor = dieselColor,
                canEdit = canEdit,
                onShiftUpdated = { updatedShift ->
                    if (canEdit) {
                        onRecordChanged(currentRecord.copy(shift2 = updatedShift))
                        ActivityLogger.log(session, "updated Shift 2 collection entries for ${currentRecord.date}")
                    }
                }
            )

            ShiftDetailedSalesBlock(
                session = session,
                currentRecordDate = currentRecord.date,
                shiftTitle = "Shift 3",
                shift = currentRecord.shift3,
                petrolPrice = currentRecord.petrolPrice,
                dieselPrice = currentRecord.dieselPrice,
                petrolColor = petrolColor,
                dieselColor = dieselColor,
                canEdit = canEdit,
                onShiftUpdated = { updatedShift ->
                    if (canEdit) {
                        onRecordChanged(currentRecord.copy(shift3 = updatedShift))
                        ActivityLogger.log(session, "updated Shift 3 collection entries for ${currentRecord.date}")
                    }
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
                    text = "  Petrol Sold: ${formatDecimal(totalPetrolLitre)} L   ${formatCurrency(totalPetrolRevenue)}",
                    fontWeight = FontWeight.Bold,
                    color = petrolColor,
                    fontSize = 12.sp
                )
                Text(
                    text = "  Diesel Sold: ${formatDecimal(totalDieselLitre)} L   ${formatCurrency(totalDieselRevenue)}",
                    fontWeight = FontWeight.Bold,
                    color = dieselColor,
                    fontSize = 12.sp
                )
                Text(
                    text = "  Grand Calculated Revenue:   ${formatCurrency(grandTotalRevenue)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    text = "  Cash Collected:   ${formatCurrency(totalCashCollected)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "  Digital Collected:   ${formatCurrency(totalDigitalCollected)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "  Credit (Lend) Given:   ${formatCurrency(totalCreditCollected)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "  Total Payment Settled:   ${formatCurrency(totalCollected)}",
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
            onClick = { showExportFormatDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Export Sales Report", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(Modifier.height(bottomInset + 4.dp))
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

    if (showExportFormatDialog) {
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            title = {
                Text(
                    text = "Choose Export Format",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select your preferred file format for $periodDesc:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            showExportFormatDialog = false
                            exportSalesRecord(context, filteredRecords, allExpenses, periodDesc, ExportFormat.XLS)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".XLS (Excel Table with Refill Data)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            showExportFormatDialog = false
                            exportSalesRecord(context, filteredRecords, allExpenses, periodDesc, ExportFormat.CSV)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".CSV (Plain Text Spreadsheet)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            showExportFormatDialog = false
                            exportSalesRecord(context, filteredRecords, allExpenses, periodDesc, ExportFormat.PDF)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".PDF (Native Printable Document)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportFormatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ShiftDetailedSalesBlock(
    session: AppUserSession,
    currentRecordDate: String,
    shiftTitle: String,
    shift: DayShift,
    petrolPrice: Double,
    dieselPrice: Double,
    petrolColor: Color,
    dieselColor: Color,
    canEdit: Boolean = true,
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
                    text = "Shift Revenue:   ${formatCurrency(shiftRevenue)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MpdSalesColumn(
                    session = session,
                    currentRecordDate = currentRecordDate,
                    shiftTitle = shiftTitle,
                    mpdTitle = "MPD 1",
                    dispenser = shift.mpd1,
                    petrolPrice = petrolPrice,
                    dieselPrice = dieselPrice,
                    petrolColor = petrolColor,
                    dieselColor = dieselColor,
                    canEdit = canEdit,
                    modifier = Modifier.weight(1f),
                    onDispenserUpdated = { updatedMpd1 ->
                        if (canEdit) onShiftUpdated(shift.copy(mpd1 = updatedMpd1))
                    }
                )

                MpdSalesColumn(
                    session = session,
                    currentRecordDate = currentRecordDate,
                    shiftTitle = shiftTitle,
                    mpdTitle = "MPD 2",
                    dispenser = shift.mpd2,
                    petrolPrice = petrolPrice,
                    dieselPrice = dieselPrice,
                    petrolColor = petrolColor,
                    dieselColor = dieselColor,
                    canEdit = canEdit,
                    modifier = Modifier.weight(1f),
                    onDispenserUpdated = { updatedMpd2 ->
                        if (canEdit) onShiftUpdated(shift.copy(mpd2 = updatedMpd2))
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
                    text = "Shift Collected:   ${formatCurrency(shift.totalCollected)}",
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
    session: AppUserSession,
    currentRecordDate: String,
    shiftTitle: String,
    mpdTitle: String,
    dispenser: DispenserShift,
    petrolPrice: Double,
    dieselPrice: Double,
    petrolColor: Color,
    dieselColor: Color,
    canEdit: Boolean = true,
    modifier: Modifier = Modifier,
    onDispenserUpdated: (DispenserShift) -> Unit
) {
    val petrolRev = dispenser.petrolSale * petrolPrice
    val dieselRev = dispenser.dieselSale * dieselPrice
    val mpdRevenue = petrolRev + dieselRev
    val mpdMismatch = dispenser.getMismatch(petrolPrice, dieselPrice)

    // INDEPENDENT RAW STRING STATES PREVENT TEXT CURSOR JUMPING & ERASING DECIMALS MID-TYPING
    var cashText by remember {
        mutableStateOf(if (dispenser.cashCollected == 0.0) "" else if (dispenser.cashCollected % 1.0 == 0.0) dispenser.cashCollected.toLong().toString() else dispenser.cashCollected.toString())
    }
    var digitalText by remember {
        mutableStateOf(if (dispenser.digitalCollected == 0.0) "" else if (dispenser.digitalCollected % 1.0 == 0.0) dispenser.digitalCollected.toLong().toString() else dispenser.digitalCollected.toString())
    }
    var creditText by remember {
        mutableStateOf(if (dispenser.creditCollected == 0.0) "" else if (dispenser.creditCollected % 1.0 == 0.0) dispenser.creditCollected.toLong().toString() else dispenser.creditCollected.toString())
    }

    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(mpdTitle, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("Petrol (${formatDecimal(dispenser.petrolSale)} L):   ${formatCurrency(petrolRev)}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = petrolColor)
            Text("Diesel (${formatDecimal(dispenser.dieselSale)} L):   ${formatCurrency(dieselRev)}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = dieselColor)
            Text("MPD Revenue:   ${formatCurrency(mpdRevenue)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            HorizontalDivider(modifier = Modifier.padding(vertical = 1.dp))

            OutlinedTextField(
                value = cashText,
                onValueChange = { input ->
                    if (canEdit && (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$")))) {
                        cashText = input
                        val parsed = round2(input.toDoubleOrNull() ?: 0.0)
                        onDispenserUpdated(dispenser.copy(cashCollected = parsed))
                        ActivityLogger.log(session, "updated $shiftTitle $mpdTitle cash collection to ₹$parsed for $currentRecordDate")
                    }
                },
                label = { Text("Cash  ", fontSize = 8.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = canEdit,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = digitalText,
                onValueChange = { input ->
                    if (canEdit && (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$")))) {
                        digitalText = input
                        val parsed = round2(input.toDoubleOrNull() ?: 0.0)
                        onDispenserUpdated(dispenser.copy(digitalCollected = parsed))
                        ActivityLogger.log(session, "updated $shiftTitle $mpdTitle digital collection to ₹$parsed for $currentRecordDate")
                    }
                },
                label = { Text("Digital  ", fontSize = 8.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = canEdit,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = creditText,
                onValueChange = { input ->
                    if (canEdit && (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$")))) {
                        creditText = input
                        val parsed = round2(input.toDoubleOrNull() ?: 0.0)
                        onDispenserUpdated(dispenser.copy(creditCollected = parsed))
                        ActivityLogger.log(session, "updated $shiftTitle $mpdTitle credit collection to ₹$parsed for $currentRecordDate")
                    }
                },
                label = { Text("Credit (Lend)  ", fontSize = 8.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = canEdit,
                modifier = Modifier.fillMaxWidth()
            )

            val mismatchColor = when {
                mpdMismatch > 0.0 -> Color(0xFF2E7D32)
                mpdMismatch < 0.0 -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.onSurface
            }

            Text(
                text = "Collected:   ${formatCurrency(dispenser.totalCollected)}",
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

private fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

private fun formatCurrency(amount: Double): String {
    return String.format(Locale.getDefault(), "%.2f", amount)
}

private fun formatSignedCurrency(amount: Double): String {
    val sign = if (amount > 0.0) "+" else ""
    return "$sign  ${String.format(Locale.getDefault(), "%.2f", amount)}"
}

private fun exportSalesRecord(
    context: Context,
    records: List<DailyFuelRecord>,
    allExpenses: List<ExpenseItem>,
    periodDescription: String,
    format: ExportFormat
) {
    val fileTimestamp = System.currentTimeMillis()
    val isMultiDay = records.size > 1

    val sumPetrolLitre = records.sumOf { it.totalPetrolSell }
    val sumPetrolRefill = records.sumOf { it.petrolRefill }
    val sumPetrolRev = records.sumOf { it.totalPetrolRevenue }
    val sumPetrolVar = records.sumOf { it.petrolVariation }

    val sumDieselLitre = records.sumOf { it.totalDieselSell }
    val sumDieselRefill = records.sumOf { it.dieselRefill }
    val sumDieselRev = records.sumOf { it.totalDieselRevenue }
    val sumDieselVar = records.sumOf { it.dieselVariation }

    val sumGrandRev = records.sumOf { it.grandTotalRevenue }
    val sumCash = records.sumOf { it.dailyCashCollected }
    val sumDigital = records.sumOf { it.dailyDigitalCollected }
    val sumCredit = records.sumOf { it.dailyCreditCollected }
    val sumTotalCollected = records.sumOf { it.dailyTotalCollected }
    val sumMismatch = records.sumOf { it.dailyMismatch }
    val sumExpenses = records.sumOf { record ->
        allExpenses.filter { it.date == record.date }.sumOf { it.amount }
    }
    val sumNetVar = sumPetrolVar + sumDieselVar

    when (format) {
        ExportFormat.CSV -> {
            val csvBuilder = StringBuilder().apply {
                append("NH Fuel Station Sales Report\n")
                append("Report Period: $periodDescription\n\n")
                append("Date,Petrol Sold (L),Petrol Refill (L),Petrol Rate (Rs),Petrol Revenue (Rs),Petrol Stock (L),Diesel Sold (L),Diesel Refill (L),Diesel Rate (Rs),Diesel Revenue (Rs),Diesel Stock (L),Grand Total Revenue (Rs),Cash Collected (Rs),Digital Collected (Rs),Credit (Lend) (Rs),Total Payment Collected (Rs),Total Mismatch (Rs),Petrol Var (L),Diesel Var (L),Net Var (L),Expenses (Rs)\n")
                records.forEach { record ->
                    val dayExpense = allExpenses.filter { it.date == record.date }.sumOf { it.amount }
                    val netVar = record.petrolVariation + record.dieselVariation
                    val rawMismatch = String.format(Locale.US, "%.2f", record.dailyMismatch)
                    append("${record.date},${formatDecimal(record.totalPetrolSell)},${formatDecimal(record.petrolRefill)},${record.petrolPrice},${formatCurrency(record.totalPetrolRevenue)},${formatDecimal(record.currentPetrolStorage)},${formatDecimal(record.totalDieselSell)},${formatDecimal(record.dieselRefill)},${record.dieselPrice},${formatCurrency(record.totalDieselRevenue)},${formatDecimal(record.currentDieselStorage)},${formatCurrency(record.grandTotalRevenue)},${formatCurrency(record.dailyCashCollected)},${formatCurrency(record.dailyDigitalCollected)},${formatCurrency(record.dailyCreditCollected)},${formatCurrency(record.dailyTotalCollected)},$rawMismatch,${formatDecimal(record.petrolVariation)},${formatDecimal(record.dieselVariation)},${formatDecimal(netVar)},${formatCurrency(dayExpense)}\n")
                }
                if (isMultiDay) {
                    val rawSumMismatch = String.format(Locale.US, "%.2f", sumMismatch)
                    append("GRAND TOTAL,${formatDecimal(sumPetrolLitre)},${formatDecimal(sumPetrolRefill)},-,${formatCurrency(sumPetrolRev)},-,${formatDecimal(sumDieselLitre)},${formatDecimal(sumDieselRefill)},-,${formatCurrency(sumDieselRev)},-,${formatCurrency(sumGrandRev)},${formatCurrency(sumCash)},${formatCurrency(sumDigital)},${formatCurrency(sumCredit)},${formatCurrency(sumTotalCollected)},$rawSumMismatch,${formatDecimal(sumPetrolVar)},${formatDecimal(sumDieselVar)},${formatDecimal(sumNetVar)},${formatCurrency(sumExpenses)}\n")
                }
            }
            try {
                val file = File(context.cacheDir, "NHFuel_Sales_Report_$fileTimestamp.csv")
                file.writeText(csvBuilder.toString(), Charsets.UTF_8)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "NH Fuel Sales Report")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Sales CSV"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        ExportFormat.XLS, ExportFormat.PDF -> {
            val totalColumns = 21
            val htmlContent = StringBuilder().apply {
                append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                append("<title>NH Fuel Station Sales Report</title><style>")
                append("body { font-family: Arial, sans-serif; font-size: 11px; background-color: #FFFFFF; padding: 10px; }")
                append("table { border-collapse: collapse; width: 100%; }")
                append("th, td { border: 1px solid #444444; padding: 6px 8px; text-align: left; }")
                append(".report-title { background-color: #1A237E; color: #FFFFFF; font-size: 16px; font-weight: bold; text-align: center; }")
                append(".period-title { background-color: #E8EAF6; color: #1A237E; font-size: 12px; font-weight: bold; text-align: center; }")
                append(".header-row th { background-color: #1565C0; color: #FFFFFF; font-weight: bold; text-align: center; }")
                append(".grand-total-row td { background-color: #FFF9C4; color: #000000; font-weight: bold; }")
                append(".number-cell { text-align: right; }")
                append("</style></head><body>")
                append("<table>")
                append("<tr><td colspan=\"$totalColumns\" class=\"report-title\">NH Fuel Station Sales Report</td></tr>")
                append("<tr><td colspan=\"$totalColumns\" class=\"period-title\">Report Period: $periodDescription</td></tr>")
                append("<tr><td colspan=\"$totalColumns\" style=\"border: none; height: 10px;\"></td></tr>")
                append("<tr class=\"header-row\">")
                append("<th>Date</th>")
                append("<th>Petrol Sold (L)</th>")
                append("<th>Petrol Refill (L)</th>")
                append("<th>Petrol Rate (Rs)</th>")
                append("<th>Petrol Revenue (Rs)</th>")
                append("<th>Petrol Stock (L)</th>")
                append("<th>Diesel Sold (L)</th>")
                append("<th>Diesel Refill (L)</th>")
                append("<th>Diesel Rate (Rs)</th>")
                append("<th>Diesel Revenue (Rs)</th>")
                append("<th>Diesel Stock (L)</th>")
                append("<th>Grand Total Revenue (Rs)</th>")
                append("<th>Cash Collected (Rs)</th>")
                append("<th>Digital Collected (Rs)</th>")
                append("<th>Credit (Lend) (Rs)</th>")
                append("<th>Total Payment Collected (Rs)</th>")
                append("<th>Total Mismatch (Rs)</th>")
                append("<th>Petrol Var (L)</th>")
                append("<th>Diesel Var (L)</th>")
                append("<th>Net Var (L)</th>")
                append("<th>Expenses (Rs)</th>")
                append("</tr>")
                records.forEach { record ->
                    val dayExpense = allExpenses.filter { it.date == record.date }.sumOf { it.amount }
                    val netVar = record.petrolVariation + record.dieselVariation
                    val rawMismatch = String.format(Locale.US, "%.2f", record.dailyMismatch)
                    append("<tr>")
                    append("<td>${record.date}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(record.totalPetrolSell)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(record.petrolRefill)}</td>")
                    append("<td class=\"number-cell\">${record.petrolPrice}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(record.totalPetrolRevenue)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(record.currentPetrolStorage)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(record.totalDieselSell)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(record.dieselRefill)}</td>")
                    append("<td class=\"number-cell\">${record.dieselPrice}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(record.totalDieselRevenue)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(record.currentDieselStorage)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(record.grandTotalRevenue)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(record.dailyCashCollected)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(record.dailyDigitalCollected)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(record.dailyCreditCollected)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(record.dailyTotalCollected)}</td>")
                    append("<td class=\"number-cell\">$rawMismatch</td>")
                    append("<td class=\"number-cell\">${formatDecimal(record.petrolVariation)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(record.dieselVariation)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(netVar)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(dayExpense)}</td>")
                    append("</tr>")
                }
                if (isMultiDay) {
                    val rawSumMismatch = String.format(Locale.US, "%.2f", sumMismatch)
                    append("<tr class=\"grand-total-row\">")
                    append("<td>GRAND TOTAL</td>")
                    append("<td class=\"number-cell\">${formatDecimal(sumPetrolLitre)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(sumPetrolRefill)}</td>")
                    append("<td class=\"number-cell\">-</td>")
                    append("<td class=\"number-cell\">${formatCurrency(sumPetrolRev)}</td>")
                    append("<td class=\"number-cell\">-</td>")
                    append("<td class=\"number-cell\">${formatDecimal(sumDieselLitre)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(sumDieselRefill)}</td>")
                    append("<td class=\"number-cell\">-</td>")
                    append("<td class=\"number-cell\">${formatCurrency(sumDieselRev)}</td>")
                    append("<td class=\"number-cell\">-</td>")
                    append("<td class=\"number-cell\">${formatCurrency(sumGrandRev)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(sumCash)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(sumDigital)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(sumCredit)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(sumTotalCollected)}</td>")
                    append("<td class=\"number-cell\">$rawSumMismatch</td>")
                    append("<td class=\"number-cell\">${formatDecimal(sumPetrolVar)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(sumDieselVar)}</td>")
                    append("<td class=\"number-cell\">${formatDecimal(sumNetVar)}</td>")
                    append("<td class=\"number-cell\">${formatCurrency(sumExpenses)}</td>")
                    append("</tr>")
                }
                append("</table></body></html>")
            }
            if (format == ExportFormat.XLS) {
                try {
                    val file = File(context.cacheDir, "NHFuel_Sales_Report_${fileTimestamp}.xls")
                    file.writeText(htmlContent.toString(), Charsets.UTF_8)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.ms-excel"
                        putExtra(Intent.EXTRA_SUBJECT, "NH Fuel Sales Report")
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Excel Sales Report"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    val webView = WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                val printAdapter = view?.createPrintDocumentAdapter("NHFuel_Sales_Report_$fileTimestamp")
                                val jobName = "NH Fuel Sales Report $periodDescription"
                                if (printAdapter != null) {
                                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                                }
                            }
                        }
                    }
                    webView.loadDataWithBaseURL(null, htmlContent.toString(), "text/html", "UTF-8", null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
