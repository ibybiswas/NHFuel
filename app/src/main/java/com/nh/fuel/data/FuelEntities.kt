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
}
