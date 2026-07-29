package com.nh.fuel.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.max

data class RefillEvent(
    val amount: Double = 0.0,
    val timestamp: String = ""
)

data class NozzleShift(
    val open: Double = 0.0,
    val close: Double = 0.0
) {
    val isValid: Boolean get() = close >= open || close == 0.0
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
    val shiftNumber: Int,
    val mpd1: DispenserShift = DispenserShift(),
    val mpd2: DispenserShift = DispenserShift()
) {
    val petrolSale: Double get() = mpd1.petrolSale + mpd2.petrolSale
    val dieselSale: Double get() = mpd1.dieselSale + mpd2.dieselSale
    val isComplete: Boolean get() = mpd1.isShiftComplete && mpd2.isShiftComplete
}

@Entity(tableName = "daily_fuel_records")
data class DailyFuelRecord(
    @PrimaryKey val date: String,
    
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

    // Added fuel price tracking per day (in Rupees / Litre)
    val petrolPrice: Double = 100.0,
    val dieselPrice: Double = 90.0,

    val shift1: DayShift = DayShift(1),
    val shift2: DayShift = DayShift(2),
    val shift3: DayShift = DayShift(3)
) {
    val totalPetrolSell: Double get() = shift1.petrolSale + shift2.petrolSale + shift3.petrolSale
    val totalDieselSell: Double get() = shift1.dieselSale + shift2.dieselSale + shift3.dieselSale

    val currentPetrolStorage: Double 
        get() = max(0.0, (petrolTotal + petrolRefill) + petrolVariation - totalPetrolSell)
    val currentDieselStorage: Double 
        get() = max(0.0, (dieselTotal + dieselRefill) + dieselVariation - totalDieselSell)

    // Financial Calculation Helpers (in Rupees ₹)
    fun getPetrolAmount(litres: Double): Double = litres * petrolPrice
    fun getDieselAmount(litres: Double): Double = litres * dieselPrice

    val shift1PetrolRevenue: Double get() = getPetrolAmount(shift1.petrolSale)
    val shift1DieselRevenue: Double get() = getDieselAmount(shift1.dieselSale)
    val shift1TotalRevenue: Double get() = shift1PetrolRevenue + shift1DieselRevenue

    val shift2PetrolRevenue: Double get() = getPetrolAmount(shift2.petrolSale)
    val shift2DieselRevenue: Double get() = getDieselAmount(shift2.dieselSale)
    val shift2TotalRevenue: Double get() = shift2PetrolRevenue + shift2DieselRevenue

    val shift3PetrolRevenue: Double get() = getPetrolAmount(shift3.petrolSale)
    val shift3DieselRevenue: Double get() = getDieselAmount(shift3.dieselSale)
    val shift3TotalRevenue: Double get() = shift3PetrolRevenue + shift3DieselRevenue

    val totalPetrolRevenue: Double get() = getPetrolAmount(totalPetrolSell)
    val totalDieselRevenue: Double get() = getDieselAmount(totalDieselSell)
    val grandTotalRevenue: Double get() = totalPetrolRevenue + totalDieselRevenue
}
