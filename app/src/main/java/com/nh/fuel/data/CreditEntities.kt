package com.nh.fuel.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CreditFuelType { PETROL, DIESEL, BOTH }
enum class CreditStatus { UNPAID, PARTIAL, PAID }

@Entity(tableName = "credit_records")
data class CreditRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                   // YYYY-MM-DD
    val vehicleNumber: String = "",     // Vehicle Number
    val customerName: String = "",      // Customer Name
    val mobileNumber: String = "",      // Mobile Number
    val fuelType: CreditFuelType = CreditFuelType.PETROL,
    val petrolQuantityLitre: Double = 0.0,
    val dieselQuantityLitre: Double = 0.0,
    val totalAmountDue: Double = 0.0,   // Total credit amount
    val amountPaid: Double = 0.0,       // Total paid so far
    val lastPaymentDate: String = "",   // Timestamp of last settlement
    val notes: String = ""
) {
    val remainingBalance: Double get() = (totalAmountDue - amountPaid).coerceAtLeast(0.0)
    val status: CreditStatus
        get() = when {
            amountPaid >= totalAmountDue && totalAmountDue > 0.0 -> CreditStatus.PAID
            amountPaid > 0.0 -> CreditStatus.PARTIAL
            else -> CreditStatus.UNPAID
        }
}
