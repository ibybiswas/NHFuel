package com.nh.fuel.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlin.math.max

@IgnoreExtraProperties
@Entity(tableName = "daily_fuel_records")
data class DailyFuelRecord(
    @PrimaryKey val date: String = "",
    val petrolTotal: Double = 0.0,
    val petrolRefill: Double = 0.0,
    val petrolVariation: Double = 0.0,
    val lastPetrolRefill: RefillEvent = RefillEvent(),
    val lastPetrolVariationAmount: Double = 0.0,
    val lastPetrolVariationTime: String = "",
    val lastPetrolDipAmount: Double = 0.0,
    val lastPetrolDipTime: String = "",
    val dieselTotal: Double = 0.0,
    val dieselRefill: Double = 0.0,
    val dieselVariation: Double = 0.0,
    val lastDieselRefill: RefillEvent = RefillEvent(),
    val lastDieselVariationAmount: Double = 0.0,
    val lastDieselVariationTime: String = "",
    val lastDieselDipAmount: Double = 0.0,
    val lastDieselDipTime: String = "",
    val petrolPrice: Double = 100.0,
    val dieselPrice: Double = 90.0,
    val shift1: DayShift = DayShift(1),
    val shift2: DayShift = DayShift(2),
    val shift3: DayShift = DayShift(3),
    val lastUpdatedTimestamp: Long = System.currentTimeMillis() // <--- Dynamic Header Update Ticker
) {
    val totalPetrolSell: Double get() = round2(shift1.petrolSale + shift2.petrolSale + shift3.petrolSale)
    val totalDieselSell: Double get() = round2(shift1.dieselSale + shift2.dieselSale + shift3.dieselSale)
    val totalPetrolTesting: Double get() = round2(shift1.totalPetrolTesting + shift2.totalPetrolTesting + shift3.totalPetrolTesting)
    val totalDieselTesting: Double get() = round2(shift1.totalDieselTesting + shift2.totalDieselTesting + shift3.totalDieselTesting)
    val currentPetrolStorage: Double
        get() = max(0.0, round2((petrolTotal + petrolRefill) + petrolVariation - totalPetrolSell))
    val currentDieselStorage: Double
        get() = max(0.0, round2((dieselTotal + dieselRefill) + dieselVariation - totalDieselSell))

    fun getPetrolAmount(litres: Double): Double = round2(litres * petrolPrice)
    fun getDieselAmount(litres: Double): Double = round2(litres * dieselPrice)

    val totalPetrolRevenue: Double get() = getPetrolAmount(totalPetrolSell)
    val totalDieselRevenue: Double get() = getDieselAmount(totalDieselSell)
    val grandTotalRevenue: Double get() = round2(totalPetrolRevenue + totalDieselRevenue)

    val dailyCashCollected: Double get() = round2(shift1.totalCashCollected + shift2.totalCashCollected + shift3.totalCashCollected)
    val dailyDigitalCollected: Double get() = round2(shift1.totalDigitalCollected + shift2.totalDigitalCollected + shift3.totalDigitalCollected)
    val dailyCreditCollected: Double get() = round2(shift1.totalCreditCollected + shift2.totalCreditCollected + shift3.totalCreditCollected)
    val dailyTotalCollected: Double get() = round2(dailyCashCollected + dailyDigitalCollected + dailyCreditCollected)
    val dailyMismatch: Double get() = round2(dailyTotalCollected - grandTotalRevenue)
}
