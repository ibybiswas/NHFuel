package com.nh.fuel.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CreditFuelType { PETROL, DIESEL, BOTH }
enum class CreditStatus { UNPAID, PARTIAL, PAID }

@Entity(tableName = "credit_records")
data class CreditRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val vehicleNumber: String = "",
    val customerName: String = "",
    val mobileNumber: String = "",
    val fuelType: CreditFuelType = CreditFuelType.PETROL,
    val petrolQuantityLitre: Double = 0.0,
    val dieselQuantityLitre: Double = 0.0,
    val totalAmountDue: Double = 0.0,
    val amountPaid: Double = 0.0,
    val lastPaymentDate: String = "",
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
