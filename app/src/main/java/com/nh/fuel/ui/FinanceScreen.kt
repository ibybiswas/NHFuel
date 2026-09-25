package com.nh.fuel.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nh.fuel.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExpensePeriodFilter { TODAY, ALL_TIME, THIS_MONTH, THIS_YEAR, CUSTOM }

data class InternalLogEntry(
    val id: String,
    val typeLabel: String,
    val isPayment: Boolean,
    val date: String,
    val timestamp: String,
    val amount: Double,
    val paymentMode: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    session: AppUserSession,
    currentRecordDate: String,
    allExpenses: List<ExpenseItem> = emptyList(),
    allCredits: List<CreditRecord> = emptyList(),
    allRecords: List<DailyFuelRecord> = emptyList(),
    onAddOrUpdateExpense: (ExpenseItem) -> Unit,
    onDeleteExpense: (ExpenseItem) -> Unit,
    onAddOrUpdateCredit: (CreditRecord) -> Unit,
    onDeleteCredit: (CreditRecord) -> Unit,
    onDateSelected: (String) -> Unit = {},
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    var selectedSubTab by remember { mutableIntStateOf(1) } // 0: Daily Expenses, 1: Credit / Lend Ledger
    var selectedCustomerForDetail by remember { mutableStateOf<CreditRecord?>(null) }

    if (selectedCustomerForDetail != null) {
        BackHandler { selectedCustomerForDetail = null }
        val currentCustomer = allCredits.find { it.id == selectedCustomerForDetail?.id } ?: selectedCustomerForDetail!!
        CustomerLedgerDetailScreen(
            session = session,
            customer = currentCustomer,
            allRecords = allRecords,
            currentRecordDate = currentRecordDate,
            onBack = { selectedCustomerForDetail = null },
            onUpdateCustomer = { updated ->
                onAddOrUpdateCredit(updated)
                val paidNow = updated.amountPaid - currentCustomer.amountPaid
                val logMsg = if (paidNow > 0.0) {
                    "recorded payment of ₹$paidNow from ${updated.customerName} (${updated.vehicleNumber}), balance ₹${updated.remainingBalance}"
                } else {
                    "updated credit ledger for ${updated.customerName} (${updated.vehicleNumber})"
                }
                ActivityLogger.log(session, logMsg)
                selectedCustomerForDetail = updated
            },
            onDeleteCustomer = {
                onDeleteCredit(currentCustomer)
                ActivityLogger.log(session, "deleted credit ledger for ${currentCustomer.customerName} (${currentCustomer.vehicleNumber})")
                selectedCustomerForDetail = null
            },
            topInset = topInset,
            bottomInset = bottomInset
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(topInset + 4.dp))

            Text(
                text = "Finance & Ledgers",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedSubTab) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Daily Expenses", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Credit / Lend Ledger", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            when (selectedSubTab) {
                0 -> ExpendScreenContent(
                    session = session,
                    currentRecordDate = currentRecordDate,
                    allExpenses = allExpenses,
                    allRecords = allRecords,
                    onAddOrUpdateExpense = onAddOrUpdateExpense,
                    onDeleteExpense = onDeleteExpense,
                    onDateSelected = onDateSelected,
                    bottomInset = bottomInset
                )
                1 -> CreditLedgerContent(
                    session = session,
                    currentRecordDate = currentRecordDate,
                    allCredits = allCredits,
                    allRecords = allRecords,
                    onCustomerSelected = { selectedCustomerForDetail = it },
                    onAddOrUpdateCredit = onAddOrUpdateCredit,
                    onDeleteCredit = onDeleteCredit,
                    bottomInset = bottomInset
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpendScreenContent(
    session: AppUserSession,
    currentRecordDate: String,
    allExpenses: List<ExpenseItem>,
    allRecords: List<DailyFuelRecord>,
    onAddOrUpdateExpense: (ExpenseItem) -> Unit,
    onDeleteExpense: (ExpenseItem) -> Unit,
    onDateSelected: (String) -> Unit,
    bottomInset: Dp
) {
    var descriptionInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var expenseDateInput by remember(currentRecordDate) { mutableStateOf(currentRecordDate) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    var editingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var selectedAggFilter by remember { mutableStateOf(ExpensePeriodFilter.TODAY) }
    var customFromDate by remember { mutableStateOf(currentRecordDate) }
    var customToDate by remember { mutableStateOf(currentRecordDate) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isPastDate = expenseDateInput < todayStr
    val isDayFinalized = allRecords.find { it.date == expenseDateInput }?.shift3?.isComplete == true

    val hasPastPrivilege = session.canEditPastDates || session.canEditFinancePastDates || session.isOwnerLogin || session.role != Role.MANAGER
    val canEdit = !session.isReadOnly &&
            (!isDayFinalized || hasPastPrivilege) &&
            (!isPastDate || hasPastPrivilege)

    // Whether a specific (possibly different) expense date can be edited/deleted right now -
    // needed once the log below can show entries from many different dates (THIS_MONTH,
    // THIS_YEAR, CUSTOM, ALL_TIME), each of which may have its own past-date/finalized lock.
    fun canEditEntryOn(entryDate: String): Boolean {
        val entryIsPastDate = entryDate < todayStr
        val entryIsDayFinalized = allRecords.find { it.date == entryDate }?.shift3?.isComplete == true
        return !session.isReadOnly &&
                (!entryIsDayFinalized || hasPastPrivilege) &&
                (!entryIsPastDate || hasPastPrivilege)
    }

    val dayExpenses = remember(allExpenses, expenseDateInput) {
        allExpenses.filter { it.date == expenseDateInput }
    }
    val totalDayExpense = remember(dayExpenses) {
        dayExpenses.sumOf { it.amount }
    }

    // The period the "TODAY" chip and the others describe, in one line, for the log header.
    val periodLabel = when (selectedAggFilter) {
        ExpensePeriodFilter.TODAY -> expenseDateInput
        ExpensePeriodFilter.ALL_TIME -> "All Time"
        ExpensePeriodFilter.THIS_MONTH -> expenseDateInput.take(7)
        ExpensePeriodFilter.THIS_YEAR -> expenseDateInput.take(4)
        ExpensePeriodFilter.CUSTOM -> "$customFromDate to $customToDate"
    }

    // All expense entries within the currently selected period, newest first - this is what the
    // "Expense Log" list below shows. TODAY keeps the original single-date-only behaviour.
    val periodExpenses = remember(allExpenses, selectedAggFilter, expenseDateInput, customFromDate, customToDate) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val filtered = when (selectedAggFilter) {
            ExpensePeriodFilter.TODAY -> allExpenses.filter { it.date == expenseDateInput }
            ExpensePeriodFilter.ALL_TIME -> allExpenses
            ExpensePeriodFilter.THIS_MONTH -> {
                val currentMonth = expenseDateInput.take(7)
                allExpenses.filter { it.date.startsWith(currentMonth) }
            }
            ExpensePeriodFilter.THIS_YEAR -> {
                val currentYear = expenseDateInput.take(4)
                allExpenses.filter { it.date.startsWith(currentYear) }
            }
            ExpensePeriodFilter.CUSTOM -> {
                val fromD = try { sdf.parse(customFromDate) } catch (e: Exception) { null }
                val toD = try { sdf.parse(customToDate) } catch (e: Exception) { null }
                if (fromD != null && toD != null) {
                    allExpenses.filter { exp ->
                        val expD = try { sdf.parse(exp.date) } catch (e: Exception) { null }
                        expD != null && !expD.before(fromD) && !expD.after(toD)
                    }
                } else allExpenses
            }
        }
        filtered.sortedWith(compareByDescending<ExpenseItem> { it.date }.thenByDescending { it.id })
    }

    val aggregatedExpenseTotal = remember(periodExpenses) {
        periodExpenses.sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = bottomInset + 12.dp)
    ) {
        if (!canEdit) {
            item {
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
                                session.isReadOnly -> "Read-Only Mode: Data entry is restricted."
                                isDayFinalized -> "Day Finalized: Expenses for $expenseDateInput are locked and sealed."
                                else -> "Past Date Locked: You do not have permission to add or edit expenses for past dates ($expenseDateInput)."
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Add New Expense",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("Expense Description * (e.g. Tea/Snacks, Generator Repair)", fontSize = 11.sp) },
                        singleLine = true,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Amount (₹)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = canEdit,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = expenseDateInput,
                            onValueChange = { newDate ->
                                expenseDateInput = newDate
                                onDateSelected(newDate)
                            },
                            label = { Text("Date", fontSize = 11.sp) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showDatePickerModal = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Pick Date",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val amount = round2(amountInput.toDoubleOrNull() ?: 0.0)
                            if (descriptionInput.isNotBlank() && amount > 0.0 && canEdit) {
                                val targetDate = expenseDateInput.ifBlank { currentRecordDate }
                                val realWallClockTime = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                                onAddOrUpdateExpense(
                                    ExpenseItem(
                                        id = System.currentTimeMillis(),
                                        date = targetDate,
                                        description = descriptionInput.trim(),
                                        amount = amount,
                                        timestamp = realWallClockTime
                                    )
                                )
                                ActivityLogger.log(session, "added expense of ₹$amount (${descriptionInput.trim()}) on $targetDate")
                                descriptionInput = ""
                                amountInput = ""
                            }
                        },
                        enabled = canEdit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Expense", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Expenses ($expenseDateInput):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedAggFilter.name.replace("_", " ")} Total",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₹ ${String.format(Locale.getDefault(), "%.2f", totalDayExpense)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "₹ ${String.format(Locale.getDefault(), "%.2f", aggregatedExpenseTotal)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ExpensePeriodFilter.values().forEach { filter ->
                            FilterChip(
                                selected = selectedAggFilter == filter,
                                onClick = { selectedAggFilter = filter },
                                label = { Text(filter.name.replace("_", " "), fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    if (selectedAggFilter == ExpensePeriodFilter.CUSTOM) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                }
            }
        }

        item {
            Text(
                text = "Expense Log (${periodExpenses.size} items for $periodLabel):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(periodExpenses, key = { it.id }) { item ->
            ExpenseCardBlock(
                item = item,
                canEdit = canEditEntryOn(item.date),
                onEdit = { editingExpense = item },
                onDelete = {
                    onDeleteExpense(item)
                    ActivityLogger.log(session, "deleted expense of ₹${item.amount} (${item.description}) on ${item.date}")
                }
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
                        val selectedStr = sdf.format(Date(millis))
                        expenseDateInput = selectedStr
                        onDateSelected(selectedStr)
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

    editingExpense?.let { expense ->
        EditExpenseDetailsDialog(
            expense = expense,
            onDismiss = { editingExpense = null },
            onSave = { updatedExpense ->
                if (canEdit) {
                    onAddOrUpdateExpense(updatedExpense)
                    ActivityLogger.log(session, "edited expense of ₹${updatedExpense.amount} (${updatedExpense.description}) on ${updatedExpense.date}")
                }
                editingExpense = null
            }
        )
    }
}

@Composable
private fun ExpenseCardBlock(
    item: ExpenseItem,
    canEdit: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.description,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (item.timestamp.isNotBlank()) "Logged on ${item.date} @ ${item.timestamp}" else "Logged on ${item.date}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "₹ ${String.format(Locale.getDefault(), "%.2f", item.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )

                if (canEdit) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Expense",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Expense",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Expense Item?", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${item.description}' (₹ ${item.amount})?", fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EditExpenseDetailsDialog(
    expense: ExpenseItem,
    onDismiss: () -> Unit,
    onSave: (ExpenseItem) -> Unit
) {
    var descText by remember { mutableStateOf(expense.description) }
    var amountText by remember { mutableStateOf(expense.amount.toString()) }
    var dateText by remember { mutableStateOf(expense.date) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Edit Expense Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Description *", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val newAmount = round2(amountText.toDoubleOrNull() ?: expense.amount)
                        if (descText.isNotBlank() && newAmount > 0.0) {
                            val realWallClockTime = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                            onSave(
                                expense.copy(
                                    description = descText.trim(),
                                    amount = newAmount,
                                    date = dateText.trim(),
                                    timestamp = realWallClockTime
                                )
                            )
                        }
                    }) { Text("Save Changes", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditLedgerContent(
    session: AppUserSession,
    currentRecordDate: String,
    allCredits: List<CreditRecord>,
    allRecords: List<DailyFuelRecord>,
    onCustomerSelected: (CreditRecord) -> Unit,
    onAddOrUpdateCredit: (CreditRecord) -> Unit,
    onDeleteCredit: (CreditRecord) -> Unit,
    bottomInset: Dp
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CreditStatus.UNPAID) }
    var showAddCreditDialog by remember { mutableStateOf(false) }
    var prefilledCustomerCredit by remember { mutableStateOf<CreditRecord?>(null) }
    var isAddingNewDueToUser by remember { mutableStateOf(false) }
    var creditToSettle by remember { mutableStateOf<CreditRecord?>(null) }
    var editingCustomerInfoCredit by remember { mutableStateOf<CreditRecord?>(null) }

    val canEdit = !session.isReadOnly

    val totalOutstanding = remember(allCredits) {
        allCredits.sumOf { it.remainingBalance }
    }
    val givenToday = remember(allCredits, currentRecordDate) {
        allCredits.filter { it.date == currentRecordDate }.sumOf { it.totalAmountDue }
    }

    val filteredCredits = remember(allCredits, searchQuery, selectedFilter) {
        allCredits.filter { credit ->
            val matchesSearch = credit.customerName.contains(searchQuery, ignoreCase = true) ||
                    credit.mobileNumber.contains(searchQuery) ||
                    credit.vehicleNumber.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                CreditStatus.UNPAID -> credit.status == CreditStatus.UNPAID || credit.status == CreditStatus.PARTIAL
                CreditStatus.PAID -> credit.status == CreditStatus.PAID
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Total Outstanding", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("₹ ${String.format(Locale.getDefault(), "%.2f", totalOutstanding)}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Given Today ($currentRecordDate)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("₹ ${String.format(Locale.getDefault(), "%.2f", givenToday)}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Name, Mobile, Vehicle No.", fontSize = 10.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = selectedFilter == CreditStatus.UNPAID,
                    onClick = { selectedFilter = CreditStatus.UNPAID },
                    label = { Text("Pending Dues", fontSize = 10.sp) }
                )
                FilterChip(
                    selected = selectedFilter == CreditStatus.PAID,
                    onClick = { selectedFilter = CreditStatus.PAID },
                    label = { Text("Cleared / Paid", fontSize = 10.sp) }
                )
            }

            if (canEdit) {
                Button(
                    onClick = { 
                        prefilledCustomerCredit = null
                        isAddingNewDueToUser = false
                        showAddCreditDialog = true 
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Credit Entry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = bottomInset + 8.dp)
        ) {
            items(filteredCredits, key = { it.id }) { credit ->
                RestoredCreditCardItem(
                    credit = credit,
                    canEdit = canEdit,
                    onOpenDetail = { onCustomerSelected(credit) },
                    onAddNewDue = {
                        prefilledCustomerCredit = credit
                        isAddingNewDueToUser = true
                        showAddCreditDialog = true
                    },
                    onEditCustomerInfo = { editingCustomerInfoCredit = credit },
                    onSettle = { creditToSettle = credit },
                    onDelete = {
                        onDeleteCredit(credit)
                        ActivityLogger.log(session, "deleted credit ledger for ${credit.customerName} (${credit.vehicleNumber})")
                    }
                )
            }
        }
    }

    if (showAddCreditDialog && canEdit) {
        AddEditCreditDialog(
            session = session,
            currentRecordDate = currentRecordDate,
            initialCredit = prefilledCustomerCredit,
            isAddingNewDue = isAddingNewDueToUser,
            allRecords = allRecords,
            onDismiss = { showAddCreditDialog = false },
            onSave = { newCredit ->
                onAddOrUpdateCredit(newCredit)
                val logMsg = if (isAddingNewDueToUser) {
                    "added new due of ₹${newCredit.totalAmountDue} for ${newCredit.customerName} (${newCredit.vehicleNumber})"
                } else {
                    "added new credit entry for ${newCredit.customerName} (${newCredit.vehicleNumber}), due ₹${newCredit.totalAmountDue}"
                }
                ActivityLogger.log(session, logMsg)
                showAddCreditDialog = false
            }
        )
    }

    editingCustomerInfoCredit?.let { credit ->
        if (canEdit) {
            EditCustomerInfoDialog(
                credit = credit,
                onDismiss = { editingCustomerInfoCredit = null },
                onSave = { updatedCredit ->
                    onAddOrUpdateCredit(updatedCredit)
                    ActivityLogger.log(session, "updated customer info for ${updatedCredit.customerName} (${updatedCredit.vehicleNumber})")
                    editingCustomerInfoCredit = null
                }
            )
        }
    }

    creditToSettle?.let { credit ->
        if (canEdit) {
            SettleCreditDialog(
                credit = credit,
                currentRecordDate = currentRecordDate,
                onDismiss = { creditToSettle = null },
                onConfirmSettlement = { updatedCredit ->
                    onAddOrUpdateCredit(updatedCredit)
                    val paidNow = updatedCredit.amountPaid - credit.amountPaid
                    ActivityLogger.log(session, "recorded payment of ₹$paidNow from ${updatedCredit.customerName} (${updatedCredit.vehicleNumber}), balance ₹${updatedCredit.remainingBalance}")
                    creditToSettle = null
                }
            )
        }
    }
}

@Composable
private fun RestoredCreditCardItem(
    credit: CreditRecord,
    canEdit: Boolean = true,
    onOpenDetail: () -> Unit,
    onAddNewDue: () -> Unit,
    onEditCustomerInfo: () -> Unit,
    onSettle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val statusColor = when (credit.status) {
        CreditStatus.PAID -> Color(0xFF2E7D32)
        CreditStatus.PARTIAL -> Color(0xFFF57C00)
        CreditStatus.UNPAID -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable { onOpenDetail() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (credit.customerName.isNotBlank()) credit.customerName else "Customer (No Name)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Vehicle: ${if (credit.vehicleNumber.isNotBlank()) credit.vehicleNumber else "-"} | Mob: ${if (credit.mobileNumber.isNotBlank()) credit.mobileNumber else "-"}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = credit.status.name,
                        color = statusColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Date: ${credit.date}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val fuelInfo = buildString {
                        if (credit.petrolQuantityLitre > 0) append("Petrol: ${credit.petrolQuantityLitre}L ")
                        if (credit.dieselQuantityLitre > 0) append("Diesel: ${credit.dieselQuantityLitre}L")
                    }
                    if (fuelInfo.isNotBlank()) Text(fuelInfo, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Due: ₹ ${String.format(Locale.getDefault(), "%.2f", credit.totalAmountDue)}", fontSize = 10.sp)
                    Text("Paid: ₹ ${String.format(Locale.getDefault(), "%.2f", credit.amountPaid)}", fontSize = 10.sp, color = Color(0xFF2E7D32))
                    Text("Balance: ₹ ${String.format(Locale.getDefault(), "%.2f", credit.remainingBalance)}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = statusColor)
                }
            }

            if (canEdit) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Credit Record", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onEditCustomerInfo, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Customer Profile Info", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onAddNewDue,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Add New Due", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        if (credit.remainingBalance > 0.0) {
                            Button(
                                onClick = onSettle,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Record Payment", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Customer Record?", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${credit.customerName}?", fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerLedgerDetailScreen(
    session: AppUserSession,
    customer: CreditRecord,
    allRecords: List<DailyFuelRecord>,
    currentRecordDate: String,
    onBack: () -> Unit,
    onUpdateCustomer: (CreditRecord) -> Unit,
    onDeleteCustomer: () -> Unit,
    topInset: Dp,
    bottomInset: Dp
) {
    val context = LocalContext.current
    var showAddDueDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditCustomerDialog by remember { mutableStateOf(false) }
    var editingLogEntry by remember { mutableStateOf<InternalLogEntry?>(null) }

    val canEdit = !session.isReadOnly

    val parsedLogs = remember(customer.notes, customer.date, customer.totalAmountDue, customer.amountPaid) {
        val list = mutableListOf<InternalLogEntry>()
        
        if (customer.notes.isNotBlank()) {
            val lines = customer.notes.split("\n")
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim().removePrefix("•").trim()
                if (trimmed.isNotBlank()) {
                    val isPayment = trimmed.contains("Settlement Received", ignoreCase = true)
                    val label = if (isPayment) "Settlement Received" else "New Due Added"
                    val isCash = trimmed.contains("Cash", ignoreCase = true)
                    val isUpi = trimmed.contains("Digital", ignoreCase = true) || trimmed.contains("UPI", ignoreCase = true)
                    val mode = if (isCash) "Cash" else if (isUpi) "Digital (UPI)" else if (isPayment) "Cash / UPI" else "(Due)"

                    val amountRegex = Regex("₹\\s*([0-9]+\\.?[0-9]*)")
                    val parsedAmt = amountRegex.find(trimmed)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

                    val dateRegex = Regex("([0-9]{4}-[0-9]{2}-[0-9]{2})")
                    val dateMatch = dateRegex.find(trimmed)?.value ?: customer.date

                    val fullTsRegex = Regex("([0-9]{4}-[0-9]{2}-[0-9]{2}\\s+[0-9]{2}:[0-9]{2}\\s*[a|p]m)", RegexOption.IGNORE_CASE)
                    val fullTsMatch = fullTsRegex.find(trimmed)?.value

                    val simpleTimeRegex = Regex("([0-9]{2}:[0-9]{2}\\s*[a|p]m)", RegexOption.IGNORE_CASE)
                    val timeOnlyMatch = simpleTimeRegex.find(trimmed)?.value ?: ""

                    val resolvedTimestamp = fullTsMatch ?: if (timeOnlyMatch.isNotBlank()) "$dateMatch $timeOnlyMatch" else "$dateMatch 12:00 pm"

                    list.add(
                        InternalLogEntry(
                            id = "log_$index",
                            typeLabel = label,
                            isPayment = isPayment,
                            date = dateMatch,
                            timestamp = resolvedTimestamp,
                            amount = parsedAmt,
                            paymentMode = mode
                        )
                    )
                }
            }
        }

        val additionalDues = list.filter { !it.isPayment && it.typeLabel.contains("New Due", ignoreCase = true) }.sumOf { it.amount }
        val initialAmount = (customer.totalAmountDue - additionalDues).coerceAtLeast(0.0)

        val initialEntry = InternalLogEntry(
            id = "init_0",
            typeLabel = "Initial Credit Issued",
            isPayment = false,
            date = customer.date,
            timestamp = "${customer.date} 09:00 am",
            amount = initialAmount,
            paymentMode = "(Due)"
        )

        mutableListOf(initialEntry).apply { addAll(list) }
    }

    val initialCreditAmount = remember(parsedLogs) {
        parsedLogs.firstOrNull { it.id == "init_0" }?.amount ?: customer.totalAmountDue
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp)
    ) {
        Spacer(Modifier.height(topInset + 4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Customer Ledger", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { sendWhatsAppReminder(context, customer) },
                    enabled = customer.mobileNumber.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "WhatsApp",
                        tint = if (customer.mobileNumber.isNotBlank()) Color(0xFF25D366) else Color.Gray
                    )
                }

                IconButton(
                    onClick = { sendSmsReminder(context, customer) },
                    enabled = customer.mobileNumber.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = "SMS",
                        tint = if (customer.mobileNumber.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                if (canEdit) {
                    IconButton(onClick = { showEditCustomerDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Customer Info")
                    }

                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Customer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(customer.customerName.ifBlank { "Customer (No Name)" }, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("Date: ${customer.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vehicle: ${customer.vehicleNumber.ifBlank { "N/A" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Mobile: ${customer.mobileNumber.ifBlank { "N/A" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                val fuelSummary = buildString {
                    if (customer.petrolQuantityLitre > 0) append("Petrol: ${customer.petrolQuantityLitre}L  ")
                    if (customer.dieselQuantityLitre > 0) append("Diesel: ${customer.dieselQuantityLitre}L")
                }
                if (fuelSummary.isNotBlank()) {
                    Text(fuelSummary, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Credit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹ ${String.format(Locale.getDefault(), "%.2f", customer.totalAmountDue)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Column {
                        Text("Total Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹ ${String.format(Locale.getDefault(), "%.2f", customer.amountPaid)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Balance Due", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "₹ ${String.format(Locale.getDefault(), "%.2f", customer.remainingBalance)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (customer.remainingBalance > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        if (canEdit) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDueDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add New Due", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showRecordPaymentDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Record Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("• Initial Credit Issued on ${customer.date}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("- ₹ ${String.format(Locale.getDefault(), "%.2f", initialCreditAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }

                if (customer.amountPaid > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("• Total Settlement Received", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            if (customer.lastPaymentDate.isNotBlank()) {
                                Text(customer.lastPaymentDate, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("+ ₹ ${String.format(Locale.getDefault(), "%.2f", customer.amountPaid)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Detailed Log Entries", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Entry", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                    Text("Date", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Timestamp", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                    Text("Amount & Type", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                    if (canEdit) {
                        Text("Actions", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = bottomInset + 12.dp)
                ) {
                    items(parsedLogs, key = { it.id }) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1.3f)) {
                                Surface(
                                    color = if (log.isPayment) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = log.typeLabel,
                                        color = if (log.isPayment) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = log.date,
                                fontSize = 9.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = log.timestamp,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1.2f)
                            )

                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(
                                    text = "₹ ${String.format(Locale.getDefault(), "%.0f", log.amount)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (log.isPayment) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                Text(
                                    text = log.paymentMode,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (canEdit) {
                                Row(
                                    modifier = Modifier.width(52.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { editingLogEntry = log },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(13.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val newLogList = parsedLogs.filter { it.id != log.id && it.id != "init_0" }
                                            val recomputedNotes = newLogList.joinToString("\n") {
                                                "• ${it.typeLabel}: ₹ ${String.format(Locale.US, "%.2f", it.amount)} on ${it.date} @ ${it.timestamp} (${it.paymentMode})"
                                            }
                                            val newPaid = round2(newLogList.filter { it.isPayment }.sumOf { it.amount })
                                            val newTotalDue = round2(newLogList.filter { !it.isPayment }.sumOf { it.amount } + initialCreditAmount)
                                            onUpdateCustomer(customer.copy(notes = recomputedNotes, amountPaid = newPaid, totalAmountDue = newTotalDue))
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    if (showAddDueDialog && canEdit) {
        AddEditCreditDialog(
            session = session,
            currentRecordDate = currentRecordDate,
            initialCredit = customer,
            isAddingNewDue = true,
            allRecords = allRecords,
            onDismiss = { showAddDueDialog = false },
            onSave = { updated ->
                onUpdateCustomer(updated)
                showAddDueDialog = false
            }
        )
    }

    if (showRecordPaymentDialog && canEdit) {
        SettleCreditDialog(
            credit = customer,
            currentRecordDate = currentRecordDate,
            onDismiss = { showRecordPaymentDialog = false },
            onConfirmSettlement = { updated ->
                onUpdateCustomer(updated)
                showRecordPaymentDialog = false
            }
        )
    }

    if (editingLogEntry != null && canEdit) {
        val entry = editingLogEntry!!
        EditLogEntryModal(
            entry = entry,
            onDismiss = { editingLogEntry = null },
            onSave = { updatedAmt, updatedDate, updatedNote ->
                val originalTimestamp = entry.timestamp
                if (entry.id == "init_0") {
                    val additionalDues = parsedLogs.filter { !it.isPayment && it.id != "init_0" }.sumOf { it.amount }
                    val newTotalDue = round2(updatedAmt + additionalDues)
                    onUpdateCustomer(customer.copy(date = updatedDate, totalAmountDue = newTotalDue))
                } else {
                    val updatedLogs = parsedLogs.filter { it.id != "init_0" }.map {
                        if (it.id == entry.id) it.copy(amount = updatedAmt, date = updatedDate, paymentMode = updatedNote, timestamp = originalTimestamp) else it
                    }
                    val recomputedNotes = updatedLogs.joinToString("\n") {
                        "• ${it.typeLabel}: ₹ ${String.format(Locale.US, "%.2f", it.amount)} on ${it.date} @ ${it.timestamp} (${it.paymentMode})"
                    }
                    val newPaid = round2(updatedLogs.filter { it.isPayment }.sumOf { it.amount })
                    val newTotalDue = round2(updatedLogs.filter { !it.isPayment }.sumOf { it.amount } + initialCreditAmount)
                    onUpdateCustomer(customer.copy(notes = recomputedNotes, amountPaid = newPaid, totalAmountDue = newTotalDue))
                }
                editingLogEntry = null
            }
        )
    }

    if (showEditCustomerDialog && canEdit) {
        EditCustomerInfoDialog(
            credit = customer,
            onDismiss = { showEditCustomerDialog = false },
            onSave = { updatedCust ->
                onUpdateCustomer(updatedCust)
                showEditCustomerDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog && canEdit) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Customer Record?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${customer.customerName}? All transaction history will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    onDeleteCustomer()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EditLogEntryModal(
    entry: InternalLogEntry,
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf(entry.amount.toString()) }
    var dateText by remember { mutableStateOf(entry.date) }
    var noteText by remember { mutableStateOf(entry.paymentMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Log Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Type / Description", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = round2(amountText.toDoubleOrNull() ?: entry.amount)
                    if (parsed > 0.0) {
                        onSave(parsed, dateText.trim(), noteText.trim())
                    }
                }
            ) { Text("Save Changes", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditCustomerInfoDialog(
    credit: CreditRecord,
    onDismiss: () -> Unit,
    onSave: (CreditRecord) -> Unit
) {
    var customerName by remember { mutableStateOf(credit.customerName) }
    var vehicleNo by remember { mutableStateOf(credit.vehicleNumber) }
    var mobileNo by remember { mutableStateOf(credit.mobileNumber) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Edit Customer Profile Info", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vehicleNo,
                    onValueChange = { vehicleNo = it },
                    label = { Text("Vehicle Number", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobileNo,
                    onValueChange = { mobileNo = it },
                    label = { Text("Mobile Number", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onSave(
                            credit.copy(
                                customerName = customerName.trim(),
                                vehicleNumber = vehicleNo.trim(),
                                mobileNumber = mobileNo.trim()
                            )
                        )
                    }) { Text("Save Info", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCreditDialog(
    session: AppUserSession,
    currentRecordDate: String,
    initialCredit: CreditRecord? = null,
    isEditing: Boolean = false,
    isAddingNewDue: Boolean = false,
    allRecords: List<DailyFuelRecord> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (CreditRecord) -> Unit
) {
    var entryDate by remember { mutableStateOf(currentRecordDate) }
    var vehicleNo by remember { mutableStateOf(initialCredit?.vehicleNumber ?: "") }
    var customerName by remember { mutableStateOf(initialCredit?.customerName ?: "") }
    var mobileNo by remember { mutableStateOf(initialCredit?.mobileNumber ?: "") }
    var selectedFuelType by remember { mutableStateOf(initialCredit?.fuelType ?: CreditFuelType.PETROL) }

    var petrolLitreText by remember { mutableStateOf("") }
    var dieselLitreText by remember { mutableStateOf("") }
    var addedAmountText by remember { mutableStateOf("") }

    var showEntryDatePicker by remember { mutableStateOf(false) }
    var isCalculatingInternal by remember { mutableStateOf(false) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isPastDate = entryDate < todayStr
    val isDayFinalized = allRecords.find { it.date == entryDate }?.shift3?.isComplete == true

    val hasPastPrivilege = session.canEditPastDates || session.canEditFinancePastDates || session.isOwnerLogin || session.role != Role.MANAGER
    val canEdit = !session.isReadOnly &&
            (!isDayFinalized || hasPastPrivilege) &&
            (!isPastDate || hasPastPrivilege)

    val fuelRates = remember(entryDate, allRecords) {
        val exactRecord = allRecords.find { it.date == entryDate }
        val pPrice = exactRecord?.petrolPrice ?: 0.0
        val dPrice = exactRecord?.dieselPrice ?: 0.0

        val fallbackP = if (pPrice > 0.0) pPrice else allRecords.filter { it.petrolPrice > 0.0 }.maxByOrNull { it.date }?.petrolPrice ?: 100.0
        val fallbackD = if (dPrice > 0.0) dPrice else allRecords.filter { it.dieselPrice > 0.0 }.maxByOrNull { it.date }?.dieselPrice ?: 90.0

        Pair(fallbackP, fallbackD)
    }

    val petrolRate = fuelRates.first
    val dieselRate = fuelRates.second

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isEditing) "Edit Credit Entry" else if (isAddingNewDue && initialCredit != null) "Add New Due for ${initialCredit.customerName}" else "New Credit / Lend Entry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (!canEdit) {
                    Text(
                        text = if (isDayFinalized) "Day Finalized: Dues for $entryDate are locked." else "Past Date Locked: You do not have permission to log dues for past dates.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Transaction Date: $entryDate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { showEntryDatePicker = true }) {
                        Text("Change Date", fontSize = 11.sp)
                    }
                }

                if (!isAddingNewDue) {
                    OutlinedTextField(
                        value = vehicleNo,
                        onValueChange = { vehicleNo = it },
                        label = { Text("Vehicle No. (e.g. WB26A1234)", fontSize = 10.sp) },
                        singleLine = true,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name", fontSize = 10.sp) },
                        singleLine = true,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobileNo,
                        onValueChange = { mobileNo = it },
                        label = { Text("Mobile Number", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = selectedFuelType == CreditFuelType.PETROL, onClick = { if (canEdit) selectedFuelType = CreditFuelType.PETROL }, label = { Text("Petrol (₹ $petrolRate/L)", fontSize = 9.sp) }, enabled = canEdit)
                    FilterChip(selected = selectedFuelType == CreditFuelType.DIESEL, onClick = { if (canEdit) selectedFuelType = CreditFuelType.DIESEL }, label = { Text("Diesel (₹ $dieselRate/L)", fontSize = 9.sp) }, enabled = canEdit)
                    FilterChip(selected = selectedFuelType == CreditFuelType.BOTH, onClick = { if (canEdit) selectedFuelType = CreditFuelType.BOTH }, label = { Text("Both", fontSize = 9.sp) }, enabled = canEdit)
                }

                if (selectedFuelType == CreditFuelType.PETROL || selectedFuelType == CreditFuelType.BOTH) {
                    OutlinedTextField(
                        value = petrolLitreText,
                        onValueChange = { input ->
                            petrolLitreText = input
                            if (!isCalculatingInternal && canEdit) {
                                isCalculatingInternal = true
                                val litres = input.toDoubleOrNull() ?: 0.0
                                val calculated = round2(litres * petrolRate)
                                addedAmountText = if (calculated > 0.0) String.format(Locale.US, "%.2f", calculated) else ""
                                isCalculatingInternal = false
                            }
                        },
                        label = { Text("Petrol Litres (L)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedFuelType == CreditFuelType.DIESEL || selectedFuelType == CreditFuelType.BOTH) {
                    OutlinedTextField(
                        value = dieselLitreText,
                        onValueChange = { input ->
                            dieselLitreText = input
                            if (!isCalculatingInternal && canEdit) {
                                isCalculatingInternal = true
                                val litres = input.toDoubleOrNull() ?: 0.0
                                val calculated = round2(litres * dieselRate)
                                addedAmountText = if (calculated > 0.0) String.format(Locale.US, "%.2f", calculated) else ""
                                isCalculatingInternal = false
                            }
                        },
                        label = { Text("Diesel Litres (L)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = addedAmountText,
                    onValueChange = { input ->
                        addedAmountText = input
                        if (!isCalculatingInternal && canEdit) {
                            isCalculatingInternal = true
                            val amount = input.toDoubleOrNull() ?: 0.0
                            when (selectedFuelType) {
                                CreditFuelType.PETROL -> {
                                    val litres = if (petrolRate > 0) round2(amount / petrolRate) else 0.0
                                    petrolLitreText = if (litres > 0.0) String.format(Locale.US, "%.2f", litres) else ""
                                }
                                CreditFuelType.DIESEL -> {
                                    val litres = if (dieselRate > 0) round2(amount / dieselRate) else 0.0
                                    dieselLitreText = if (litres > 0.0) String.format(Locale.US, "%.2f", litres) else ""
                                }
                                else -> {}
                            }
                            isCalculatingInternal = false
                        }
                    },
                    label = { Text(if (isAddingNewDue) "New Due Amount (₹)" else "Total Amount Due (₹)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = canEdit,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val enteredDue = round2(addedAmountText.toDoubleOrNull() ?: 0.0)
                            if (enteredDue > 0.0 && canEdit) {
                                val realWallClockTimestamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                                val record = if (isAddingNewDue && initialCredit != null) {
                                    val newNoteLog = buildString {
                                        if (initialCredit.notes.isNotBlank()) append("${initialCredit.notes}\n")
                                        append("• New Due Added: ₹ ${String.format(Locale.US, "%.2f", enteredDue)} on $entryDate @ $realWallClockTimestamp")
                                    }
                                    initialCredit.copy(
                                        date = entryDate,
                                        petrolQuantityLitre = round2(initialCredit.petrolQuantityLitre + (petrolLitreText.toDoubleOrNull() ?: 0.0)),
                                        dieselQuantityLitre = round2(initialCredit.dieselQuantityLitre + (dieselLitreText.toDoubleOrNull() ?: 0.0)),
                                        totalAmountDue = round2(initialCredit.totalAmountDue + enteredDue),
                                        notes = newNoteLog
                                    )
                                } else {
                                    (initialCredit ?: CreditRecord(id = System.currentTimeMillis(), date = entryDate)).copy(
                                        id = initialCredit?.id ?: System.currentTimeMillis(),
                                        date = entryDate,
                                        vehicleNumber = vehicleNo.trim(),
                                        customerName = customerName.trim(),
                                        mobileNumber = mobileNo.trim(),
                                        fuelType = selectedFuelType,
                                        petrolQuantityLitre = round2(petrolLitreText.toDoubleOrNull() ?: 0.0),
                                        dieselQuantityLitre = round2(dieselLitreText.toDoubleOrNull() ?: 0.0),
                                        totalAmountDue = enteredDue
                                    )
                                }
                                onSave(record)
                            }
                        },
                        enabled = canEdit
                    ) { Text("Save Entry", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (showEntryDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEntryDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showEntryDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        entryDate = sdf.format(Date(millis))
                    }
                }) { Text("Select Date", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEntryDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettleCreditDialog(
    credit: CreditRecord,
    currentRecordDate: String,
    onDismiss: () -> Unit,
    onConfirmSettlement: (CreditRecord) -> Unit
) {
    var paymentAmountText by remember { mutableStateOf("") }
    var selectedPaymentMode by remember { mutableStateOf("Cash") }
    var settlementDate by remember { mutableStateOf(currentRecordDate) }
    var showSettlementDatePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Record Payment Settlement", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Text("Customer: ${credit.customerName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Remaining Balance: ₹ ${String.format(Locale.getDefault(), "%.2f", credit.remainingBalance)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Settlement Date: $settlementDate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { showSettlementDatePicker = true }) {
                        Text("Change Date", fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedPaymentMode == "Cash",
                        onClick = { selectedPaymentMode = "Cash" },
                        label = { Text("Cash", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedPaymentMode == "Digital (UPI)",
                        onClick = { selectedPaymentMode = "Digital (UPI)" },
                        label = { Text("Digital (UPI)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { paymentAmountText = it },
                    label = { Text("Amount Received (₹)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val addedPayment = round2(paymentAmountText.toDoubleOrNull() ?: 0.0)
                        if (addedPayment > 0.0) {
                            val realWallClockTimestamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                            val newNoteLog = buildString {
                                if (credit.notes.isNotBlank()) append("${credit.notes}\n")
                                append("• Settlement Received: ₹ ${String.format(Locale.US, "%.2f", addedPayment)} on $settlementDate @ $realWallClockTimestamp ($selectedPaymentMode)")
                            }
                            val updated = credit.copy(
                                amountPaid = round2(credit.amountPaid + addedPayment),
                                lastPaymentDate = settlementDate,
                                notes = newNoteLog
                            )
                            onConfirmSettlement(updated)
                        }
                    }) { Text("Confirm Payment", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (showSettlementDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showSettlementDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showSettlementDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        settlementDate = sdf.format(Date(millis))
                    }
                }) { Text("Select Date", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSettlementDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun sendWhatsAppReminder(context: Context, customer: CreditRecord) {
    if (customer.mobileNumber.isBlank()) return

    val cleanPhone = customer.mobileNumber.replace(Regex("[^0-9]"), "")
    val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

    val message = """
⛽ *NH FUEL STATION*
--------------------------------
Hello *${customer.customerName}*,

আপনার কাছে প্রাপ্য বকেয়া টাকার বিষয়ে এটি একটি বিনীত স্মরণিকা।

📋 *গ্রাহকের বিবরণ:*
• *নাম:* ${customer.customerName}
• *গাড়ির নম্বর:* ${customer.vehicleNumber.ifBlank { "N/A" }}
• *মোট বকেয়া:* ₹ ${String.format(Locale.getDefault(), "%.2f", customer.totalAmountDue)}
• *মোট পেমেন্ট করা হয়েছে:* ₹ ${String.format(Locale.getDefault(), "%.2f", customer.amountPaid)}

💰 *বর্তমানে বকেয়া রয়েছে: ₹ ${String.format(Locale.getDefault(), "%.2f", customer.remainingBalance)}*

অনুগ্ৰহ করে আপনার সুবিধামতো যত দ্রুত সম্ভব বকেয়া পরিশোধের ব্যবস্থা করুন।

ধন্যবাদ!
--------------------------------
NH Fuel Station
    """.trimIndent()

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show()
    }
}

private fun sendSmsReminder(context: Context, customer: CreditRecord) {
    if (customer.mobileNumber.isBlank()) return

    val message = "NH Fuel Station Reminder: Dear ${customer.customerName}, your current fuel credit balance due is Rs. ${String.format(Locale.getDefault(), "%.2f", customer.remainingBalance)} (Vehicle: ${customer.vehicleNumber.ifBlank { "N/A" }}). Kindly clear the pending dues. Thank you!"

    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${customer.mobileNumber}")
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch SMS app.", Toast.LENGTH_SHORT).show()
    }
}
