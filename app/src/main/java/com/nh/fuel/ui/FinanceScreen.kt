package com.nh.fuel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Expenses, 1: Credit / Lend
    var showDatePickerModal by remember { mutableStateOf(false) }

    fun navigateDate(daysOffset: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = try { sdf.parse(currentRecordDate) ?: Date() } catch (e: Exception) { Date() }
        val cal = Calendar.getInstance().apply {
            time = parsedDate
            add(Calendar.DAY_OF_MONTH, daysOffset)
        }
        onDateSelected(sdf.format(cal.time))
    }

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

        // Date Picker Bar for Finance Tab
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
                Text("Next", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", modifier = Modifier.size(16.dp))
            }
        }

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
}

@Composable
fun ExpendScreenContent(
    currentRecordDate: String,
    allExpenses: List<ExpenseItem>,
    onAddOrUpdateExpense: (ExpenseItem) -> Unit,
    onDeleteExpense: (ExpenseItem) -> Unit,
    bottomInset: Dp
) {
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }

    val dayExpenses = remember(allExpenses, currentRecordDate) {
        allExpenses.filter { it.date == currentRecordDate }
    }
    val totalDayExpense = remember(dayExpenses) {
        dayExpenses.sumOf { it.amount }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Total Expenses ($currentRecordDate)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("₹ ${String.format(Locale.getDefault(), "%.2f", totalDayExpense)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Add New Expense", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Expense Title", fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (₹)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull() ?: 0.0
                        if (titleInput.isNotBlank() && amount > 0.0) {
                            onAddOrUpdateExpense(
                                ExpenseItem(
                                    date = currentRecordDate,
                                    description = titleInput.trim(),
                                    amount = amount
                                )
                            )
                            titleInput = ""
                            amountInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Add Expense", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = bottomInset + 8.dp)
        ) {
            items(dayExpenses, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.description, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("₹ ${String.format(Locale.getDefault(), "%.2f", item.amount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            IconButton(onClick = { onDeleteExpense(item) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Expense", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
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
                onClick = { showAddCreditDialog = true },
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
                    onSettle = { creditToSettle = credit },
                    onDelete = { onDeleteCredit(credit) }
                )
            }
        }
    }

    if (showAddCreditDialog) {
        AddEditCreditDialog(
            currentRecordDate = currentRecordDate,
            onDismiss = { showAddCreditDialog = false },
            onSave = { newCredit ->
                onAddOrUpdateCredit(newCredit)
                showAddCreditDialog = false
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
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Credit", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }

                if (credit.remainingBalance > 0.0) {
                    OutlinedButton(
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
    onDismiss: () -> Unit,
    onSave: (CreditRecord) -> Unit
) {
    var entryDate by remember { mutableStateOf(currentRecordDate) }
    var vehicleNo by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var mobileNo by remember { mutableStateOf("") }
    var selectedFuelType by remember { mutableStateOf(CreditFuelType.PETROL) }
    var petrolLitreText by remember { mutableStateOf("") }
    var dieselLitreText by remember { mutableStateOf("") }
    var totalAmountText by remember { mutableStateOf("") }
    var initialPaidText by remember { mutableStateOf("") }
    var showEntryDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Credit / Lend Entry", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

                OutlinedTextField(value = vehicleNo, onValueChange = { vehicleNo = it }, label = { Text("Vehicle No. (e.g. WB26A1234)", fontSize = 9.sp) }, singleLine = true)
                OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name", fontSize = 9.sp) }, singleLine = true)
                OutlinedTextField(value = mobileNo, onValueChange = { mobileNo = it }, label = { Text("Mobile Number", fontSize = 9.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = selectedFuelType == CreditFuelType.PETROL, onClick = { selectedFuelType = CreditFuelType.PETROL }, label = { Text("Petrol", fontSize = 9.sp) })
                    FilterChip(selected = selectedFuelType == CreditFuelType.DIESEL, onClick = { selectedFuelType = CreditFuelType.DIESEL }, label = { Text("Diesel", fontSize = 9.sp) })
                    FilterChip(selected = selectedFuelType == CreditFuelType.BOTH, onClick = { selectedFuelType = CreditFuelType.BOTH }, label = { Text("Both", fontSize = 9.sp) })
                }

                if (selectedFuelType == CreditFuelType.PETROL || selectedFuelType == CreditFuelType.BOTH) {
                    OutlinedTextField(value = petrolLitreText, onValueChange = { petrolLitreText = it }, label = { Text("Petrol Litres (L)", fontSize = 9.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
                if (selectedFuelType == CreditFuelType.DIESEL || selectedFuelType == CreditFuelType.BOTH) {
                    OutlinedTextField(value = dieselLitreText, onValueChange = { dieselLitreText = it }, label = { Text("Diesel Litres (L)", fontSize = 9.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }

                OutlinedTextField(value = totalAmountText, onValueChange = { totalAmountText = it }, label = { Text("Total Amount Due (₹)", fontSize = 9.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = initialPaidText, onValueChange = { initialPaidText = it }, label = { Text("Initial Down Payment (Optional ₹)", fontSize = 9.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                val totalDue = totalAmountText.toDoubleOrNull() ?: 0.0
                val initialPaid = initialPaidText.toDoubleOrNull() ?: 0.0
                if (totalDue > 0.0) {
                    val record = CreditRecord(
                        date = entryDate,
                        vehicleNumber = vehicleNo.trim(),
                        customerName = customerName.trim(),
                        mobileNumber = mobileNo.trim(),
                        fuelType = selectedFuelType,
                        petrolQuantityLitre = petrolLitreText.toDoubleOrNull() ?: 0.0,
                        dieselQuantityLitre = dieselLitreText.toDoubleOrNull() ?: 0.0,
                        totalAmountDue = totalDue,
                        amountPaid = initialPaid,
                        lastPaymentDate = if (initialPaid > 0.0) SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date()) else ""
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Customer: ${credit.customerName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Remaining Balance: ₹ ${String.format(Locale.getDefault(), "%.2f", credit.remainingBalance)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = paymentAmountText,
                    onValueChange = { paymentAmountText = it },
                    label = { Text("Amount Received (₹)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
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
