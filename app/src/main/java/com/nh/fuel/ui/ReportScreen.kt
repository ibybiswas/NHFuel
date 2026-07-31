package com.nh.fuel.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nh.fuel.data.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportPeriodFilter { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, CUSTOM }
enum class ReportExportFormat { XLS, PDF_HTML }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    currentRecordDate: String,
    allRecords: List<DailyFuelRecord> = emptyList(),
    allExpenses: List<ExpenseItem> = emptyList(),
    allCredits: List<CreditRecord> = emptyList(),
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(ReportPeriodFilter.TODAY) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    var customFromDate by remember { mutableStateOf(currentRecordDate) }
    var customToDate by remember { mutableStateOf(currentRecordDate) }

    // Filter Records based on Period Selection (Using exact Business Day logic)
    val filteredRecords = remember(selectedPeriod, currentRecordDate, customFromDate, customToDate, allRecords) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDate = try { sdf.parse(currentRecordDate) ?: Date() } catch (e: Exception) { Date() }
        val cal = Calendar.getInstance().apply { time = targetDate }

        when (selectedPeriod) {
            ReportPeriodFilter.TODAY -> allRecords.filter { it.date == currentRecordDate }
            ReportPeriodFilter.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_MONTH, -1)
                val yesterdayStr = sdf.format(cal.time)
                allRecords.filter { it.date == yesterdayStr }
            }
            ReportPeriodFilter.THIS_WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val startDate = cal.time
                allRecords.filter { rec ->
                    val recDate = try { sdf.parse(rec.date) } catch (e: Exception) { null }
                    recDate != null && !recDate.before(startDate) && !recDate.after(targetDate)
                }
            }
            ReportPeriodFilter.THIS_MONTH -> {
                val currentMonth = currentRecordDate.take(7)
                allRecords.filter { it.date.startsWith(currentMonth) }
            }
            ReportPeriodFilter.CUSTOM -> {
                val fromD = try { sdf.parse(customFromDate) } catch (e: Exception) { null }
                val toD = try { sdf.parse(customToDate) } catch (e: Exception) { null }
                if (fromD != null && toD != null) {
                    allRecords.filter { rec ->
                        val d = try { sdf.parse(rec.date) } catch (e: Exception) { null }
                        d != null && !d.before(fromD) && !d.after(toD)
                    }
                } else allRecords.filter { it.date == currentRecordDate }
            }
        }
    }

    val periodDates = remember(filteredRecords) { filteredRecords.map { it.date }.toSet() }

    val filteredExpenses = remember(allExpenses, periodDates, selectedPeriod, currentRecordDate) {
        if (selectedPeriod == ReportPeriodFilter.TODAY) allExpenses.filter { it.date == currentRecordDate }
        else allExpenses.filter { it.date in periodDates }
    }

    // Key Business Calculations
    val totalPetrolLitre = filteredRecords.sumOf { it.totalPetrolSell }
    val totalDieselLitre = filteredRecords.sumOf { it.totalDieselSell }
    val totalVolumeLitre = totalPetrolLitre + totalDieselLitre

    val totalPetrolRev = filteredRecords.sumOf { it.totalPetrolRevenue }
    val totalDieselRev = filteredRecords.sumOf { it.totalDieselRevenue }
    val grossRevenue = totalPetrolRev + totalDieselRev

    val totalExpenses = filteredExpenses.sumOf { it.amount }

    val totalCash = filteredRecords.sumOf { it.dailyCashCollected }
    val totalDigital = filteredRecords.sumOf { it.dailyDigitalCollected }
    val totalCreditIssued = filteredRecords.sumOf { it.dailyCreditCollected }
    val totalMismatch = filteredRecords.sumOf { it.dailyMismatch }

    val totalPetrolRefill = filteredRecords.sumOf { it.petrolRefill }
    val totalDieselRefill = filteredRecords.sumOf { it.dieselRefill }
    val totalPetrolVariation = filteredRecords.sumOf { it.petrolVariation }
    val totalDieselVariation = filteredRecords.sumOf { it.dieselVariation }
    val netTotalVariation = totalPetrolVariation + totalDieselVariation

    val petrolColor = Color(0xFFFF9800)  // Orange
    val dieselColor = Color(0xFF29B6F6)  // Light Blue

    val periodDesc = remember(selectedPeriod, currentRecordDate, customFromDate, customToDate) {
        when (selectedPeriod) {
            ReportPeriodFilter.TODAY -> "Today ($currentRecordDate)"
            ReportPeriodFilter.YESTERDAY -> "Yesterday"
            ReportPeriodFilter.THIS_WEEK -> "This Week (Ending $currentRecordDate)"
            ReportPeriodFilter.THIS_MONTH -> "This Month (${currentRecordDate.take(7)})"
            ReportPeriodFilter.CUSTOM -> "Custom ($customFromDate to $customToDate)"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(topInset + 4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reports & Analytics",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Right Row: Dropdown Period Menu + Share Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = selectedPeriod.name.replace("_", " "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        ReportPeriodFilter.values().forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.name.replace("_", " "), fontSize = 12.sp) },
                                onClick = {
                                    selectedPeriod = period
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Share / Export Icon Button sitting right beside Dropdown
                FilledTonalIconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export Report",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (selectedPeriod == ReportPeriodFilter.CUSTOM) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customFromDate,
                    onValueChange = { customFromDate = it },
                    label = { Text("From Date", fontSize = 8.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = customToDate,
                    onValueChange = { customToDate = it },
                    label = { Text("To Date", fontSize = 8.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top KPI Cards: Gross Revenue (Left) & Total Expenses (Right)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "Gross Revenue",
                value = "₹ ${formatVal(grossRevenue)}",
                subtitle = "Calculated Sales",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Total Expenses",
                value = "₹ ${formatVal(totalExpenses)}",
                subtitle = "${filteredExpenses.size} items recorded",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        // Volume Sold & Net Mismatch
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "Total Volume Sold",
                value = "${formatVal(totalVolumeLitre)} L",
                subtitle = "Petrol: ${formatVal(totalPetrolLitre)}L | Diesel: ${formatVal(totalDieselLitre)}L",
                color = Color(0xFF1565C0),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Net Mismatch",
                value = if (totalMismatch > 0) "+₹ ${formatVal(totalMismatch)}" else "₹ ${formatVal(totalMismatch)}",
                subtitle = if (totalMismatch >= 0) "Surplus" else "Shortage",
                color = if (totalMismatch < 0) Color(0xFFC62828) else Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }

        // Pie Chart 1: Revenue Breakdown (₹) with 2 decimal precision
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Sales Revenue Breakdown (₹)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FuelPieChart(
                    petrolValue = totalPetrolRev,
                    dieselValue = totalDieselRev,
                    petrolColor = petrolColor,
                    dieselColor = dieselColor,
                    unitPrefix = "₹"
                )
            }
        }

        // Pie Chart 2: Volume Sold Breakdown (L) with 2 decimal precision
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Fuel Volume Sold Breakdown (Litres)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FuelPieChart(
                    petrolValue = totalPetrolLitre,
                    dieselValue = totalDieselLitre,
                    petrolColor = petrolColor,
                    dieselColor = dieselColor,
                    unitSuffix = "L"
                )
            }
        }

        // Shift-by-Shift Performance Breakdown
        if (filteredRecords.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Shift-by-Shift Performance Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val s1Rev = filteredRecords.sumOf { it.shift1.getRevenue(it.petrolPrice, it.dieselPrice) }
                    val s2Rev = filteredRecords.sumOf { it.shift2.getRevenue(it.petrolPrice, it.dieselPrice) }
                    val s3Rev = filteredRecords.sumOf { it.shift3.getRevenue(it.petrolPrice, it.dieselPrice) }
                    val maxShiftRev = maxOf(s1Rev, s2Rev, s3Rev, 1.0)

                    ShiftBarItem("Shift 1", s1Rev, maxShiftRev, Color(0xFF7E57C2))
                    ShiftBarItem("Shift 2", s2Rev, maxShiftRev, Color(0xFF26A69A))
                    ShiftBarItem("Shift 3", s3Rev, maxShiftRev, Color(0xFFFFA726))
                }
            }
        }

        // Payment Collections Breakdown Segment Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Payment Method Distribution",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                PaymentDistributionBar(
                    cash = totalCash,
                    digital = totalDigital,
                    credit = totalCreditIssued
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem("Cash", "₹ ${formatVal(totalCash)}", Color(0xFF4CAF50))
                    LegendItem("Digital UPI", "₹ ${formatVal(totalDigital)}", Color(0xFF2196F3))
                    LegendItem("Credit (Lend)", "₹ ${formatVal(totalCreditIssued)}", Color(0xFFFF9800))
                }
            }
        }

        // Tank Refill & Variation Metrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tank Refill & Net Variation Analytics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Petrol Refills Added:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${formatVal(totalPetrolRefill)} L", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = petrolColor)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Diesel Refills Added:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${formatVal(totalDieselRefill)} L", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = dieselColor)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Petrol Tank Variation:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${if (totalPetrolVariation > 0) "+" else ""}${formatVal(totalPetrolVariation)} L",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalPetrolVariation < 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Diesel Tank Variation:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${if (totalDieselVariation > 0) "+" else ""}${formatVal(totalDieselVariation)} L",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalDieselVariation < 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Net Combined Variation (Petrol + Diesel):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "${if (netTotalVariation > 0) "+" else ""}${formatVal(netTotalVariation)} L",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (netTotalVariation < 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }
            }
        }

        Spacer(Modifier.height(bottomInset + 8.dp))
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Report Dashboard", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose format to share summary for $periodDesc:", fontSize = 12.sp)

                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            exportReportDashboard(
                                context = context,
                                periodDescription = periodDesc,
                                grossRevenue = grossRevenue,
                                totalExpenses = totalExpenses,
                                totalVolumeLitre = totalVolumeLitre,
                                totalPetrolLitre = totalPetrolLitre,
                                totalDieselLitre = totalDieselLitre,
                                totalPetrolRev = totalPetrolRev,
                                totalDieselRev = totalDieselRev,
                                totalCash = totalCash,
                                totalDigital = totalDigital,
                                totalCreditIssued = totalCreditIssued,
                                totalMismatch = totalMismatch,
                                totalPetrolRefill = totalPetrolRefill,
                                totalDieselRefill = totalDieselRefill,
                                netTotalVariation = netTotalVariation,
                                format = ReportExportFormat.XLS
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".XLS (Excel Workbook with Visual SVG Charts)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            exportReportDashboard(
                                context = context,
                                periodDescription = periodDesc,
                                grossRevenue = grossRevenue,
                                totalExpenses = totalExpenses,
                                totalVolumeLitre = totalVolumeLitre,
                                totalPetrolLitre = totalPetrolLitre,
                                totalDieselLitre = totalDieselLitre,
                                totalPetrolRev = totalPetrolRev,
                                totalDieselRev = totalDieselRev,
                                totalCash = totalCash,
                                totalDigital = totalDigital,
                                totalCreditIssued = totalCreditIssued,
                                totalMismatch = totalMismatch,
                                totalPetrolRefill = totalPetrolRefill,
                                totalDieselRefill = totalDieselRefill,
                                netTotalVariation = netTotalVariation,
                                format = ReportExportFormat.PDF_HTML
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".PDF / Styled Document (Visual Infographic)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FuelPieChart(
    petrolValue: Double,
    dieselValue: Double,
    petrolColor: Color,
    dieselColor: Color,
    unitPrefix: String = "",
    unitSuffix: String = ""
) {
    val total = petrolValue + dieselValue
    val petrolSweep = if (total > 0) ((petrolValue / total) * 360f).toFloat() else 180f
    val dieselSweep = if (total > 0) ((dieselValue / total) * 360f).toFloat() else 180f

    val petrolPct = if (total > 0) String.format(Locale.getDefault(), "%.2f", petrolValue / total * 100) else "50.00"
    val dieselPct = if (total > 0) String.format(Locale.getDefault(), "%.2f", dieselValue / total * 100) else "50.00"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Canvas(modifier = Modifier.size(130.dp)) {
            drawArc(
                color = petrolColor,
                startAngle = -90f,
                sweepAngle = petrolSweep,
                useCenter = true
            )
            drawArc(
                color = dieselColor,
                startAngle = -90f + petrolSweep,
                sweepAngle = dieselSweep,
                useCenter = true
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PieLegendItem(
                label = "Petrol ($petrolPct%)",
                amountStr = "$unitPrefix ${formatVal(petrolValue)} $unitSuffix".trim(),
                color = petrolColor
            )
            PieLegendItem(
                label = "Diesel ($dieselPct%)",
                amountStr = "$unitPrefix ${formatVal(dieselValue)} $unitSuffix".trim(),
                color = dieselColor
            )
        }
    }
}

