package com.nh.fuel.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nh.fuel.data.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExpensePeriodFilter { ALL_TIME, THIS_MONTH, THIS_YEAR, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    currentRecordDate: String,
    allExpenses: List<ExpenseItem> = emptyList(),
    allCredits: List<CreditRecord> = emptyList(),
    onAddOrUpdateExpense: (ExpenseItem) -> Unit,
    onDeleteExpense: (ExpenseItem) -> Unit,
    onAddOrUpdateCredit: (CreditRecord) -> Unit,
    onDeleteCredit: (CreditRecord) -> Unit,
    onDateSelected: (String) -> Unit = {},
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Daily Expenses, 1: Credit / Lend Ledger

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
                currentRecordDate = currentRecordDate,
                allExpenses = allExpenses,
                onAddOrUpdateExpense = onAddOrUpdateExpense,
                onDeleteExpense = onDeleteExpense,
                onDateSelected = onDateSelected,
                bottomInset = bottomInset
            )
            1 -> CreditLedgerContent(
                currentRecordDate = currentRecordDate,
                allCredits = allCredits,
                onAddOrUpdateCredit = onAddOrUpdateCredit,
                onDeleteCredit = onDeleteCredit,
                bottomInset = bottomInset
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpendScreenContent(
    currentRecordDate: String,
    allExpenses: List<ExpenseItem>,
    onAddOrUpdateExpense: (ExpenseItem) -> Unit,
    onDeleteExpense: (ExpenseItem) -> Unit,
    onDateSelected: (String) -> Unit,
    bottomInset: Dp
) {
    var descriptionInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var expenseDateInput by remember(currentRecordDate) { mutableStateOf(currentRecordDate) }
    var showDatePickerModal by remember { mutableStateOf(false) }
    var showTopDatePickerModal by remember { mutableStateOf(false) }

    var editingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var selectedAggFilter by remember { mutableStateOf(ExpensePeriodFilter.ALL_TIME) }
    var customFromDate by remember { mutableStateOf(currentRecordDate) }
    var customToDate by remember { mutableStateOf(currentRecordDate) }

    fun navigateDate(daysOffset: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = try { sdf.parse(currentRecordDate) ?: Date() } catch (e: Exception) { Date() }
        val cal = Calendar.getInstance().apply {
            time = parsedDate
            add(Calendar.DAY_OF_MONTH, daysOffset)
        }
        onDateSelected(sdf.format(cal.time))
    }

    val dayExpenses = remember(allExpenses, currentRecordDate) {
        allExpenses.filter { it.date == currentRecordDate }
    }
    val totalDayExpense = remember(dayExpenses) {
        dayExpenses.sumOf { it.amount }
    }

    val aggregatedExpenseTotal = remember(allExpenses, selectedAggFilter, currentRecordDate, customFromDate, customToDate) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        when (selectedAggFilter) {
            ExpensePeriodFilter.ALL_TIME -> allExpenses.sumOf { it.amount }
            ExpensePeriodFilter.THIS_MONTH -> {
                val currentMonth = currentRecordDate.take(7)
                allExpenses.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
            }
            ExpensePeriodFilter.THIS_YEAR -> {
                val currentYear = currentRecordDate.take(4)
                allExpenses.filter { it.date.startsWith(currentYear) }.sumOf { it.amount }
            }
            ExpensePeriodFilter.CUSTOM -> {
                val fromD = try { sdf.parse(customFromDate) } catch (e: Exception) { null }
                val toD = try { sdf.parse(customToDate) } catch (e: Exception) { null }
                if (fromD != null && toD != null) {
                    allExpenses.filter { exp ->
                        val expD = try { sdf.parse(exp.date) } catch (e: Exception) { null }
                        expD != null && !expD.before(fromD) && !expD.after(toD)
                    }.sumOf { it.amount }
                } else allExpenses.sumOf { it.amount }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = bottomInset + 12.dp)
    ) {
        // Date Selector Row
        item {
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
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Day", modifier = Modifier.size(16.dp))
                    Text("Prev", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentRecordDate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { showTopDatePickerModal = true },
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
                    Text("Next", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", modifier = Modifier.size(16.dp))
                }
            }
        }

        // Add New Expense Card
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
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = expenseDateInput,
                            onValueChange = { expenseDateInput = it },
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
                            val amount = amountInput.toDoubleOrNull() ?: 0.0
                            if (descriptionInput.isNotBlank() && amount > 0.0) {
                                val nowTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                onAddOrUpdateExpense(
                                    ExpenseItem(
                                        date = expenseDateInput.ifBlank { currentRecordDate },
                                        description = descriptionInput.trim(),
                                        amount = amount,
                                        timestamp = nowTimeStr
                                    )
                                )
                                descriptionInput = ""
                                amountInput = ""
                            }
                        },
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

        // Aggregated Expenses Summary Card
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
                            text = "Aggregated Expenses ($currentRecordDate):",
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
                text = "Expense Log (${dayExpenses.size} items for $currentRecordDate):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Expense Item Card Blocks
        items(dayExpenses, key = { it.id }) { item ->
            ExpenseCardBlock(
                item = item,
                onEdit = { editingExpense = item },
                onDelete = { onDeleteExpense(item) }
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
                        expenseDateInput = sdf.format(Date(millis))
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

    if (showTopDatePickerModal) {
        val topDatePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showTopDatePickerModal = false },
            confirmButton = {
                TextButton(onClick = {
                    showTopDatePickerModal = false
                    topDatePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        onDateSelected(sdf.format(Date(millis)))
                    }
                }) { Text("Select Date", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTopDatePickerModal = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = topDatePickerState)
        }
    }

    editingExpense?.let { expense ->
        EditExpenseDetailsDialog(
            expense = expense,
            onDismiss = { editingExpense = null },
            onSave = { updatedExpense ->
                onAddOrUpdateExpense(updatedExpense)
                editingExpense = null
            }
        )
    }
}

@Composable
private fun ExpenseCardBlock(
    item: ExpenseItem,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Expense Details", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
            }
        },
        confirmButton = {
            Button(onClick = {
                val newAmount = amountText.toDoubleOrNull() ?: expense.amount
                if (descText.isNotBlank() && newAmount > 0.0) {
                    onSave(
                        expense.copy(
                            description = descText.trim(),
                            amount = newAmount,
                            date = dateText.trim()
                        )
                    )
                }
            }) { Text("Save Changes", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditLedgerContent(
    currentRecordDate: String,
    allCredits: List<CreditRecord>,
    onAddOrUpdateCredit: (CreditRecord) -> Unit,
    onDeleteCredit: (CreditRecord) -> Unit,
    bottomInset: Dp
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CreditStatus.UNPAID) }
    var showAddCreditDialog by remember { mutableStateOf(false) }
    var prefilledCustomerCredit by remember { mutableStateOf<CreditRecord?>(null) }
    var editingCredit by remember { mutableStateOf<CreditRecord?>(null) }
    var creditToSettle by remember { mutableStateOf<CreditRecord?>(null) }

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

            Button(
                onClick = { 
                    prefilledCustomerCredit = null
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

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = bottomInset + 8.dp)
        ) {
            items(filteredCredits, key = { it.id }) { credit ->
                CreditCardItem(
                    credit = credit,
                    onAddMoreCredit = {
                        prefilledCustomerCredit = credit
                        showAddCreditDialog = true
                    },
                    onEdit = { editingCredit = credit },
                    onSettle = { creditToSettle = credit },
                    onDelete = { onDeleteCredit(credit) }
                )
            }
        }
    }

    if (showAddCreditDialog) {
        AddEditCreditDialog(
            currentRecordDate = currentRecordDate,
            initialCredit = prefilledCustomerCredit,
            onDismiss = { showAddCreditDialog = false },
            onSave = { newCredit ->
                onAddOrUpdateCredit(newCredit)
                showAddCreditDialog = false
            }
        )
    }

    editingCredit?.let { credit ->
        AddEditCreditDialog(
            currentRecordDate = currentRecordDate,
            initialCredit = credit,
            isEditing = true,
            onDismiss = { editingCredit = null },
            onSave = { updatedCredit ->
                onAddOrUpdateCredit(updatedCredit)
                editingCredit = null
            }
        )
    }

    creditToSettle?.let { credit ->
        SettleCreditDialog(
            credit = credit,
            onDismiss = { creditToSettle = null },
            onConfirmSettlement = { updatedCredit ->
                onAddOrUpdateCredit(updatedCredit)
                creditToSettle = null
            }
        )
    }
}

@Composable
private fun CreditCardItem(
    credit: CreditRecord,
    onAddMoreCredit: () -> Unit,
    onEdit: () -> Unit,
    onSettle: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val statusColor = when (credit.status) {
        CreditStatus.PAID -> Color(0xFF2E7D32)
        CreditStatus.PARTIAL -> Color(0xFFF57C00)
        CreditStatus.UNPAID -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded },
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
                        fontSize = 13.sp,
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

            // Expanded Customer Payment & Transaction Log
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    Text("Transaction & Settlement History Log:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    Text("• Credit Issued: ₹ ${credit.totalAmountDue} on ${credit.date}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    if (credit.lastPaymentDate.isNotBlank()) {
                        Text("• Last Settlement Received: ₹ ${credit.amountPaid} @ ${credit.lastPaymentDate}", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("• No settlements recorded yet.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (credit.notes.isNotBlank()) {
                        Text("• Notes: ${credit.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Credit", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Entry", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onAddMoreCredit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("+ Add Credit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Credit Record?", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this credit entry for ${credit.customerName}?", fontSize = 12.sp) },
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
private fun AddEditCreditDialog(
    currentRecordDate: String,
    initialCredit: CreditRecord? = null,
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (CreditRecord) -> Unit
) {
    var entryDate by remember { mutableStateOf(initialCredit?.date ?: currentRecordDate) }
    var vehicleNo by remember { mutableStateOf(initialCredit?.vehicleNumber ?: "") }
    var customerName by remember { mutableStateOf(initialCredit?.customerName ?: "") }
    var mobileNo by remember { mutableStateOf(initialCredit?.mobileNumber ?: "") }
    var selectedFuelType by remember { mutableStateOf(initialCredit?.fuelType ?: CreditFuelType.PETROL) }
    var petrolLitreText by remember { mutableStateOf(if ((initialCredit?.petrolQuantityLitre ?: 0.0) > 0) initialCredit?.petrolQuantityLitre.toString() else "") }
    var dieselLitreText by remember { mutableStateOf(if ((initialCredit?.dieselQuantityLitre ?: 0.0) > 0) initialCredit?.dieselQuantityLitre.toString() else "") }
    var totalAmountText by remember { mutableStateOf(if (isEditing) initialCredit?.totalAmountDue.toString() else "") }
    var initialPaidText by remember { mutableStateOf(if (isEditing) initialCredit?.amountPaid.toString() else "") }
    var showEntryDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (isEditing) "Edit Credit Entry" else if (initialCredit != null) "Add Credit for ${initialCredit.customerName}" else "New Credit / Lend Entry", 
                fontWeight = FontWeight.Bold, 
                fontSize = 15.sp
            ) 
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Date: $entryDate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { showEntryDatePicker = true }) {
                        Text("Change Date", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = vehicleNo, 
                    onValueChange = { vehicleNo = it }, 
                    label = { Text("Vehicle No. (e.g. WB26A1234)", fontSize = 9.sp) }, 
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customerName, 
                    onValueChange = { customerName = it }, 
                    label = { Text("Customer Name", fontSize = 9.sp) }, 
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobileNo, 
                    onValueChange = { mobileNo = it }, 
                    label = { Text("Mobile Number", fontSize = 9.sp) }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), 
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = selectedFuelType == CreditFuelType.PETROL, onClick = { selectedFuelType = CreditFuelType.PETROL }, label = { Text("Petrol", fontSize = 9.sp) })
                    FilterChip(selected = selectedFuelType == CreditFuelType.DIESEL, onClick = { selectedFuelType = CreditFuelType.DIESEL }, label = { Text("Diesel", fontSize = 9.sp) })
                    FilterChip(selected = selectedFuelType == CreditFuelType.BOTH, onClick = { selectedFuelType = CreditFuelType.BOTH }, label = { Text("Both", fontSize = 9.sp) })
                }

                if (selectedFuelType == CreditFuelType.PETROL || selectedFuelType == CreditFuelType.BOTH) {
                    OutlinedTextField(
                        value = petrolLitreText, 
                        onValueChange = { petrolLitreText = it }, 
                        label = { Text("Petrol Litres (L)", fontSize = 9.sp) }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (selectedFuelType == CreditFuelType.DIESEL || selectedFuelType == CreditFuelType.BOTH) {
                    OutlinedTextField(
                        value = dieselLitreText, 
                        onValueChange = { dieselLitreText = it }, 
                        label = { Text("Diesel Litres (L)", fontSize = 9.sp) }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = totalAmountText, 
                    onValueChange = { totalAmountText = it }, 
                    label = { Text("Total Amount Due (₹)", fontSize = 9.sp) }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (!isEditing) {
                    OutlinedTextField(
                        value = initialPaidText, 
                        onValueChange = { initialPaidText = it }, 
                        label = { Text("Initial Down Payment (Optional ₹)", fontSize = 9.sp) }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val totalDue = totalAmountText.toDoubleOrNull() ?: 0.0
                val initialPaid = initialPaidText.toDoubleOrNull() ?: 0.0
                if (totalDue > 0.0) {
                    val record = (initialCredit ?: CreditRecord(date = entryDate)).copy(
                        date = entryDate,
                        vehicleNumber = vehicleNo.trim(),
                        customerName = customerName.trim(),
                        mobileNumber = mobileNo.trim(),
                        fuelType = selectedFuelType,
                        petrolQuantityLitre = petrolLitreText.toDoubleOrNull() ?: 0.0,
                        dieselQuantityLitre = dieselLitreText.toDoubleOrNull() ?: 0.0,
                        totalAmountDue = totalDue,
                        amountPaid = if (isEditing) initialCredit?.amountPaid ?: 0.0 else initialPaid,
                        lastPaymentDate = if (initialPaid > 0.0 && !isEditing) SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date()) else initialCredit?.lastPaymentDate ?: ""
                    )
                    onSave(record)
                }
            }) { Text("Save Entry", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

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

@Composable
private fun SettleCreditDialog(
    credit: CreditRecord,
    onDismiss: () -> Unit,
    onConfirmSettlement: (CreditRecord) -> Unit
) {
    var paymentAmountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment Settlement", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Customer: ${credit.customerName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Remaining Balance: ₹ ${String.format(Locale.getDefault(), "%.2f", credit.remainingBalance)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { paymentAmountText = it },
                    label = { Text("Amount Received (₹)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val addedPayment = paymentAmountText.toDoubleOrNull() ?: 0.0
                if (addedPayment > 0.0) {
                    val nowStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
                    val updated = credit.copy(
                        amountPaid = credit.amountPaid + addedPayment,
                        lastPaymentDate = nowStr
                    )
                    onConfirmSettlement(updated)
                }
            }) { Text("Confirm Payment", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
