package com.nh.fuel.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties

enum class CreditFuelType { PETROL, DIESEL, BOTH }
enum class CreditStatus { UNPAID, PARTIAL, PAID }

@IgnoreExtraProperties
@Entity(tableName = "credit_records")
data class CreditRecord(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val date: String = "",
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
    val remainingBalance: Double get() = round2((totalAmountDue - amountPaid).coerceAtLeast(0.0))
    val status: CreditStatus
        get() = when {
            amountPaid >= totalAmountDue && totalAmountDue > 0.0 -> CreditStatus.PAID
            amountPaid > 0.0 -> CreditStatus.PARTIAL
            else -> CreditStatus.UNPAID
        }

    /** Returns this record with every rupee/litre field snapped to 2 decimals. */
    fun normalizeRounding(): CreditRecord = copy(
        petrolQuantityLitre = round2(petrolQuantityLitre),
        dieselQuantityLitre = round2(dieselQuantityLitre),
        totalAmountDue = round2(totalAmountDue),
        amountPaid = round2(amountPaid)
    )

    /** True if any raw field in this record holds more precision than 2 decimals. */
    fun needsRoundingMigration(): Boolean = this != normalizeRounding()
}