@Composable
private fun PieLegendItem(label: String, amountStr: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Column {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(amountStr, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PaymentDistributionBar(
    cash: Double,
    digital: Double,
    credit: Double
) {
    val total = cash + digital + credit
    val cashWeight = if (total > 0) (cash / total).toFloat() else 0.33f
    val digitalWeight = if (total > 0) (digital / total).toFloat() else 0.33f
    val creditWeight = if (total > 0) (credit / total).toFloat() else 0.34f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (cashWeight > 0f) {
            Box(modifier = Modifier.weight(cashWeight).fillMaxHeight().background(Color(0xFF4CAF50)))
        }
        if (digitalWeight > 0f) {
            Box(modifier = Modifier.weight(digitalWeight).fillMaxHeight().background(Color(0xFF2196F3)))
        }
        if (creditWeight > 0f) {
            Box(modifier = Modifier.weight(creditWeight).fillMaxHeight().background(Color(0xFFFF9800)))
        }
    }
}

@Composable
private fun LegendItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Column {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ShiftBarItem(
    shiftName: String,
    revenue: Double,
    maxRevenue: Double,
    barColor: Color
) {
    val fraction = if (maxRevenue > 0) (revenue / maxRevenue).toFloat().coerceIn(0.05f, 1f) else 0.05f

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(shiftName, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }

        Text("₹ ${formatVal(revenue)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
    }
}

private fun formatVal(value: Double): String {
    return String.format(Locale.getDefault(), "%.2f", value)
}

private fun exportReportDashboard(
    context: Context,
    periodDescription: String,
    grossRevenue: Double,
    totalExpenses: Double,
    totalVolumeLitre: Double,
    totalPetrolLitre: Double,
    totalDieselLitre: Double,
    totalPetrolRev: Double,
    totalDieselRev: Double,
    totalCash: Double,
    totalDigital: Double,
    totalCreditIssued: Double,
    totalMismatch: Double,
    totalPetrolRefill: Double,
    totalDieselRefill: Double,
    netTotalVariation: Double,
    format: ReportExportFormat
) {
    val fileTimestamp = System.currentTimeMillis()
    val ext = if (format == ReportExportFormat.XLS) "xls" else "html"
    val mime = if (format == ReportExportFormat.XLS) "application/vnd.ms-excel" else "text/html"

    val totalVol = if (totalVolumeLitre > 0) totalVolumeLitre else 1.0
    val pVolPct = String.format(Locale.US, "%.2f", (totalPetrolLitre / totalVol) * 100)
    val dVolPct = String.format(Locale.US, "%.2f", (totalDieselLitre / totalVol) * 100)

    val totalRev = if (grossRevenue > 0) grossRevenue else 1.0
    val pRevPct = String.format(Locale.US, "%.2f", (totalPetrolRev / totalRev) * 100)
    val dRevPct = String.format(Locale.US, "%.2f", (totalDieselRev / totalRev) * 100)

    val htmlContent = StringBuilder().apply {
        append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
        append("<title>NH Fuel Station Executive Report</title><style>")
        append("body { font-family: Arial, sans-serif; font-size: 13px; background-color: #F5F5F5; padding: 15px; }")
        append(".container { max-width: 800px; margin: 0 auto; background: #FFFFFF; border-radius: 12px; padding: 20px; border: 1px solid #DDDDDD; }")
        append(".header { text-align: center; background: #1A237E; color: white; padding: 15px; border-radius: 8px; margin-bottom: 15px; }")
        append(".kpi-grid { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 15px; }")
        append(".kpi-card { flex: 1; min-width: 170px; background: #F8F9FA; border: 1px solid #CCCCCC; border-radius: 8px; padding: 12px; }")
        append(".kpi-title { font-size: 11px; font-weight: bold; color: #666666; }")
        append(".kpi-value { font-size: 18px; font-weight: bold; margin-top: 4px; }")
        append(".section-title { font-size: 14px; font-weight: bold; color: #1A237E; border-bottom: 2px solid #1A237E; padding-bottom: 4px; margin-top: 15px; margin-bottom: 10px; }")
        append("table { border-collapse: collapse; width: 100%; margin-bottom: 15px; }")
        append("th, td { border: 1px solid #DDDDDD; padding: 8px 12px; text-align: left; }")
        append("th { background-color: #E8EAF6; color: #1A237E; font-weight: bold; }")
        append("</style></head><body>")

        append("<div class=\"container\">")
        append("<div class=\"header\"><h2>NH FUEL STATION - EXECUTIVE REPORT</h2><p>Period: $periodDescription</p></div>")

        append("<div class=\"kpi-grid\">")
        append("<div class=\"kpi-card\"><div class=\"kpi-title\">GROSS REVENUE</div><div class=\"kpi-value\" style=\"color:#1A237E;\">₹ ${formatVal(grossRevenue)}</div></div>")
        append("<div class=\"kpi-card\"><div class=\"kpi-title\">TOTAL EXPENSES</div><div class=\"kpi-value\" style=\"color:#C62828;\">₹ ${formatVal(totalExpenses)}</div></div>")
        append("<div class=\"kpi-card\"><div class=\"kpi-title\">VOLUME SOLD</div><div class=\"kpi-value\" style=\"color:#1565C0;\">${formatVal(totalVolumeLitre)} L</div></div>")
        append("<div class=\"kpi-card\"><div class=\"kpi-title\">NET MISMATCH</div><div class=\"kpi-value\" style=\"color:${if (totalMismatch < 0) "#C62828" else "#2E7D32"};\">${if (totalMismatch > 0) "+" else ""}₹ ${formatVal(totalMismatch)}</div></div>")
        append("</div>")

        append("<div class=\"section-title\">1. Sales & Volume Breakdown</div>")
        append("<table>")
        append("<tr><th>Fuel Type</th><th>Volume Sold (L)</th><th>Volume %</th><th>Revenue (Rs)</th><th>Revenue %</th></tr>")
        append("<tr><td>Petrol</td><td>${formatVal(totalPetrolLitre)} L</td><td>$pVolPct %</td><td>₹ ${formatVal(totalPetrolRev)}</td><td>$pRevPct %</td></tr>")
        append("<tr><td>Diesel</td><td>${formatVal(totalDieselLitre)} L</td><td>$dVolPct %</td><td>₹ ${formatVal(totalDieselRev)}</td><td>$dRevPct %</td></tr>")
        append("</table>")

        append("<div class=\"section-title\">2. Payment Method Distribution</div>")
        append("<table>")
        append("<tr><th>Cash Collected</th><th>Digital UPI Collected</th><th>Credit Issued (Lend)</th></tr>")
        append("<tr><td>₹ ${formatVal(totalCash)}</td><td>₹ ${formatVal(totalDigital)}</td><td>₹ ${formatVal(totalCreditIssued)}</td></tr>")
        append("</table>")

        append("<div class=\"section-title\">3. Tank Stock & Variation Summary</div>")
        append("<table>")
        append("<tr><th>Fuel Type</th><th>Refill Added (L)</th><th>Net Variation (L)</th></tr>")
        append("<tr><td>Petrol</td><td>${formatVal(totalPetrolRefill)} L</td><td>${formatVal(totalPetrolVariation)} L</td></tr>")
        append("<tr><td>Diesel</td><td>${formatVal(totalDieselRefill)} L</td><td>${formatVal(totalDieselVariation)} L</td></tr>")
        append("<tr style=\"font-weight:bold; background-color:#FFF9C4;\"><td colspan=\"2\">Net Combined Variation (Petrol + Diesel)</td><td>${if (netTotalVariation > 0) "+" else ""}${formatVal(netTotalVariation)} L</td></tr>")
        append("</table>")

        append("</div></body></html>")
    }

    try {
        val file = File(context.cacheDir, "NHFuel_Report_${fileTimestamp}.$ext")
        file.writeText(htmlContent.toString(), Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_SUBJECT, "NH Fuel Station Executive Report")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Executive Report"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
