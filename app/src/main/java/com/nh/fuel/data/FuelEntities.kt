package com.nh.fuel.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class RefillEvent(
    val amount: Double = 0.0,
    val timestamp: String = "" // e.g., "2026-07-28 08:30 AM"
)

data class NozzleShift(
    val open: Double = 0.0,
    val close: Double = 0.0
) {
    val sale: Double get() = if (close >= open && close > 0.0) close - open else 0.0
    val isClosed: Boolean get() = close > 0.0 && close >= open
}

data class DispenserShift(
    val petrolN2: NozzleShift = NozzleShift(),
    val petrolN3: NozzleShift = NozzleShift(),
    val dieselN1: NozzleShift = NozzleShift(),
    val dieselN4: NozzleShift = NozzleShift()
) {
    val petrolSale: Double get() = petrolN2.sale + petrolN3.sale
    val dieselSale: Double get() = dieselN1.sale + dieselN4.sale
    val isShiftComplete: Boolean
        get() = petrolN2.isClosed && petrolN3.isClosed && dieselN1.isClosed && dieselN4.isClosed
}

data class DayShift(
    val shiftNumber: Int, // 1, 2, or 3
    val npd1: DispenserShift = DispenserShift(),
    val npd2: DispenserShift = DispenserShift()
) {
    val petrolSale: Double get() = npd1.petrolSale + npd2.petrolSale
    val dieselSale: Double get() = npd1.dieselSale + npd2.dieselSale
    val isComplete: Boolean get() = npd1.isShiftComplete && npd2.isShiftComplete
}

@Entity(tableName = "daily_fuel_records")
data class DailyFuelRecord(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    
    // Petrol Tank Storage & Refill/Shortage Tracking
    val petrolTotal: Double = 0.0,
    val petrolRefill: Double = 0.0,      // Cumulative Total Refills
    val petrolShortage: Double = 0.0,    // Cumulative Total Shortages
    val lastPetrolRefill: RefillEvent = RefillEvent(),

    // Diesel Tank Storage & Refill/Shortage Tracking
    val dieselTotal: Double = 0.0,
    val dieselRefill: Double = 0.0,      // Cumulative Total Refills
    val dieselShortage: Double = 0.0,    // Cumulative Total Shortages
    val lastDieselRefill: RefillEvent = RefillEvent(),

    // 3 Shifts for NPD1 & NPD2
    val shift1: DayShift = DayShift(1),
    val shift2: DayShift = DayShift(2),
    val shift3: DayShift = DayShift(3)
) {
    // Total Daily Sales Across Shifts
    val totalPetrolSell: Double get() = shift1.petrolSale + shift2.petrolSale + shift3.petrolSale
    val totalDieselSell: Double get() = shift1.dieselSale + shift2.dieselSale + shift3.dieselSale

    // Current Tank Storage Calculation: (Base Stock + Refills) - Shortages - Shift Sales
    val currentPetrolStorage: Double get() = (petrolTotal + petrolRefill) - petrolShortage - totalPetrolSell
    val currentDieselStorage: Double get() = (dieselTotal + dieselRefill) - dieselShortage - totalDieselSell
}
