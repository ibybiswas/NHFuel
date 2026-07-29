package com.nh.fuel.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.nh.fuel.data.ExpenseItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpendScreen(
    currentRecordDate: String,
    allExpenses: List<ExpenseItem>,
    onAddOrUpdateExpense: (ExpenseItem) -> Unit,
    onDeleteExpense: (ExpenseItem) -> Unit,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp
) {
    val isDark = isSystemInDarkTheme()
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // New Expense Input States
    var descriptionInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedExpenseDate by remember { mutableStateOf(currentRecordDate.ifBlank { todayStr }) }

    // Error & Dialog States
    var descriptionError by remember { mutableStateOf(false) }
    var showDatePickerModal by remember { mutableStateOf(false) }
    var editingExpenseItem by remember { mutableStateOf<ExpenseItem?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseItem?>(null) }

    // Keep selected date aligned if user switches main screen date
    LaunchedEffect(currentRecordDate) {
        if (currentRecordDate.isNotBlank()) {
            selectedExpenseDate = currentRecordDate
        }
    }

    // Filter & Aggregate expenses for the active date
    val currentDayExpenses = remember(allExpenses, selectedExpenseDate) {
        allExpenses.filter { it.date == selectedExpenseDate }
    }

    val totalDayExpense = remember(currentDayExpenses) {
        currentDayExpenses.sumOf { it.amount }
    }

    val grandTotalExpenses = remember(allExpenses) {
        allExpenses.sumOf { it.amount }
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
            text = "Expenditure Tracker",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Add Expense Card Block
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Add New Expense",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Expense Description Input
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = {
                        descriptionInput = it
                        if (it.isNotBlank()) descriptionError = false
                    },
                    label = { Text("Expense Description * (e.g. Tea/Snacks, Generator Repair)") },
                    isError = descriptionError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (descriptionError) {
                    Text(
                        text = "Expense description cannot be blank!",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Amount Input
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                amountInput = input
                            }
                        },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Date Picker Selector Field
                    OutlinedTextField(
                        value = selectedExpenseDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePickerModal = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePickerModal = true }
                    )
                }

                Button(
                    onClick = {
                        if (descriptionInput.isBlank()) {
                            descriptionError = true
                            return@Button
                        }
                        val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
                        val finalDate = selectedExpenseDate.ifBlank { todayStr }
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

                        onAddOrUpdateExpense(
                            ExpenseItem(
                                description = descriptionInput.trim(),
                                amount = parsedAmount,
                                date = finalDate,
                                timestamp = timeStr
                            )
                        )

                        // Clear Inputs
                        descriptionInput = ""
                        amountInput = ""
                        descriptionError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Expense", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Daily Aggregated Expenses Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aggregated Expenses ($selectedExpenseDate):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "₹ ${String.format(Locale.getDefault(), "%.2f", totalDayExpense)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("All-Time Total", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "₹ ${String.format(Locale.getDefault(), "%.2f", grandTotalExpenses)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Expense List for Selected Date
        Text(
            text = "Expense Log (${currentDayExpenses.size} items for $selectedExpenseDate):",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (currentDayExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses recorded for $selectedExpenseDate.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                currentDayExpenses.forEach { item ->
                    ExpenseCardRow(
                        item = item,
                        onEdit = { editingExpenseItem = item },
                        onDelete = { expenseToDelete = item }
                    )
                }
            }
        }

        Spacer(Modifier.height(bottomInset + 4.dp))
    }

    // Material 3 Easy Date Picker Dialog
    if (showDatePickerModal) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerModal = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePickerModal = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        selectedExpenseDate = sdf.format(Date(millis))
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

    // Edit Expense Dialog
    editingExpenseItem?.let { item ->
        var editDesc by remember { mutableStateOf(item.description) }
        var editAmount by remember { mutableStateOf(item.amount.toString()) }
        var editDate by remember { mutableStateOf(item.date) }
        var editError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingExpenseItem = null },
            title = { Text("Edit Expense Details", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = {
                            editDesc = it
                            if (it.isNotBlank()) editError = false
                        },
                        label = { Text("Description *") },
                        isError = editError,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editDesc.isBlank()) {
                        editError = true
                        return@TextButton
                    }
                    onAddOrUpdateExpense(
                        item.copy(
                            description = editDesc.trim(),
                            amount = editAmount.toDoubleOrNull() ?: 0.0,
                            date = editDate.ifBlank { todayStr }
                        )
                    )
                    editingExpenseItem = null
                }) { Text("Save Changes", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { editingExpenseItem = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    expenseToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense?", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${item.description}' (₹ ${item.amount})?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExpense(item)
                    expenseToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ExpenseCardRow(
    item: ExpenseItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (item.timestamp.isNotBlank()) "Logged @ ${item.timestamp}" else item.date,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "₹ ${String.format(Locale.getDefault(), "%.2f", item.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Expense",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Expense",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
